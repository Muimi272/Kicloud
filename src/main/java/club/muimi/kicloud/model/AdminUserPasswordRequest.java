package club.muimi.kicloud.model;

import lombok.Data;

@Data
public class AdminUserPasswordRequest {
    private Long userId;
    private String password;
}
