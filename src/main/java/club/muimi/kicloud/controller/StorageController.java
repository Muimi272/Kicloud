package club.muimi.kicloud.controller;

import club.muimi.kicloud.model.CreateFolderRequest;
import club.muimi.kicloud.model.DeleteStorageFileRequest;
import club.muimi.kicloud.model.LoginUser;
import club.muimi.kicloud.model.RenameStorageFileRequest;
import club.muimi.kicloud.service.StorageFileService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/storage")
public class StorageController {
    private final StorageFileService storageFileService;

    public StorageController(StorageFileService storageFileService) {
        this.storageFileService = storageFileService;
    }

    @PostMapping("/upload")
    @ResponseBody
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "parentId", required = false) Long parentId,
                                      HttpSession session) {
        return storageFileService.uploadFile(file, parentId, getLoginUser(session));
    }

    @PostMapping("/folder")
    @ResponseBody
    public Map<String, Object> createFolder(@RequestBody CreateFolderRequest request, HttpSession session) {
        if (request == null) {
            return missingInformation();
        }
        return storageFileService.createFolder(request.getName(), request.getParentId(), getLoginUser(session));
    }

    @GetMapping("/list")
    @ResponseBody
    public Map<String, Object> list(@RequestParam(value = "parentId", required = false) Long parentId,
                                    HttpSession session) {
        return storageFileService.listFiles(parentId, getLoginUser(session));
    }

    @GetMapping("/search")
    @ResponseBody
    public Map<String, Object> search(@RequestParam(value = "keyword", required = false) String keyword,
                                      HttpSession session) {
        return storageFileService.searchFiles(keyword, getLoginUser(session));
    }

    @PostMapping("/rename")
    @ResponseBody
    public Map<String, Object> rename(@RequestBody RenameStorageFileRequest request, HttpSession session) {
        if (request == null) {
            return missingInformation();
        }
        return storageFileService.renameFile(request.getId(), request.getNewName(), getLoginUser(session));
    }

    @PostMapping("/delete")
    @ResponseBody
    public Map<String, Object> delete(@RequestBody DeleteStorageFileRequest request, HttpSession session) {
        if (request == null) {
            return missingInformation();
        }
        return storageFileService.deleteFile(request.getId(), getLoginUser(session));
    }

    @GetMapping("/summary")
    @ResponseBody
    public Map<String, Object> summary(HttpSession session) {
        return storageFileService.getStorageSummary(getLoginUser(session));
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam("id") Long id, HttpSession session) {
        return storageFileService.downloadFile(id, getLoginUser(session));
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
