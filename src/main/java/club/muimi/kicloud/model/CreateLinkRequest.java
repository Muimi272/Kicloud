package club.muimi.kicloud.model;

import lombok.Data;

@Data
public class CreateLinkRequest {
    private Long storageFileId;
    private String password;
}
