package club.muimi.kicloud.model;

import lombok.Data;

@Data
public class AdminUserStatusRequest {
    private Long userId;
    private Boolean enabled;
}
