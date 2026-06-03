package club.muimi.kicloud.model;

import lombok.Data;

@Data
public class AdminUserSpaceRequest {
    private Long userId;
    private Long totalSpace;
}
