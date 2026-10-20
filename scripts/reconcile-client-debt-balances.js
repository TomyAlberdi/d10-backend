/*
 * Brings client balances in line with the debt model introduced by the
 * linked invoice payments change: a client's balance holds minus what their
 * open debts still owe (total - partialPayment of every DEUDA invoice), and
 * every payment, cancellation or edit on a debt moves it by the difference.
 *
 * The previous code did not keep that invariant, so balances drifted:
 *   - debts created before the client balance feature were never charged;
 *   - a debt created as DEUDA was charged its whole total, ignoring the part
 *     already paid;
 *   - a sale created as PENDIENTE and edited to DEUDA was never charged;
 *   - paying off or cancelling a debt never gave the charge back.
 * Left as is, the new code would then hand out phantom credit or leave
 * phantom debt when those debts are paid or cancelled.
 *
 * Apart from the clients listed in MANUAL_BALANCE_CLIENTS, a balance has only
 * ever been moved by invoices, so it is recomputed from the open debts:
 *   balance = -(sum over the client's DEUDA invoices of total - partialPayment)
 * Clients listed in MANUAL_BALANCE_CLIENTS had their balance set by hand and
 * are never written; the script only reports how far they are from that sum.
 * A client with a positive balance (credit) not on the list is also skipped
 * and reported, since only a manual adjustment can create credit.
 *
 * Usage:
 *   mongosh "<connection-string>" --file reconcile-client-debt-balances.js
 *
 * Run it once per database (d10-olavarria, d10-tandil, ...): the target
 * database is the one of the connection string. Run it right AFTER the new
 * backend is deployed: until then the old code keeps charging new debts their
 * whole total.
 *
 * Re-running is safe: balances are recomputed, not shifted, so a second run
 * finds nothing to change.
 *
 * Every balance is copied to BACKUP_COLLECTION before it is changed (the
 * first copy is kept on re-runs). Rollback:
 *   db.client_balance_backup_debt_reconcile.find().forEach((b) =>
 *     db.clients.updateOne({ _id: b._id }, { $set: { balance: b.balance } }));
 * Taking a dump before running it is still recommended:
 *   mongodump --uri "<connection-string>" --collection clients
 */

// ---------------------------------------------------------------------------
// Options
// ---------------------------------------------------------------------------

/** true = report only, nothing is written. Set to false to actually migrate. */
const DRY_RUN = true;

/**
 * Clients whose balance was adjusted by hand, by name. Matching ignores case,
 * accents and repeated spaces. A real run aborts if a name matches no client or
 * more than one, so the list must hold only clients of the target database.
 */
const MANUAL_BALANCE_CLIENTS = ["juan talamo"];

const BACKUP_COLLECTION = "client_balance_backup_debt_reconcile";

/** Same tolerance as the backend: differences under a cent are ignored. */
const PAYMENT_TOLERANCE = 0.01;

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function normalizeName(name) {
  return String(name ?? "")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase()
    .replace(/\s+/g, " ")
    .trim();
}

/** Ids are ObjectIds in the database, but tolerate plain strings. */
function idKey(id) {
  if (id == null) {
    return null;
  }
  return typeof id.toHexString === "function" ? id.toHexString() : String(id);
}

function round2(value) {
  return Math.round(value * 100) / 100;
}

function money(value) {
  return `$ ${round2(value).toFixed(2)}`;
}

// ---------------------------------------------------------------------------
// Migration
// ---------------------------------------------------------------------------

print(`Base de datos: ${db.getName()}${DRY_RUN ? "  (DRY RUN, no se escribe nada)" : ""}`);

const clients = db.clients.find({}, { name: 1, balance: 1 }).toArray();
const clientsById = new Map(clients.map((c) => [idKey(c._id), c]));

// Clients whose balance was set by hand.
const manualIds = new Set();
let manualProblem = false;
for (const wanted of MANUAL_BALANCE_CLIENTS) {
  const matches = clients.filter((c) => normalizeName(c.name) === normalizeName(wanted));
  if (matches.length === 1) {
    manualIds.add(idKey(matches[0]._id));
  } else {
    manualProblem = true;
    print(
      matches.length === 0
        ? `ATENCIÓN: no hay ningún cliente llamado "${wanted}".`
        : `ATENCIÓN: hay ${matches.length} clientes llamados "${wanted}": ` +
            matches.map((c) => idKey(c._id)).join(", "),
    );
  }
}
if (manualProblem && !DRY_RUN) {
  print("Corregí MANUAL_BALANCE_CLIENTS para esta base antes de migrar. No se escribió nada.");
  quit(1);
}

