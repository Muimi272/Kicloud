package club.muimi.kicloud.model;

import lombok.Data;

@Data
public class RenameStorageFileRequest {
    private Long id;
    private String newName;
}
