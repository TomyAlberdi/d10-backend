package d10.backend.Mapper;

import d10.backend.DTO.Product.CreateProductDTO;
import d10.backend.DTO.Product.PartialProductDTO;
import d10.backend.Model.Product;

public class ProductMapper {

    public static Product toEntity(CreateProductDTO dto) {
        Product product = new Product();
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
        return partialProductDTO;
    }

}
