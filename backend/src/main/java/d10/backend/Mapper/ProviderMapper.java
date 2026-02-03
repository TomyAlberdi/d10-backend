package d10.backend.Mapper;

import d10.backend.DTO.Provider.CreateProviderDTO;
import d10.backend.Model.Provider;

public class ProviderMapper {

    public static Provider toEntity(CreateProviderDTO dto) {
        Provider provider = new Provider();
        updateFromDTO(provider, dto);
        return provider;
    }

    public static void updateFromDTO(Provider provider, CreateProviderDTO dto) {
        provider.setName(dto.getName());
        provider.setCity(dto.getCity());
        provider.setAddress(dto.getAddress());
        provider.setPhone(dto.getPhone());
        provider.setEmail(dto.getEmail());
        provider.setCuit(dto.getCuit());
    }

}
