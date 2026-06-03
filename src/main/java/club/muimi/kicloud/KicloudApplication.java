package club.muimi.kicloud;

import club.muimi.kicloud.dao.UserDao;
import club.muimi.kicloud.entity.User;
import club.muimi.kicloud.model.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableJpaAuditing
public class KicloudApplication {

    @Value("${kicloud.init.superadmin.username}")
    private String SuperAdmin;

    @Value("${kicloud.init.superadmin.password}")
    private String SuperAdminPassword;

    @Bean
    public CommandLineRunner initSuperAdmin(UserDao
                                                    userDao, PasswordEncoder passwordEncoder) {
        return _ -> {
            if (!userDao.existsByRole(Role.SUPERADMIN)) {
                User defaultUser = new User();
                defaultUser.setUsername(SuperAdmin);
                defaultUser.setPasswordHash(passwordEncoder.encode(SuperAdminPassword));
                defaultUser.setRole(Role.SUPERADMIN);
                defaultUser.setTotalSpace(1024L * 1024L * 1024L * 1024L * 1024L);
                userDao.save(defaultUser);
            }
        };
    }

    static void main(String[] args) {
        SpringApplication.run(KicloudApplication.class, args);
    }

}
