package club.muimi.kicloud.controller;

import club.muimi.kicloud.entity.Invitation;
import club.muimi.kicloud.model.AdminInvitationStatusRequest;
import club.muimi.kicloud.model.AdminUserIdRequest;
import club.muimi.kicloud.model.DeleteLinkRequest;
import club.muimi.kicloud.model.AdminUserPasswordRequest;
import club.muimi.kicloud.model.AdminUserRoleRequest;
import club.muimi.kicloud.model.AdminUserSpaceRequest;
import club.muimi.kicloud.model.AdminUserStatusRequest;
import club.muimi.kicloud.model.AdminUsernameRequest;
import club.muimi.kicloud.service.AdminService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/invite")
    @ResponseBody
    public Map<String, Object> invite(HttpSession session) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", 0);
        Invitation invitation = adminService.createInvitation(session);
        if (invitation == null) {
            map.put("msg", "Create Invitation Failed");
        } else {
            map.put("code", 1);
            map.put("msg", "Invitation Created");
            map.put("invitation code", invitation.getInviteCode());
            map.put("data", Map.of("invitation", adminService.buildInvitationDetail(invitation)));
        }
        return map;
    }

    @GetMapping("/users")
    @ResponseBody
    public Map<String, Object> getUsers(HttpSession session) {
        return adminService.getAllUsers(session);
    }

    @GetMapping("/user/{userId}")
    @ResponseBody
    public Map<String, Object> getUserDetail(@PathVariable Long userId, HttpSession session) {
        return adminService.getUserDetail(session, userId);
    }

    @GetMapping("/invitations")
    @ResponseBody
    public Map<String, Object> getInvitations(HttpSession session) {
        return adminService.getAllInvitations(session);
    }

    @GetMapping("/invitation/{invitationId}")
    @ResponseBody
    public Map<String, Object> getInvitationDetail(@PathVariable Long invitationId, HttpSession session) {
        return adminService.getInvitationDetail(session, invitationId);
    }

    @PostMapping("/user/status")
    @ResponseBody
    public Map<String, Object> changeUserStatus(@RequestBody AdminUserStatusRequest request, HttpSession session) {
        if (request == null) {
            return missingInformation();
        }
        return adminService.changeUserStatus(session, request.getUserId(), request.getEnabled());
    }

    @PostMapping("/user/ban")
    @ResponseBody
    public Map<String, Object> banUser(@RequestBody AdminUserIdRequest request, HttpSession session) {
        if (request == null) {
            return missingInformation();
        }
        return adminService.changeUserStatus(session, request.getUserId(), false);
    }

    @PostMapping("/user/space")
    @ResponseBody
    public Map<String, Object> changeUserSpace(@RequestBody AdminUserSpaceRequest request, HttpSession session) {
        if (request == null) {
            return missingInformation();
        }
        return adminService.changeUserSpace(session, request.getUserId(), request.getTotalSpace());
    }

    @PostMapping("/user/password")
    @ResponseBody
    public Map<String, Object> changeUserPassword(@RequestBody AdminUserPasswordRequest request, HttpSession session) {
        if (request == null) {
            return missingInformation();
        }
        return adminService.changeUserPassword(session, request.getUserId(), request.getPassword());
    }

    @PostMapping("/user/username")
    @ResponseBody
    public Map<String, Object> changeUsername(@RequestBody AdminUsernameRequest request, HttpSession session) {
        if (request == null) {
            return missingInformation();
        }
        return adminService.changeUsername(session, request.getUserId(), request.getUsername());
    }

    @PostMapping("/user/role")
    @ResponseBody
    public Map<String, Object> changeUserRole(@RequestBody AdminUserRoleRequest request, HttpSession session) {
        if (request == null) {
            return missingInformation();
        }
        return adminService.changeUserRole(session, request.getUserId(), request.getRole());
    }

    @PostMapping("/invitation/status")
    @ResponseBody
    public Map<String, Object> changeInvitationStatus(@RequestBody AdminInvitationStatusRequest request, HttpSession session) {
        if (request == null) {
            return missingInformation();
        }
        return adminService.changeInvitationStatus(session, request.getInvitationId(), request.getValid());
    }

    @GetMapping("/user/{userId}/files")
    @ResponseBody
    public Map<String, Object> getUserFiles(@PathVariable Long userId,
                                            @RequestParam(value = "parentId", required = false) Long parentId,
                                            HttpSession session) {
        return adminService.getUserFiles(session, userId, parentId);
    }

    @GetMapping("/user/{userId}/files/search")
    @ResponseBody
    public Map<String, Object> searchUserFiles(@PathVariable Long userId,
                                               @RequestParam(value = "keyword", required = false) String keyword,
                                               HttpSession session) {
        return adminService.searchUserFiles(session, userId, keyword);
    }

    @GetMapping("/links")
    @ResponseBody
    public Map<String, Object> getLinks(HttpSession session) {
        return adminService.getAllLinks(session);
    }

    @PostMapping("/link/delete")
    @ResponseBody
    public Map<String, Object> deleteLink(@RequestBody DeleteLinkRequest request, HttpSession session) {
        if (request == null) {
            return missingInformation();
        }
        return adminService.deleteLink(session, request.getId());
    }

    private Map<String, Object> missingInformation() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 0);
        response.put("msg", "Missing Information");
        return response;
    }
}
