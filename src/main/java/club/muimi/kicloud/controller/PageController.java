package club.muimi.kicloud.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class PageController {
    @GetMapping("/user/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/share")
    public String share() {
        return "share";
    }

    @GetMapping("/share/file/{linkId}")
    public String shareFile(@PathVariable String linkId,
                            @RequestParam(value = "password", required = false) String password) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/link/{linkId}")
                .queryParamIfPresent("password", java.util.Optional.ofNullable(password));
        return "redirect:" + builder.buildAndExpand(linkId).toUriString();
    }

    @GetMapping("/user/register")
    public String register() {
        return "register";
    }

    @GetMapping("/admin/back")
    public String back() {
        return "back";
    }

    @GetMapping("/forbidden")
    public String forbidden() {
        return "forbidden";
    }
}
