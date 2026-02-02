package d10.backend.Mapper;


import d10.backend.DTO.Client.CreateClientDTO;
import d10.backend.DTO.Client.PartialClientDTO;
import d10.backend.Model.Client;

public class ClientMapper {

    public static Client toEntity(CreateClientDTO dto) {
        Client client = new Client();
        updateFromDTO(client, dto);
        return client;
    }

    public static void updateFromDTO(Client client,  CreateClientDTO dto) {
        client.setType(dto.getType());
        client.setName(dto.getName());
        client.setAddress(dto.getAddress());
        client.setCuitDni(dto.getCuitDni());
        client.setEmail(dto.getEmail());
        client.setPhone(dto.getPhone());
    }

    public static PartialClientDTO toPartialDTO(Client client) {
        PartialClientDTO partialClientDTO = new PartialClientDTO();
        partialClientDTO.setId(partialClientDTO.getId());
        partialClientDTO.setName(client.getName());
        partialClientDTO.setType(client.getType());
        return partialClientDTO;
    }

}
