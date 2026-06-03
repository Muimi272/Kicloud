package club.muimi.kicloud.service;

import club.muimi.kicloud.dao.InvitationDao;
import club.muimi.kicloud.dao.LinkDao;
import club.muimi.kicloud.dao.StorageFileDao;
import club.muimi.kicloud.dao.UserDao;
import club.muimi.kicloud.entity.Invitation;
import club.muimi.kicloud.entity.StorageFile;
import club.muimi.kicloud.entity.User;
import club.muimi.kicloud.model.LoginUser;
import club.muimi.kicloud.model.Role;
import club.muimi.kicloud.service.StorageFileService;
import club.muimi.kicloud.tool.InvitationTool;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
public class AdminService {
    private final InvitationDao invitationDao;
    private final InvitationTool invitationTool;
    private final UserDao userDao;
    private final LinkDao linkDao;
    private final StorageFileDao storageFileDao;
    private final StorageFileService storageFileService;
    private final PasswordEncoder passwordEncoder;

    public AdminService(InvitationDao invitationDao, InvitationTool invitationTool, UserDao userDao, LinkDao linkDao, StorageFileDao storageFileDao, StorageFileService storageFileService, PasswordEncoder passwordEncoder) {
        this.invitationDao = invitationDao;
        this.invitationTool = invitationTool;
        this.userDao = userDao;
        this.linkDao = linkDao;
        this.storageFileDao = storageFileDao;
        this.storageFileService = storageFileService;
        this.passwordEncoder = passwordEncoder;
    }

    public Invitation createInvitation(HttpSession session) {
        LoginUser operator = getCheckedAdmin(session);
        if (operator == null) {
            return null;
        }
        String invitationCode = invitationTool.createInvitation();
        Invitation invitation = new Invitation();
        invitation.setInviteCode(invitationCode);
        invitation.setGeneratorId(operator.getId());
        invitationDao.save(invitation);
        return invitation;
    }

    public Map<String, Object> getAllUsers(HttpSession session) {
        LoginUser operator = getCheckedAdmin(session);
        if (operator == null) {
            return buildError("Invalid session");
        }
        List<Map<String, Object>> users = userDao.findAll().stream()
                .map(this::buildUserSummary)
                .collect(Collectors.toList());
        return buildSuccess(Map.of("users", users), "Users loaded successfully.");
    }

    public Map<String, Object> getUserDetail(HttpSession session, Long userId) {
        LoginUser operator = getCheckedAdmin(session);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (userId == null) {
            return buildError("Missing User ID");
        }
        User user = userDao.findById(userId).orElse(null);
        if (user == null) {
            return buildError("User does not exist.");
        }
        return buildSuccess(Map.of("user", buildUserDetail(user)), "User loaded successfully.");
    }

    public Map<String, Object> getAllInvitations(HttpSession session) {
        LoginUser operator = getCheckedAdmin(session);
        if (operator == null) {
            return buildError("Invalid session");
        }
        List<Map<String, Object>> invitations = invitationDao.findAll().stream()
                .map(this::buildInvitationSummary)
                .collect(Collectors.toList());
        return buildSuccess(Map.of("invitations", invitations), "Invitations loaded successfully.");
    }

    public Map<String, Object> getInvitationDetail(HttpSession session, Long invitationId) {
        LoginUser operator = getCheckedAdmin(session);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (invitationId == null) {
            return buildError("Missing Invitation ID");
        }
        Invitation invitation = invitationDao.findById(invitationId).orElse(null);
        if (invitation == null) {
            return buildError("Invitation not found");
        }
        return buildSuccess(Map.of("invitation", buildInvitationDetail(invitation)), "Invitation loaded successfully.");
    }

