package club.muimi.kicloud.model;

import lombok.Data;

@Data
public class AdminUserRoleRequest {
    private Long userId;
    private Role role;
}
