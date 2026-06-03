package club.muimi.kicloud.controller;

import club.muimi.kicloud.model.LoginRequest;
import club.muimi.kicloud.model.RegisterRequest;
import club.muimi.kicloud.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController {
    private static final String LOGIN_FAILED_MESSAGE = "Login Failed";

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    @ResponseBody
    public Map<String, Object> login(@RequestBody LoginRequest loginRequest, HttpSession session) {
        if (loginRequest == null) {
            return loginFailedResponse();
        }
        if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
            return loginFailedResponse();
        }
        if (loginRequest.getId() != null) {
            return userService.login(loginRequest.getId(), loginRequest.getPassword(), session);
        }
        if (loginRequest.getUsername() != null && !loginRequest.getUsername().trim().isEmpty()) {
            return userService.login(loginRequest.getUsername(), loginRequest.getPassword(), session);
        }
        return loginFailedResponse();
    }

    @PostMapping("/logout")
    @ResponseBody
    public Map<String, Object> logout(HttpSession session) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (userService.logout(session)) {
            response.put("code", 1);
            response.put("msg", "Logout Successful");
        } else {
            response.put("code", 0);
            response.put("msg", "Not Logged In");
        }
        return response;
    }

    @PostMapping("/register")
    @ResponseBody
    public Map<String, Object> register(@RequestBody RegisterRequest registerRequest) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 0);
        if (registerRequest == null) {
            response.put("msg", "Missing Information");
            return response;
        }
        if (registerRequest.getPassword() == null || registerRequest.getPassword().trim().isEmpty()) {
            response.put("msg", "Missing Password");
            return response;
        }
        if (registerRequest.getUsername() == null || registerRequest.getUsername().trim().isEmpty()) {
            response.put("msg", "Missing Username");
            return response;
        }
        return userService.register(registerRequest.getUsername(), registerRequest.getPassword(), registerRequest.getInvitationCode());
    }

    private Map<String, Object> loginFailedResponse() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 0);
        response.put("msg", LOGIN_FAILED_MESSAGE);
        return response;
    }
}
