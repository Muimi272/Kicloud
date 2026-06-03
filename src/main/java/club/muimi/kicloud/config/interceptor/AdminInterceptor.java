package club.muimi.kicloud.config.interceptor;

import club.muimi.kicloud.entity.User;
import club.muimi.kicloud.model.LoginUser;
import club.muimi.kicloud.model.Role;
import club.muimi.kicloud.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final UserService userService;

    public AdminInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("LoginUser") == null) {
            userService.clearLoginState(session);
            response.sendRedirect("/user/login");
            return false;
        }
        LoginUser loginUser = (LoginUser) session.getAttribute("LoginUser");
        if (userService.isLoginUserIllegal(loginUser)) {
            userService.clearLoginState(session);
            response.sendRedirect("/user/login?reason=session-expired");
            return false;
        }
        User user = userService.getUserByLoginUser(loginUser);
        if (user == null || user.getRole() != Role.ADMIN && user.getRole() != Role.SUPERADMIN) {
            userService.clearLoginState(session);
            response.sendRedirect("/forbidden");
            return false;
        }
        return true;
    }
}
