package club.muimi.kicloud.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    private String invitationCode;
    private String username;
    private String password;
}
