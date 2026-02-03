package d10.backend.Mapper;

import d10.backend.DTO.Product.CreateProductDTO;
import d10.backend.DTO.Product.PartialProductDTO;
import d10.backend.Model.Product;
import d10.backend.Model.ProductStock;

import java.util.ArrayList;

public class ProductMapper {

    public static Product toEntity(CreateProductDTO dto) {
        Product product = new Product();
        ProductStock stock = new ProductStock(0,0.0, new ArrayList<>());
        product.setStock(stock);
        updateFromDTO(product, dto);
        return product;
    }

    public static void updateFromDTO(Product product, CreateProductDTO createProductDTO) {
        product.setCode(createProductDTO.getCode());
        product.setName(createProductDTO.getName());
        product.setDescription(createProductDTO.getDescription());
    }

    public static PartialProductDTO toPartialDTO(Product product) {
        PartialProductDTO partialProductDTO = new PartialProductDTO();
        partialProductDTO.setId(product.getId());
        partialProductDTO.setCode(product.getCode());
        partialProductDTO.setName(product.getName());
        partialProductDTO.setDiscontinued(product.getDiscontinued());
        return partialProductDTO;
    }

}
