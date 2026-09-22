package d10.backend.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import d10.backend.DTO.Pack.CreatePackDTO;
import d10.backend.Exception.ResourceNotFoundException;
import d10.backend.Mapper.PackMapper;
import d10.backend.Model.Pack;
import d10.backend.Repository.PackRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class PackService {

    private final PackRepository packRepository;

    public Pack findById(String id) {
        Optional<Pack> result = packRepository.findById(id);
        if (result.isEmpty()) {
            throw new ResourceNotFoundException("Pack con ID " + id + " no encontrado.");
        }
        return result.get();
    }

    public List<Pack> findAll() {
        return packRepository.findAll();
    }

    public Pack createPack(CreatePackDTO dto) {
        Pack pack = PackMapper.toEntity(dto);
        packRepository.save(pack);
        return pack;
    }

    public Pack updatePack(String id, CreatePackDTO dto) {
        Pack pack = findById(id);
        PackMapper.updateFromDTO(pack, dto);
        packRepository.save(pack);
        return pack;
    }

    public void deletePack(String id) {
        findById(id);
        packRepository.deleteById(id);
    }
}
