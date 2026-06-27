package d10.backend.Mapper;

import java.time.LocalDateTime;
import java.util.List;

import d10.backend.DTO.Pack.CreatePackDTO;
import d10.backend.DTO.Pack.PackItemDTO;
import d10.backend.Model.Pack;
import d10.backend.Model.PackItem;

public class PackMapper {

    public static Pack toEntity(CreatePackDTO dto) {
        Pack pack = new Pack();
        pack.setName(dto.getName());
        pack.setItems(mapItems(dto.getItems()));
        pack.setCreatedAt(LocalDateTime.now());
        pack.setUpdatedAt(LocalDateTime.now());
        return pack;
    }

    public static void updateFromDTO(Pack pack, CreatePackDTO dto) {
        pack.setName(dto.getName());
        pack.setItems(mapItems(dto.getItems()));
        pack.setUpdatedAt(LocalDateTime.now());
    }

    private static List<PackItem> mapItems(List<PackItemDTO> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream()
                .map(dto -> new PackItem(dto.getProductId(), dto.getProductName(), dto.getQuantity()))
                .toList();
    }
}
