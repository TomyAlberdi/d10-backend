package d10.backend.Mapper;

import java.time.LocalDateTime;

import d10.backend.DTO.StockLog.StockLogDTO;
import d10.backend.Model.Product;
import d10.backend.Model.StockLog;

public class StockLogMapper {

    private StockLogMapper() {
        // Utility class
    }

    public static StockLog toEntity(Product product, StockLog.StockLogType type, Integer saleUnitQuantity,
            String detail, LocalDateTime datetime) {
        StockLog stockLog = new StockLog();
        stockLog.setProductId(product.getId());
        stockLog.setProductName(product.getName());
        stockLog.setSaleUnitQuantity(saleUnitQuantity);
        stockLog.setSaleUnitType(product.getSaleUnitType());
        stockLog.setMeasureUnitQuantity(measureEquivalent(product, saleUnitQuantity));
        stockLog.setMeasureUnitType(product.getMeasureType());
        stockLog.setType(type);
        stockLog.setDatetime(datetime != null ? datetime : LocalDateTime.now());
        stockLog.setDetail(detail != null && !detail.trim().isEmpty() ? detail.trim() : null);
        return stockLog;
    }

    public static StockLogDTO toDTO(StockLog stockLog) {
        if (stockLog == null) {
            return null;
        }
        return new StockLogDTO(
                stockLog.getId(),
                stockLog.getProductName(),
                stockLog.getProductId(),
                stockLog.getSaleUnitQuantity(),
                stockLog.getSaleUnitType(),
                stockLog.getMeasureUnitQuantity(),
                stockLog.getMeasureUnitType(),
                stockLog.getType(),
                stockLog.getDatetime(),
                stockLog.getDetail());
    }

    private static Double measureEquivalent(Product product, Integer saleUnitQuantity) {
        if (saleUnitQuantity == null) {
            return 0.0;
        }
        double measurePerSaleUnit = product.getMeasurePerSaleUnit() != null ? product.getMeasurePerSaleUnit() : 0.0;
        return Math.round(saleUnitQuantity * measurePerSaleUnit * 100.0) / 100.0;
    }

}
