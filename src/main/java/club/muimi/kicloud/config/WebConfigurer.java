package club.muimi.kicloud.config;

import club.muimi.kicloud.config.interceptor.AdminInterceptor;
import club.muimi.kicloud.config.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfigurer implements WebMvcConfigurer {

    private final LoginInterceptor loginHandlerInterceptor;
    private final AdminInterceptor adminHandlerInterceptor;

    public WebConfigurer(LoginInterceptor loginHandlerInterceptor, AdminInterceptor adminHandlerInterceptor) {
        this.loginHandlerInterceptor = loginHandlerInterceptor;
        this.adminHandlerInterceptor = adminHandlerInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginHandlerInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/login",
                        "/user/logout",
                        "/user/register",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/error",
                        "/forbidden",
                        "/link/**"
                );
        registry.addInterceptor(adminHandlerInterceptor)
                .addPathPatterns("/admin/**");
    }
}
