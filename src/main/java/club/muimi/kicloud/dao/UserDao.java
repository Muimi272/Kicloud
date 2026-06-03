package club.muimi.kicloud.dao;

import club.muimi.kicloud.entity.User;
import club.muimi.kicloud.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserDao extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String name);

    boolean existsByRole(Role role);
}
