package club.muimi.kicloud.model;

import lombok.Data;

@Data
public class AdminInvitationStatusRequest {
    private Long invitationId;
    private Boolean valid;
}
