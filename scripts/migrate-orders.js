/*
 * Migrates the orders collection from the legacy shape (one document per
 * ordered product, batches grouped implicitly by orderDate) into the current
 * one: a single Order document per order, holding a products array.
 *
 * Usage:
 *   mongosh "<connection-string>" --file migrate-orders.js
 *
 * Run it once per database (d10-olavarria, d10-tandil, ...): the target
 * database is the one of the connection string.
 *
 * Lines are grouped by orderDate AND by their received flag, never by date
 * alone. A batch where only some lines were received would otherwise end up as
 * a single order whose flag is wrong for half of it, and receiving or reverting
 * that order would move stock that was already moved.
 *
 * Re-running is safe: the script only reads documents that still carry a top
 * level productId, so a migrated collection has nothing left to migrate.
 *
 * There is no rollback. Take a dump before running it:
 *   mongodump --uri "<connection-string>" --collection orders
 */

// ---------------------------------------------------------------------------
// Options
// ---------------------------------------------------------------------------

/** true = report only, nothing is written. Set to false to actually migrate. */
const DRY_RUN = true;

// ---------------------------------------------------------------------------
// Migration
// ---------------------------------------------------------------------------

const NO_DATE = "sin-fecha";

/** Legacy lines only: a migrated order has no top level productId. */
const legacy = db.orders
  .find({ productId: { $exists: true } })
  .sort({ _id: 1 })
  .toArray();

if (legacy.length === 0) {
  print("No hay pedidos con el formato viejo. Nada para migrar.");
  quit(0);
}

// Key = date + received, so a partially received batch stays split in two.
const groups = new Map();
for (const line of legacy) {
  const dateKey = line.orderDate ? line.orderDate.toISOString() : NO_DATE;
  const key = `${dateKey}|${line.received === true}`;
  if (!groups.has(key)) {
    groups.set(key, []);
  }
  groups.get(key).push(line);
}

const orders = [];
for (const lines of groups.values()) {
  const first = lines[0];
  orders.push({
    // The id of the first line is reused, so the new order keeps a real
    // ObjectId and re-running cannot duplicate it.
    _id: first._id,
    date: first.orderDate ?? null,
    received: first.received === true,
    products: lines.map((line) => {
      const product = {
        productId: line.productId ?? null,
        productCode: line.productCode ?? null,
        productName: line.productName ?? null,
        providerName: line.providerName ?? null,
        saleUnitType: line.saleUnitType ?? null,
        saleUnitQuantity: line.saleUnitQuantity ?? null,
      };
      // Null fields are not written by the application either.
      if (line.detail) product.detail = line.detail;
      return product;
    }),
    _class: "d10.backend.Model.Order",
  });
}

print(`Líneas con formato viejo: ${legacy.length}`);
print(`Pedidos a crear: ${orders.length}`);
for (const order of orders) {
  const date = order.date ? order.date.toISOString().slice(0, 10) : NO_DATE;
  const state = order.received ? "recibido" : "pendiente";
  print(`  ${date} (${state}): ${order.products.length} producto(s)`);
}

if (DRY_RUN) {
  print("");
  print("DRY_RUN activo: no se escribió nada. Poné DRY_RUN = false para migrar.");
  quit(0);
}

const deleted = db.orders.deleteMany({ _id: { $in: legacy.map((l) => l._id) } });
const inserted = db.orders.insertMany(orders);

print("");
print(`Líneas eliminadas: ${deleted.deletedCount}`);
print(`Pedidos creados: ${Object.keys(inserted.insertedIds).length}`);
