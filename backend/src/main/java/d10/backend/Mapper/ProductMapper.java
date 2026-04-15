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
        product.setCostByMeasureUnit(createProductDTO.getCostByMeasureUnit());
    }

    public static void setCommercialProductFields(Product product, CreateProductDTO dto) {
        double parsedCostByMeasureUnit = dto.getCostByMeasureUnit() != null ? dto.getCostByMeasureUnit() : 0.0;
        double profitPercentage = dto.getProfitPercentage() != null ? dto.getProfitPercentage() : 0.0;
        double measurePerSaleUnit = dto.getMeasurePerSaleUnit() != null ? dto.getMeasurePerSaleUnit() : 1.0;
        
        double priceByMeasureUnit = 0.0;
        double priceBySaleUnit = 0.0;
        
        // Calculate priceByMeasureUnit from costByMeasureUnit and profit percentage
        if (profitPercentage > 0 && parsedCostByMeasureUnit > 0) {
            priceByMeasureUnit = truncateToTwoDecimals(parsedCostByMeasureUnit * (1 + (profitPercentage / 100)));
        } else {
            priceByMeasureUnit = truncateToTwoDecimals(parsedCostByMeasureUnit);
        }
        
        // Calculate priceBySaleUnit from priceByMeasureUnit and measurePerSaleUnit
        if (dto.getSaleUnitType().equals(Product.SaleType.UNIDAD) && dto.getMeasureType().equals(Product.MeasureType.UNIDAD)) {
            dto.setMeasurePerSaleUnit(1.0);
            priceBySaleUnit = priceByMeasureUnit;
        } else if (measurePerSaleUnit > 0) {
            priceBySaleUnit = truncateToTwoDecimals(priceByMeasureUnit * measurePerSaleUnit);
        }
        
        product.setMeasurePerSaleUnit(measurePerSaleUnit);
        product.setPriceByMeasureUnit(priceByMeasureUnit);
        product.setPriceBySaleUnit(priceBySaleUnit);
        product.setCostByMeasureUnit(parsedCostByMeasureUnit);
        product.setProfit(profitPercentage);
    }

    private static double truncateToTwoDecimals(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.FLOOR) // Truncate without rounding
                .doubleValue();
    }

}
