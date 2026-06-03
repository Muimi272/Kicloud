package club.muimi.kicloud.dao;

import club.muimi.kicloud.entity.StorageFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StorageFileDao extends JpaRepository<StorageFile, Long> {
    Optional<StorageFile> findByIdAndDeletedFalse(Long id);

    Optional<StorageFile> findByIdAndOwnerIdAndDeletedFalse(Long id, Long ownerId);

    @Query("""
            select sf from StorageFile sf
            where sf.ownerId = :ownerId
              and sf.deleted = false
              and ((:parentId is null and sf.parentId is null) or sf.parentId = :parentId)
            order by sf.fileType asc, sf.createdAt desc
            """)
    List<StorageFile> findActiveByOwnerIdAndParentId(Long ownerId, Long parentId);

    List<StorageFile> findByOwnerIdAndNameContainingIgnoreCaseAndDeletedFalseOrderByFileTypeAscCreatedAtDesc(Long ownerId, String keyword);

    @Query("""
            select count(sf) > 0 from StorageFile sf
            where sf.ownerId = :ownerId
              and sf.deleted = false
              and sf.name = :name
              and ((:parentId is null and sf.parentId is null) or sf.parentId = :parentId)
            """)
    boolean existsActiveByOwnerIdAndParentIdAndName(Long ownerId, Long parentId, String name);

    @Query("""
            select count(sf) > 0 from StorageFile sf
            where sf.ownerId = :ownerId
              and sf.deleted = false
              and sf.name = :name
              and sf.id <> :id
              and ((:parentId is null and sf.parentId is null) or sf.parentId = :parentId)
            """)
    boolean existsActiveSiblingNameExcludingId(Long ownerId, Long parentId, String name, Long id);
}
