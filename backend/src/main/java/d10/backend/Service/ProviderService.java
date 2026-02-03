package d10.backend.Service;

import d10.backend.DTO.Provider.CreateProviderDTO;
import d10.backend.Exception.ResourceNotFoundException;
import d10.backend.Mapper.ProviderMapper;
import d10.backend.Model.Provider;
import d10.backend.Repository.ProviderPaginationRepository;
import d10.backend.Repository.ProviderRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final ProviderPaginationRepository providerPaginationRepository;

    public Page<Provider> getPaginatedProviders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return providerPaginationRepository.findAll(pageable);
    }

    public Provider findById(String id) {
        Optional<Provider> providerSearch = providerRepository.findById(id);
        if (providerSearch.isEmpty()) {
            throw new ResourceNotFoundException("Proveedor con ID " + id + " no encontrado.");
        }
        Provider provider = providerSearch.get();
        return provider;
    }

    public Provider createProvider(CreateProviderDTO createProviderDTO) {
        Provider provider = ProviderMapper.toEntity(createProviderDTO);
        providerRepository.save(provider);
        return provider;
    }

    public Provider updateProvider(String id, CreateProviderDTO createProviderDTO) {
        Provider provider = findById(id);
        ProviderMapper.updateFromDTO(provider, createProviderDTO);
        providerRepository.save(provider);
        return provider;
    }

    public void deleteProvider(String id) {
        findById(id);
        providerRepository.deleteById(id);
    }

}
