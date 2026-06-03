package club.muimi.kicloud.model;

import lombok.Data;

@Data
public class AdminUsernameRequest {
    private Long userId;
    private String username;
}