// What each client still owes on their open debts.
const owedByClient = new Map();
const orphanDebts = [];
db.invoices
  .find(
    { status: "DEUDA" },
    { invoiceNumber: 1, total: 1, partialPayment: 1, "client._id": 1, "client.id": 1, "client.name": 1 },
  )
  .forEach((invoice) => {
    const clientId = idKey(invoice.client?._id ?? invoice.client?.id);
    if (clientId === null) {
      return; // A debt without a client owes nothing to any balance.
    }
    const owed = Math.max(0, (invoice.total ?? 0) - (invoice.partialPayment ?? 0));
    if (owed < PAYMENT_TOLERANCE) {
      return;
    }
    if (!clientsById.has(clientId)) {
      orphanDebts.push(invoice);
      return;
    }
    const entry = owedByClient.get(clientId) ?? { owed: 0, debts: 0 };
    entry.owed += owed;
    entry.debts += 1;
    owedByClient.set(clientId, entry);
  });

const changes = [];
const skippedCredit = [];
const manualReport = [];
for (const client of clients) {
  const key = idKey(client._id);
  const current = client.balance ?? 0;
  const debt = owedByClient.get(key) ?? { owed: 0, debts: 0 };
  const expected = round2(-debt.owed) || 0; // never store -0
  if (manualIds.has(key)) {
    manualReport.push({ client, current, expected, debts: debt.debts });
    continue;
  }
  if (Math.abs(expected - current) < PAYMENT_TOLERANCE) {
    continue;
  }
  if (current >= PAYMENT_TOLERANCE) {
    skippedCredit.push({ client, current, expected, debts: debt.debts });
    continue;
  }
  changes.push({ client, current, expected, debts: debt.debts });
}

print("");
print(`Clientes: ${clients.length}. Con deudas abiertas: ${owedByClient.size}. A corregir: ${changes.length}.`);
for (const { client, current, expected, debts } of changes) {
  print(
    `  ${client.name} (${idKey(client._id)}): ${money(current)} -> ${money(expected)}` +
      `  [${debts} deuda(s) abierta(s), diferencia ${money(expected - current)}]`,
  );
}

for (const { client, current, expected, debts } of manualReport) {
  print("");
  print(`Saldo manual, no se modifica: ${client.name} (${idKey(client._id)})`);
  print(`  Saldo actual ${money(current)}; sus ${debts} deuda(s) abierta(s) suman ${money(expected)}.`);
  if (Math.abs(expected - current) >= PAYMENT_TOLERANCE) {
    print(
      `  Diferencia ${money(current - expected)}: si no es un crédito o deuda real ajeno a ` +
        "las ventas, al saldar sus deudas el saldo no va a quedar en 0.",
    );
  }
}

if (skippedCredit.length > 0) {
  print("");
  print("Clientes con saldo a favor que no están en MANUAL_BALANCE_CLIENTS (no se modifican, revisar a mano):");
  for (const { client, current, expected, debts } of skippedCredit) {
    print(`  ${client.name} (${idKey(client._id)}): ${money(current)}; ${debts} deuda(s) abierta(s) suman ${money(expected)}`);
  }
}

if (orphanDebts.length > 0) {
  print("");
  print("Deudas de clientes que ya no existen (se ignoran):");
  for (const invoice of orphanDebts) {
    print(`  Venta #${invoice.invoiceNumber} de ${invoice.client?.name ?? "?"}`);
  }
}

if (DRY_RUN || changes.length === 0) {
  print("");
  print(DRY_RUN ? "DRY RUN: no se escribió nada." : "Nada para corregir.");
  quit(0);
}

let updated = 0;
for (const { client, current, expected } of changes) {
  db.getCollection(BACKUP_COLLECTION).updateOne(
    { _id: client._id },
    { $setOnInsert: { name: client.name, balance: client.balance ?? null, backedUpAt: new Date() } },
    { upsert: true },
  );
  // Only if the balance did not move since it was read.
  const result = db.clients.updateOne(
    { _id: client._id, balance: client.balance ?? null },
    { $set: { balance: expected } },
  );
  if (result.modifiedCount === 1) {
    updated += 1;
  } else {
    print(`  ${client.name}: el saldo cambió mientras corría el script (${money(current)}), no se modificó. Volvé a correrlo.`);
  }
}

print("");
print(`Listo: ${updated} saldo(s) corregido(s). Copia de los anteriores en ${BACKUP_COLLECTION}.`);
