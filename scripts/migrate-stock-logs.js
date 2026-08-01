/*
 * Migrates the legacy per product stock history (products.stock.recordList)
 * into the document based stock_logs collection.
 *
 * Usage:
 *   mongosh "<connection-string>" --file migrate-stock-logs.js
 *
 * The script only reads the products collection and writes to stock_logs, it
 * never modifies products. Run it once per database (d10-olavarria, d10-tandil,
 * ...): the target database is the one of the connection string.
 *
 * Re-running is safe: every generated log has a deterministic _id
 * ("legacy-<productId>-<index>"), so a second run replaces the same documents
 * instead of duplicating them.
 *
 * Rollback (removes only what this script created):
 *   db.stock_logs.deleteMany({ migrationSource: "stock.recordList" })
 */

// ---------------------------------------------------------------------------
// Options
// ---------------------------------------------------------------------------

/** true = report only, nothing is written. Set to false to actually migrate. */
const DRY_RUN = true;

/**
 * Records dated on or after this moment are skipped.
 *
 * The application now writes to stock_logs AND to the legacy recordList, so any
 * movement registered after the new version went live already exists as a log.
 * Migrating it again would duplicate it. Set this to the deploy date to avoid
 * that, for example:
 *
 *   const CUTOFF = new Date("2026-08-01T00:00:00-03:00");
 *
 * Leave it as null when migrating before deploying the new version.
 */
const CUTOFF = null;

/**
 * Legacy records only store a date, so every movement of a day lands on
 * 00:00 and same day movements have no defined order. When true, the index of
 * the record inside the product list is added as milliseconds, which keeps the
 * original order without changing the timestamp shown in the UI.
 */
const PRESERVE_ORDER_WITHIN_DAY = true;

/** Documents written per bulkWrite call. */
const BATCH_SIZE = 500;

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

// mongosh exposes the Double constructor; fall back to the plain number when it
// is not available (Spring still converts an int into the Double field).
const asDouble = typeof Double === "function" ? (v) => Double(v) : (v) => v;

/** Measure equivalent of the movement, rounded to 2 decimals like the backend. */
function measureEquivalent(quantity, measurePerSaleUnit) {
  const perUnit = typeof measurePerSaleUnit === "number" ? measurePerSaleUnit : 0;
  return asDouble(Math.round(quantity * perUnit * 100) / 100);
}

/** Legacy date (a BSON date) turned into the datetime of the log. */
function toDatetime(record, index) {
  const raw = record.date;
  let value = null;
  if (raw instanceof Date) {
    value = new Date(raw.getTime());
  } else if (typeof raw === "string" && raw.length > 0) {
    // Fallback that should not be needed: every record checked stores a date.
    // A "YYYY-MM-DD" string is read as UTC midnight, not local midnight.
    value = new Date(raw);
  }
  if (value === null || isNaN(value.getTime())) {
    return null;
  }
  if (PRESERVE_ORDER_WITHIN_DAY) {
    value = new Date(value.getTime() + index);
  }
  return value;
}

function toStockLog(product, record, index, datetime) {
  return {
    _id: "legacy-" + product._id.toString() + "-" + index,
    productName: product.name != null ? product.name : null,
    productId: product._id.toString(),
    saleUnitQuantity: record.quantity,
    saleUnitType: product.saleUnitType != null ? product.saleUnitType : null,
    measureUnitQuantity: measureEquivalent(record.quantity, product.measurePerSaleUnit),
    measureUnitType: product.measureType != null ? product.measureType : null,
    type: record.type,
    datetime: datetime,
    // The legacy format has no detail; invoice references cannot be recovered.
    detail: null,
    // Provenance marker, ignored by the application, used for rollback.
    migrationSource: "stock.recordList",
    _class: "d10.backend.Model.StockLog",
  };
}

// ---------------------------------------------------------------------------
// Pre-flight
// ---------------------------------------------------------------------------

