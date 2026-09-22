/*
 * Same migration as migrate-stock-logs.js, expressed as a single aggregation
 * pipeline. Everything runs server side, so it can be pasted into Compass, the
 * Atlas UI or any driver instead of running a script.
 *
 * Preview it by removing the last stage ($merge); nothing is written until that
 * stage runs.
 *
 * Rollback:
 *   db.stock_logs.deleteMany({ migrationSource: "stock.recordList" })
 */

db.products.aggregate([
  { $match: { "stock.recordList.0": { $exists: true } } },

  { $unwind: { path: "$stock.recordList", includeArrayIndex: "idx" } },

  {
    $match: {
      "stock.recordList.type": { $in: ["IN", "OUT"] },
      "stock.recordList.quantity": { $gt: 0 },
      "stock.recordList.date": { $type: "date" },
      // Uncomment and set the date the new version went live, so movements the
      // application already logged are not migrated a second time:
      // "stock.recordList.date": { $lt: ISODate("2026-08-01T00:00:00-03:00") },
    },
  },

  {
    $project: {
      // Deterministic id: re-running replaces instead of duplicating.
      _id: {
        $concat: ["legacy-", { $toString: "$_id" }, "-", { $toString: "$idx" }],
      },
      productName: "$name",
      productId: { $toString: "$_id" },
      saleUnitQuantity: "$stock.recordList.quantity",
      saleUnitType: "$saleUnitType",
      measureUnitQuantity: {
        $round: [
          {
            $multiply: [
              "$stock.recordList.quantity",
              { $toDouble: { $ifNull: ["$measurePerSaleUnit", 0] } },
            ],
          },
          2,
        ],
      },
      measureUnitType: "$measureType",
      type: "$stock.recordList.type",
      // The index is added as milliseconds so same day movements keep their
      // original order; remove the $add to copy the date as is.
      datetime: { $add: ["$stock.recordList.date", "$idx"] },
      detail: { $literal: null },
      migrationSource: { $literal: "stock.recordList" },
      _class: { $literal: "d10.backend.Model.StockLog" },
    },
  },

  {
    $merge: {
      into: "stock_logs",
      on: "_id",
      whenMatched: "replace",
      whenNotMatched: "insert",
    },
  },
]);
