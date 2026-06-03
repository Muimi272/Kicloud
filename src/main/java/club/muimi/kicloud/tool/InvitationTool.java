package club.muimi.kicloud.tool;

import club.muimi.kicloud.dao.InvitationDao;
import org.springframework.stereotype.Component;

@Component
public class InvitationTool {
    private static final char[] CHARACTER_POOL =
            {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
                    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
            };

    private static final int INVITATION_LENGTH = 6;

    private final InvitationDao invitationDao;

    public InvitationTool(InvitationDao invitationDao) {
        this.invitationDao = invitationDao;
    }

    public String createInvitation() {
        StringBuilder invitationCode = new StringBuilder();
        do {
            invitationCode.setLength(0);
            for (int i = 0; i < INVITATION_LENGTH; i++) {
                int randomIndex = (int) (Math.random() * CHARACTER_POOL.length);
                char randomChar = CHARACTER_POOL[randomIndex];
                invitationCode.append(randomChar);
            }
        } while (invitationDao.existsByInviteCode(invitationCode.toString()));
        return invitationCode.toString();
    }
}
