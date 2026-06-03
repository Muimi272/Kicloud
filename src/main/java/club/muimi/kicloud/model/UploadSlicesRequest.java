package club.muimi.kicloud.model;

import lombok.Data;

@Data
public class UploadSlicesRequest {
    private Integer sliceCount;
    private String fullFileMD5;
    private String fileName;
    private Long parentId;
    private Long totalSize;
}
