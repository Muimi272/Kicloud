package club.muimi.kicloud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
public class Link {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String linkId;

    @Column(nullable = false)
    private Long storageFileId;

    @Column
    private String password;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false)
    private Long downloadTimes = 0L;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime deletedAt;
}
