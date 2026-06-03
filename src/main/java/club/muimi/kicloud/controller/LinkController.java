package club.muimi.kicloud.controller;

import club.muimi.kicloud.model.CreateLinkRequest;
import club.muimi.kicloud.model.DeleteLinkRequest;
import club.muimi.kicloud.model.LoginUser;
import club.muimi.kicloud.service.LinkService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
@RequestMapping("/link")
public class LinkController {
    private final LinkService linkService;

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
    }

    @PostMapping("/create")
    @ResponseBody
    public Map<String, Object> create(@RequestBody CreateLinkRequest request, HttpSession session) {
        if (request == null) {
            return missingInformation();
        }
        return linkService.createLink(request.getStorageFileId(), request.getPassword(), getLoginUser(session));
    }

    @GetMapping("/my")
    @ResponseBody
    public Map<String, Object> myLinks(HttpSession session) {
        return linkService.getMyLinks(getLoginUser(session));
    }

    @PostMapping("/delete")
    @ResponseBody
    public Map<String, Object> delete(@RequestBody DeleteLinkRequest request, HttpSession session) {
        if (request == null) {
            return missingInformation();
        }
        return linkService.deleteOwnLink(request.getId(), getLoginUser(session));
    }

    @GetMapping("/{linkId}")
    public String openSharePage(@PathVariable String linkId,
                                @RequestParam(value = "password", required = false) String password,
                                @RequestParam(value = "error", required = false) String error,
                                Model model) {
        Map<String, Object> link = linkService.getPublicLinkView(linkId);
        model.addAttribute("linkId", linkId);
        model.addAttribute("presetPassword", password == null ? "" : password);
        model.addAttribute("downloadError", error == null ? "" : error);
        model.addAttribute("linkAvailable", link != null);
        model.addAttribute("linkData", link);
        return "share-file";
    }

    @GetMapping("/{linkId}/detail")
    @ResponseBody
    public Map<String, Object> detail(@PathVariable String linkId) {
        return linkService.getPublicLinkDetail(linkId);
    }

    @PostMapping("/{linkId}/download")
    public Object download(@PathVariable String linkId,
                           @RequestParam(value = "password", required = false) String password) {
        String validationMessage = linkService.validateDownloadRequest(linkId, password);
        if (validationMessage != null) {
            return redirectWithError(linkId, password, validationMessage);
        }
        return linkService.downloadByLinkId(linkId, password);
    }

    @GetMapping("/{linkId}/download")
    public Object downloadByQuery(@PathVariable String linkId,
                                  @RequestParam(value = "password", required = false) String password) {
        String validationMessage = linkService.validateDownloadRequest(linkId, password);
        if (validationMessage != null) {
            return redirectWithError(linkId, password, validationMessage);
        }
        return linkService.downloadByLinkId(linkId, password);
    }

    private String redirectWithError(String linkId, String password, String message) {
        StringBuilder builder = new StringBuilder("redirect:/link/").append(linkId).append("?error=")
                .append(org.springframework.web.util.UriUtils.encode(message, java.nio.charset.StandardCharsets.UTF_8));
        if (password != null && !password.isBlank()) {
            builder.append("&password=")
                    .append(org.springframework.web.util.UriUtils.encode(password, java.nio.charset.StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    private LoginUser getLoginUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object loginUser = session.getAttribute("LoginUser");
        if (loginUser instanceof LoginUser casted) {
            return casted;
        }
        return null;
    }

    private Map<String, Object> missingInformation() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 0);
        response.put("msg", "Missing Information");
        return response;
    }
}
