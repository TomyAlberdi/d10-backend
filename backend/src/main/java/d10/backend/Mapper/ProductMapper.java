package d10.backend.Mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

import d10.backend.DTO.Product.CreateProductDTO;
import d10.backend.Model.Product;
import d10.backend.Model.ProductStock;

public class ProductMapper {

    public static Product toEntity(CreateProductDTO dto) {
        Product product = new Product();
        // Initialize product stock
        ProductStock stock = new ProductStock(0,0.0, new ArrayList<>());
        product.setStock(stock);
        // Calculate Commercial Fields
        setCommercialProductFields(product, dto);
        // Set Remaining Fields
        updateFromDTO(product, dto);
        return product;
    }

    public static void updateFromDTO(Product product, CreateProductDTO createProductDTO) {
        // Main Data
        product.setCode(createProductDTO.getCode());
        product.setName(createProductDTO.getName());
        // Secondary Data
        product.setDescription(createProductDTO.getDescription());
        product.setQuality(createProductDTO.getQuality());
        product.setProviderName(createProductDTO.getProviderName());
        product.setCharacteristics(createProductDTO.getCharacteristics());
        product.setImages(createProductDTO.getImages());
        product.setCategory(createProductDTO.getCategory());
        product.setSubcategory(createProductDTO.getSubcategory());
        product.setDimensions(createProductDTO.getDimensions());
        // Measure Data
        product.setMeasureType(createProductDTO.getMeasureType());
        // Sale Data
        product.setSaleUnitType(createProductDTO.getSaleUnitType());
        product.setCostBySaleUnit(createProductDTO.getCostBySaleUnit());
    }

    public static void setCommercialProductFields(Product product, CreateProductDTO dto) {
        double parsedPriceBySaleUnit = dto.getPriceBySaleUnit();
        double parsedCostBySaleUnit = dto.getCostBySaleUnit() != null ? dto.getCostBySaleUnit() : 0.0;
        double priceByMeasureUnit = 0.0;
        if (dto.getSaleUnitType().equals(Product.SaleType.UNIDAD) && dto.getMeasureType().equals(Product.MeasureType.UNIDAD)) {
            dto.setMeasurePerSaleUnit(1.0);
            priceByMeasureUnit = truncateToTwoDecimals(parsedPriceBySaleUnit);
        } else if (dto.getMeasurePerSaleUnit() > 0) {
            priceByMeasureUnit = truncateToTwoDecimals(parsedPriceBySaleUnit / dto.getMeasurePerSaleUnit());
        }
        // Calculate profit percentage
        double profit = 0.0;
        if (parsedCostBySaleUnit > 0) {
            profit = truncateToTwoDecimals(((parsedPriceBySaleUnit - parsedCostBySaleUnit) / parsedCostBySaleUnit) * 100);
        }
        product.setMeasurePerSaleUnit(dto.getMeasurePerSaleUnit());
        product.setPriceByMeasureUnit(priceByMeasureUnit);
        product.setPriceBySaleUnit(parsedPriceBySaleUnit);
        product.setCostBySaleUnit(parsedCostBySaleUnit);
        product.setProfit(profit);
    }

    private static double truncateToTwoDecimals(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.FLOOR) // Truncate without rounding
                .doubleValue();
    }

}