    public Map<String, Object> changeInvitationStatus(HttpSession session, Long invitationId, Boolean valid) {
        LoginUser operator = getCheckedAdmin(session);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (invitationId == null) {
            return buildError("Missing Invitation ID");
        }
        if (valid == null) {
            return buildError("Missing Invitation Status");
        }
        Invitation invitation = invitationDao.findById(invitationId).orElse(null);
        if (invitation == null) {
            return buildError("Invitation not found");
        }
        if (!canManageInvitation(operator, invitation)) {
            return buildError("Can not manage this invitation.");
        }
        if (invitation.isUsed() && valid) {
            return buildError("Used invitation can not be re-enabled.");
        }
        if (invitation.isValid() == valid) {
            return buildSuccess(Map.of("invitation", buildInvitationDetail(invitation)), "Invitation status is already up to date.");
        }
        invitation.setValid(valid);
        invitationDao.save(invitation);
        return buildSuccess(Map.of("invitation", buildInvitationDetail(invitation)), valid ? "Invitation has been enabled successfully." : "Invitation has been banned successfully.");
    }

    public Map<String, Object> changeUserStatus(HttpSession session, Long userId, Boolean enabled) {
        LoginUser operator = getCheckedAdmin(session);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (userId == null) {
            return buildError("Missing User ID");
        }
        if (enabled == null) {
            return buildError("Missing User Status");
        }
        User user = userDao.findById(userId).orElse(null);
        if (user == null) {
            return buildError("User does not exist.");
        }
        if (!canManageUser(operator, user)) {
            return buildError("Can not manage this user.");
        }
        if (user.isEnabled() == enabled) {
            return buildSuccess(Map.of("user", buildUserDetail(user)), enabled ? "User is already enabled." : "User is already disabled.");
        }
        user.setEnabled(enabled);
        userDao.save(user);
        return buildSuccess(Map.of("user", buildUserDetail(user)), enabled ? "User has been enabled successfully." : "User has been banned successfully.");
    }

    public Map<String, Object> changeUserSpace(HttpSession session, Long userId, Long totalSpace) {
        LoginUser operator = getCheckedAdmin(session);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (userId == null) {
            return buildError("Missing User ID");
        }
        if (totalSpace == null) {
            return buildError("Missing Total Space");
        }
        if (totalSpace < 0) {
            return buildError("Total Space can not be negative");
        }
        User user = userDao.findById(userId).orElse(null);
        if (user == null) {
            return buildError("User does not exist.");
        }
        if (!canManageUser(operator, user)) {
            return buildError("Can not manage this user.");
        }
        if (user.getUsedSpace() != null && totalSpace < user.getUsedSpace()) {
            return buildError("Total Space can not be less than Used Space");
        }
        user.setTotalSpace(totalSpace);
        userDao.save(user);
        return buildSuccess(Map.of("user", buildUserDetail(user)), "User total space has been updated successfully.");
    }

    public Map<String, Object> changeUserPassword(HttpSession session, Long userId, String password) {
        LoginUser operator = getCheckedAdmin(session);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (userId == null) {
            return buildError("Missing User ID");
        }
        if (password == null || password.trim().isEmpty()) {
            return buildError("Missing Password");
        }
        User user = userDao.findById(userId).orElse(null);
        if (user == null) {
            return buildError("User does not exist.");
        }
        if (!canManageUser(operator, user)) {
            return buildError("Can not manage this user.");
        }
        user.setPasswordHash(passwordEncoder.encode(password.trim()));
        userDao.save(user);
        return buildSuccess(Map.of("user", buildUserDetail(user)), "User password has been updated successfully.");
    }

