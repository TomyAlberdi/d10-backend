package d10.backend.Model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "packs")
public class Pack {

    @Id
    private String id;

    private String name;

    private List<PackItem> items;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
