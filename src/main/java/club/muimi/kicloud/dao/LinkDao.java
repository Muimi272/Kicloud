package club.muimi.kicloud.dao;

import club.muimi.kicloud.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LinkDao extends JpaRepository<Link, Long> {
    Optional<Link> findByIdAndDeletedFalse(Long id);

    Optional<Link> findByIdAndOwnerIdAndDeletedFalse(Long id, Long ownerId);

    Optional<Link> findByLinkIdAndDeletedFalse(String linkId);

    boolean existsByLinkId(String linkId);

    List<Link> findByOwnerIdAndDeletedFalseOrderByCreatedAtDesc(Long ownerId);

    List<Link> findByDeletedFalseOrderByCreatedAtDesc();
}