    public Map<String, Object> changeUsername(HttpSession session, Long userId, String username) {
        LoginUser operator = getCheckedAdmin(session);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (userId == null) {
            return buildError("Missing User ID");
        }
        if (username == null || username.trim().isEmpty()) {
            return buildError("Missing Username");
        }
        String newUsername = username.trim();
        User user = userDao.findById(userId).orElse(null);
        if (user == null) {
            return buildError("User does not exist.");
        }
        if (!canManageUser(operator, user)) {
            return buildError("Can not manage this user.");
        }
        User sameNameUser = userDao.findByUsername(newUsername).orElse(null);
        if (sameNameUser != null && !sameNameUser.getId().equals(userId)) {
            return buildError("Username exist.");
        }
        user.setUsername(newUsername);
        userDao.save(user);
        LoginUser loginUser = (LoginUser) session.getAttribute("LoginUser");
        if (loginUser != null && userId.equals(loginUser.getId())) {
            session.setAttribute("LoginUser", new LoginUser(loginUser.getId(), newUsername, loginUser.getRole()));
        }
        return buildSuccess(Map.of("user", buildUserDetail(user)), "Username has been updated successfully.");
    }

    public Map<String, Object> changeUserRole(HttpSession session, Long userId, Role role) {
        LoginUser operator = getCheckedAdmin(session);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (userId == null) {
            return buildError("Missing User ID");
        }
        if (role == null) {
            return buildError("Missing Role");
        }
        User user = userDao.findById(userId).orElse(null);
        if (user == null) {
            return buildError("User does not exist.");
        }
        if (!canManageRoleChange(operator, user, role)) {
            return buildError("Can not change this user role.");
        }
        if (user.getRole() == role) {
            return buildSuccess(Map.of("user", buildUserDetail(user)), "User role is already up to date.");
        }
        user.setRole(role);
        userDao.save(user);
        return buildSuccess(Map.of("user", buildUserDetail(user)), "User role has been updated successfully.");
    }

    public Map<String, Object> getUserFiles(HttpSession session, Long userId, Long parentId) {
        LoginUser operator = getCheckedAdmin(session);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (operator.getRole() != Role.SUPERADMIN) {
            return buildError("Forbidden");
        }
        if (userId == null) {
            return buildError("Missing User ID");
        }
        return storageFileService.listFilesByOwnerForSuperAdmin(userId, parentId, operator);
    }

    public Map<String, Object> searchUserFiles(HttpSession session, Long userId, String keyword) {
        LoginUser operator = getCheckedAdmin(session);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (operator.getRole() != Role.SUPERADMIN) {
            return buildError("Forbidden");
        }
        if (userId == null) {
            return buildError("Missing User ID");
        }
        return storageFileService.searchFilesByOwnerForSuperAdmin(userId, keyword, operator);
    }

    public Map<String, Object> getAllLinks(HttpSession session) {
        LoginUser operator = getCheckedAdmin(session);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (operator.getRole() != Role.SUPERADMIN) {
            return buildError("Forbidden");
        }
        List<Map<String, Object>> links = linkDao.findByDeletedFalseOrderByCreatedAtDesc().stream()
                .map(this::buildLinkSummary)
                .collect(Collectors.toList());
        return buildSuccess(Map.of("links", links), "Links loaded successfully.");
    }

    public Map<String, Object> deleteLink(HttpSession session, Long linkId) {
        LoginUser operator = getCheckedAdmin(session);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (operator.getRole() != Role.SUPERADMIN) {
            return buildError("Forbidden");
        }
        if (linkId == null) {
            return buildError("Missing Link ID");
        }
        club.muimi.kicloud.entity.Link link = linkDao.findByIdAndDeletedFalse(linkId).orElse(null);
        if (link == null) {
            return buildError("Link does not exist.");
        }
        link.setDeleted(true);
        link.setDeletedAt(java.time.LocalDateTime.now());
        linkDao.save(link);
        return buildSuccess(Map.of("link", buildLinkSummary(link)), "Link deleted successfully.");
    }

    public Map<String, Object> buildUserSummary(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("enabled", user.isEnabled());
        map.put("role", user.getRole());
        map.put("usedSpace", user.getUsedSpace());
        map.put("totalSpace", user.getTotalSpace());
        map.put("remainingSpace", calculateRemainingSpace(user));
        map.put("createdAt", user.getCreatedAt());
        return map;
    }

