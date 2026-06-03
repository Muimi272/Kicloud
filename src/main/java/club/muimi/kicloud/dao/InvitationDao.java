package club.muimi.kicloud.dao;


import club.muimi.kicloud.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvitationDao extends JpaRepository<Invitation, Long> {
    boolean existsByInviteCode(String inviteCode);
    Optional<Invitation> findByInviteCode(String inviteCode);
}
