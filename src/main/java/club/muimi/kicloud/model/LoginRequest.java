package club.muimi.kicloud.model;

import lombok.Data;

@Data
public class LoginRequest {
    private Long id;
    private String username;
    private String password;
}
