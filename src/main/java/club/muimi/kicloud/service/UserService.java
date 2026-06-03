package club.muimi.kicloud.service;

import club.muimi.kicloud.dao.InvitationDao;
import club.muimi.kicloud.dao.UserDao;
import club.muimi.kicloud.entity.Invitation;
import club.muimi.kicloud.entity.User;
import club.muimi.kicloud.model.LoginUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {
    private static final String LOGIN_FAILED_MESSAGE = "Login Failed";

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final InvitationDao invitationDao;

    public UserService(UserDao userDao, PasswordEncoder passwordEncoder, InvitationDao invitationDao) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.invitationDao = invitationDao;
    }

    public Map<String, Object> login(Long id, String password, HttpSession session) {
        Map<String, Object> response = buildLoginFailedResponse();
        if (id == null) {
            return response;
        }
        if (password == null || password.trim().isEmpty()) {
            return response;
        }
        Optional<User> user = userDao.findById(id);
        if (user.isEmpty()) {
            return response;
        }
        if (!user.get().isEnabled()) {
            return response;
        }
        if (!passwordEncoder.matches(password, user.get().getPasswordHash())) {
            return response;
        }
        LoginUser loginUser = new LoginUser(user.get().getId(), user.get().getUsername(), user.get().getRole());
        saveLoginState(session, loginUser);
        response.put("code", 1);
        response.put("msg", "Login Successful");
        return response;
    }

    public Map<String, Object> login(String username, String password, HttpSession session) {
        Map<String, Object> response = buildLoginFailedResponse();
        if (username == null || username.trim().isEmpty()) {
            return response;
        }
        if (password == null || password.trim().isEmpty()) {
            return response;
        }
        Optional<User> user = userDao.findByUsername(username);
        if (user.isEmpty()) {
            return response;
        }
        if (!user.get().isEnabled()) {
            return response;
        }
        if (!passwordEncoder.matches(password, user.get().getPasswordHash())) {
            return response;
        }
        LoginUser loginUser = new LoginUser(user.get().getId(), user.get().getUsername(), user.get().getRole());
        saveLoginState(session, loginUser);
        response.put("code", 1);
        response.put("msg", "Login Successful");
        return response;
    }

    public boolean isLoginUserIllegal(LoginUser loginUser) {
        if (loginUser == null) return true;
        if (loginUser.getId() == null || loginUser.getUsername() == null || loginUser.getRole() == null) return true;
        if (loginUser.getUsername().trim().isEmpty()) return true;
        Optional<User> optionalUser = userDao.findById(loginUser.getId());
        if (optionalUser.isEmpty()) return true;
        User user = optionalUser.get();
        if (!user.isEnabled()) return true;
        if (!Objects.equals(user.getUsername(), loginUser.getUsername())) return true;
        if (user.getRole() != loginUser.getRole()) return true;
        return false;
    }

    public User getUserByLoginUser(LoginUser loginUser) {
        if (isLoginUserIllegal(loginUser)) {
            return null;
        }
        Optional<User> optional = userDao.findById(loginUser.getId());
        return optional.orElse(null);
    }

    public boolean logout(HttpSession session) {
        if (session == null) {
            SecurityContextHolder.clearContext();
            return false;
        }
        boolean hasLoginUser = session.getAttribute("LoginUser") != null;
        boolean hasSecurityContext = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) != null;
        clearLoginState(session);
        return hasLoginUser || hasSecurityContext;
    }

    public void clearLoginState(HttpSession session) {
        SecurityContextHolder.clearContext();
        if (session == null) {
            return;
        }
        session.removeAttribute("LoginUser");
        session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    }

    public Map<String, Object> register(String username, String password, String invitationCode) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 0);
        if (username == null || username.trim().isEmpty()) {
            response.put("msg", "Missing Username");
            return response;
        }
        if (password == null || password.trim().isEmpty()) {
            response.put("msg", "Missing Password");
            return response;
        }
        if (invitationCode == null) {
            response.put("msg", "Missing Invitation Code");
            return response;
        }
        if (userDao.findByUsername(username).isPresent()) {
            response.put("msg", "Username exist.");
            return response;
        }
        Invitation invitation = invitationDao.findByInviteCode(invitationCode).orElse(null);
        if (invitation == null) {
            response.put("msg", "Invitation Code does not exist.");
            return response;
        }
        if (invitation.isUsed() || !invitation.isValid()) {
            response.put("msg", "Invalid Invitation Code");
            return response;
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        userDao.save(user);
        invitation.setUsed(true);
        invitation.setUserId(userDao.findByUsername(username).get().getId());
        invitation.setUsedAt(LocalDateTime.now());
        invitationDao.save(invitation);
        response.put("code", 1);
        response.put("msg", "User has been registered successfully.");
        return response;
    }

    private void saveLoginState(HttpSession session, LoginUser loginUser) {
        session.setAttribute("LoginUser", loginUser);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + loginUser.getRole().name()));
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
        SecurityContext securityContext = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(securityContext);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
    }

    private Map<String, Object> buildLoginFailedResponse() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 0);
        response.put("msg", LOGIN_FAILED_MESSAGE);
        return response;
    }
}
