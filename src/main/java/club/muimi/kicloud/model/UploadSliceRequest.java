package club.muimi.kicloud.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadSliceRequest {
    private String uploadId;
    private String fullFileMD5;
    private Integer sliceNum;
    private Integer size;
    private MultipartFile slice;
}