print("");
print("=== stock.recordList -> stock_logs ===");
print("Database:   " + db.getName());
print("Mode:       " + (DRY_RUN ? "DRY RUN (nothing is written)" : "WRITE"));
print("Cutoff:     " + (CUTOFF ? CUTOFF.toISOString() : "none (every record)"));
print("");

const alreadyMigrated = db.stock_logs.countDocuments({
  migrationSource: "stock.recordList",
});
const writtenByApp = db.stock_logs.countDocuments({
  migrationSource: { $exists: false },
});

print("stock_logs: " + alreadyMigrated + " from a previous run of this script, " + writtenByApp + " written by the application.");

if (writtenByApp > 0 && CUTOFF === null) {
  const oldest = db.stock_logs
    .find({ migrationSource: { $exists: false } })
    .sort({ datetime: 1 })
    .limit(1)
    .toArray()[0];
  if (oldest && oldest.datetime) {
    print("");
    print("WARNING: the application already logged movements, the oldest one is dated");
    print("         " + oldest.datetime.toISOString() + ".");
    print("         Set CUTOFF to that date to avoid migrating movements twice.");
  }
}
print("");

// ---------------------------------------------------------------------------
// Migration
// ---------------------------------------------------------------------------

const report = {
  products: 0,
  records: 0,
  migrated: 0,
  skippedByCutoff: 0,
  skippedInvalidType: 0,
  skippedInvalidQuantity: 0,
  skippedInvalidDate: 0,
};

let operations = [];

function flush() {
  if (operations.length === 0) return;
  if (!DRY_RUN) {
    db.stock_logs.bulkWrite(operations, { ordered: false });
  }
  operations = [];
}

db.products
  .find({ "stock.recordList.0": { $exists: true } })
  .forEach(function (product) {
    report.products++;
    product.stock.recordList.forEach(function (record, index) {
      report.records++;

      if (record.type !== "IN" && record.type !== "OUT") {
        report.skippedInvalidType++;
        return;
      }
      if (typeof record.quantity !== "number" || record.quantity <= 0) {
        report.skippedInvalidQuantity++;
        return;
      }
      const datetime = toDatetime(record, index);
      if (datetime === null) {
        report.skippedInvalidDate++;
        return;
      }
      if (CUTOFF !== null && datetime >= CUTOFF) {
        report.skippedByCutoff++;
        return;
      }

      const stockLog = toStockLog(product, record, index, datetime);
      operations.push({
        replaceOne: {
          filter: { _id: stockLog._id },
          replacement: stockLog,
          upsert: true,
        },
      });
      report.migrated++;

      if (operations.length >= BATCH_SIZE) flush();
    });
  });

flush();

// ---------------------------------------------------------------------------
// Report
// ---------------------------------------------------------------------------

print("Products with history:  " + report.products);
print("Records found:          " + report.records);
print("Logs " + (DRY_RUN ? "to migrate:        " : "migrated:          ") + report.migrated);
if (report.skippedByCutoff > 0) print("Skipped (cutoff):       " + report.skippedByCutoff);
if (report.skippedInvalidType > 0) print("Skipped (bad type):     " + report.skippedInvalidType);
if (report.skippedInvalidQuantity > 0) print("Skipped (bad quantity): " + report.skippedInvalidQuantity);
if (report.skippedInvalidDate > 0) print("Skipped (bad date):     " + report.skippedInvalidDate);
print("");

if (DRY_RUN) {
  print("DRY RUN: nothing was written. Set DRY_RUN = false to migrate.");
  const preview = db.products.findOne({ "stock.recordList.0": { $exists: true } });
  if (preview) {
    const datetime = toDatetime(preview.stock.recordList[0], 0);
    if (datetime !== null) {
      print("");
      print("Example of a generated document:");
      printjson(toStockLog(preview, preview.stock.recordList[0], 0, datetime));
    }
  }
} else {
  print("Done. stock_logs now holds " + db.stock_logs.countDocuments({}) + " documents (" + db.stock_logs.countDocuments({ migrationSource: "stock.recordList" }) + " from this migration).");
}
print("");
