package d10.backend.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import d10.backend.DTO.Client.CreateClientDTO;
import d10.backend.Exception.ResourceNotFoundException;
import d10.backend.Mapper.ClientMapper;
import d10.backend.Model.Client;
import d10.backend.Repository.ClientRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    public Client findById(String id) {
        Optional<Client> clientSearch = clientRepository.findById(id);
        if (clientSearch.isEmpty()) {
            throw new ResourceNotFoundException("Cliente con ID " + id + " no encontrado.");
        }
        Client client = clientSearch.get();
        return client;
    }

    public Client createClient(CreateClientDTO createClientDTO) {
        Client client = ClientMapper.toEntity(createClientDTO);
        clientRepository.save(client);
        return client;
    }

    public Client updateClient(String id, CreateClientDTO createClientDTO) {
        Client client = findById(id);
        ClientMapper.updateFromDTO(client, createClientDTO);
        clientRepository.save(client);
        return client;
    }

    public List<Client> searchClients(String q) {
        if (q == null) {
            return java.util.Collections.emptyList();
        }
        return clientRepository.findByCuitDniContainingIgnoreCaseOrNameContainingIgnoreCase(q, q);
    }

    public void deleteClient(String id) {
        findById(id);
        clientRepository.deleteById(id);
    }

}
