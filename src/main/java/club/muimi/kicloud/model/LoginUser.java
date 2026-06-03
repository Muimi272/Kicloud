package club.muimi.kicloud.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginUser {
    private Long id;
    private String username;
    private Role role;
}