    public Map<String, Object> buildUserDetail(User user) {
        return buildUserSummary(user);
    }

    public Map<String, Object> buildInvitationSummary(Invitation invitation) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", invitation.getId());
        map.put("inviteCode", invitation.getInviteCode());
        map.put("used", invitation.isUsed());
        map.put("valid", invitation.isValid());
        map.put("userId", invitation.getUserId());
        map.put("generatorId", invitation.getGeneratorId());
        map.put("createdAt", invitation.getCreatedAt());
        map.put("usedAt", invitation.getUsedAt());
        return map;
    }

    public Map<String, Object> buildInvitationDetail(Invitation invitation) {
        return buildInvitationSummary(invitation);
    }

    public Map<String, Object> buildLinkSummary(club.muimi.kicloud.entity.Link link) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", link.getId());
        map.put("linkId", link.getLinkId());
        map.put("storageFileId", link.getStorageFileId());
        map.put("ownerId", link.getOwnerId());
        map.put("ownerName", link.getOwnerName());
        map.put("downloadTimes", link.getDownloadTimes());
        map.put("hasPassword", link.getPassword() != null && !link.getPassword().isBlank());
        map.put("deleted", link.isDeleted());
        map.put("createdAt", link.getCreatedAt());
        map.put("deletedAt", link.getDeletedAt());
        return map;
    }

    private long calculateRemainingSpace(User user) {
        long totalSpace = user.getTotalSpace() == null ? 0L : user.getTotalSpace();
        long usedSpace = user.getUsedSpace() == null ? 0L : user.getUsedSpace();
        return Math.max(0L, totalSpace - usedSpace);
    }

    private Map<String, Object> buildSuccess(Map<String, Object> data, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", 1);
        map.put("msg", message);
        map.put("data", data);
        return map;
    }

    private Map<String, Object> buildError(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", 0);
        map.put("msg", message);
        return map;
    }

    private LoginUser getCheckedAdmin(HttpSession session) {
        if (session == null) return null;
        if (session.getAttribute("LoginUser") == null) return null;
        LoginUser loginUser = (LoginUser) session.getAttribute("LoginUser");
        if (loginUser == null) return null;
        if (loginUser.getRole() != Role.ADMIN && loginUser.getRole() != Role.SUPERADMIN) {
            return null;
        }
        User dbUser = userDao.findById(loginUser.getId()).orElse(null);
        if (dbUser == null) return null;
        if (!dbUser.isEnabled()) return null;
        if (!loginUser.getId().equals(dbUser.getId())) return null;
        if (!Objects.equals(loginUser.getUsername(), dbUser.getUsername())) return null;
        if (loginUser.getRole() != dbUser.getRole()) return null;
        return loginUser;
    }

    private boolean canManageUser(LoginUser operator, User targetUser) {
        if (operator == null || targetUser == null) {
            return false;
        }
        if (Objects.equals(operator.getId(), targetUser.getId())) {
            return false;
        }
        if (targetUser.getRole() == Role.SUPERADMIN) {
            return false;
        }
        return operator.getRole() == Role.SUPERADMIN || targetUser.getRole() == Role.USER;
    }

    private boolean canManageRoleChange(LoginUser operator, User targetUser, Role newRole) {
        if (!canManageUser(operator, targetUser)) {
            return false;
        }
        if (newRole == Role.SUPERADMIN) {
            return operator.getRole() == Role.SUPERADMIN;
        }
        if (operator.getRole() == Role.ADMIN && targetUser.getRole() != Role.USER) {
            return false;
        }
        return true;
    }

    private boolean canManageInvitation(LoginUser operator, Invitation invitation) {
        if (operator == null || invitation == null) {
            return false;
        }
        if (operator.getRole() == Role.SUPERADMIN) {
            return true;
        }
        return Objects.equals(invitation.getGeneratorId(), operator.getId());
    }
}
