package club.muimi.kicloud.service;

import club.muimi.kicloud.dao.LinkDao;
import club.muimi.kicloud.dao.UserDao;
import club.muimi.kicloud.entity.Link;
import club.muimi.kicloud.entity.StorageFile;
import club.muimi.kicloud.entity.User;
import club.muimi.kicloud.model.LoginUser;
import club.muimi.kicloud.model.Role;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class LinkService {
    private static final String LINK_ID_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int LINK_ID_LENGTH = 12;

    private final LinkDao linkDao;
    private final UserDao userDao;
    private final StorageFileService storageFileService;
    private final PasswordEncoder passwordEncoder;

    public LinkService(LinkDao linkDao,
                       UserDao userDao,
                       StorageFileService storageFileService,
                       PasswordEncoder passwordEncoder) {
        this.linkDao = linkDao;
        this.userDao = userDao;
        this.storageFileService = storageFileService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Map<String, Object> createLink(Long storageFileId, String password, LoginUser loginUser) {
        User operator = getCheckedUser(loginUser);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (storageFileId == null) {
            return buildError("Missing Storage File ID");
        }

        StorageFile storageFile = storageFileService.getActiveFileForLink(storageFileId);
        if (storageFile == null) {
            return buildError("File does not exist.");
        }
        if (!Objects.equals(storageFile.getOwnerId(), operator.getId()) && operator.getRole() != Role.SUPERADMIN) {
            return buildError("Can not share this file.");
        }

        Link link = new Link();
        link.setLinkId(generateUniqueLinkId());
        link.setStorageFileId(storageFile.getId());
        link.setOwnerId(storageFile.getOwnerId());
        link.setOwnerName(resolveOwnerName(storageFile.getOwnerId(), operator));
        link.setPassword(normalizePassword(password));
        link.setDeleted(false);
        link.setDownloadTimes(0L);
        linkDao.save(link);
        return buildSuccess(Map.of("link", buildLinkDetail(link)), "Link created successfully.");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMyLinks(LoginUser loginUser) {
        User operator = getCheckedUser(loginUser);
        if (operator == null) {
            return buildError("Invalid session");
        }
        List<Map<String, Object>> links = linkDao.findByOwnerIdAndDeletedFalseOrderByCreatedAtDesc(operator.getId())
                .stream()
                .map(this::buildLinkSummary)
                .collect(Collectors.toList());
        return buildSuccess(Map.of("links", links), "Links loaded successfully.");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAllLinksForSuperAdmin(LoginUser loginUser) {
        User operator = requireSuperAdmin(loginUser);
        if (operator == null) {
            return buildError("Forbidden");
        }
        List<Map<String, Object>> links = linkDao.findByDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .map(this::buildLinkSummary)
                .collect(Collectors.toList());
        return buildSuccess(Map.of("links", links), "Links loaded successfully.");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPublicLinkDetail(String linkId) {
        Link link = getActiveLink(linkId);
        if (link == null) {
            return buildError("Link does not exist.");
        }
        StorageFile storageFile = storageFileService.getActiveFileForLink(link.getStorageFileId());
        if (storageFile == null) {
            return buildError("File does not exist.");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("link", buildPublicLinkDetail(link, storageFile));
        return buildSuccess(data, "Link loaded successfully.");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPublicLinkView(String linkId) {
        Link link = getActiveLink(linkId);
        if (link == null) {
            return null;
        }
        StorageFile storageFile = storageFileService.getActiveFileForLink(link.getStorageFileId());
        if (storageFile == null) {
            return null;
        }
        return buildPublicLinkDetail(link, storageFile);
    }

    @Transactional(readOnly = true)
    public String validateDownloadRequest(String linkId, String password) {
        Link link = getActiveLink(linkId);
        if (link == null) {
            return "分享链接不存在或已失效。";
        }
        StorageFile storageFile = storageFileService.getActiveFileForLink(link.getStorageFileId());
        if (storageFile == null) {
            return "分享文件不存在或已失效。";
        }
        if (hasPassword(link)) {
            if (password == null || password.isBlank()) {
                return "请输入分享密码后再下载。";
            }
            if (!passwordEncoder.matches(password, link.getPassword())) {
                return "分享密码错误，请重新输入。";
            }
        }
        return null;
    }

    @Transactional
    public Map<String, Object> deleteOwnLink(Long id, LoginUser loginUser) {
        User operator = getCheckedUser(loginUser);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (id == null) {
            return buildError("Missing Link ID");
        }
        Link link = linkDao.findByIdAndOwnerIdAndDeletedFalse(id, operator.getId()).orElse(null);
        if (link == null) {
            return buildError("Link does not exist.");
        }
        softDeleteLink(link);
        return buildSuccess(Map.of("link", buildLinkDetail(link)), "Link deleted successfully.");
    }

    @Transactional
    public Map<String, Object> deleteLinkForSuperAdmin(Long id, LoginUser loginUser) {
        User operator = requireSuperAdmin(loginUser);
        if (operator == null) {
            return buildError("Forbidden");
        }
        if (id == null) {
            return buildError("Missing Link ID");
        }
        Link link = linkDao.findByIdAndDeletedFalse(id).orElse(null);
        if (link == null) {
            return buildError("Link does not exist.");
        }
        softDeleteLink(link);
        return buildSuccess(Map.of("link", buildLinkDetail(link)), "Link deleted successfully.");
    }

    @Transactional
    public ResponseEntity<Resource> downloadByLinkId(String linkId, String password) {
        Link link = getActiveLink(linkId);
        if (link == null) {
            return ResponseEntity.notFound().build();
        }
        if (hasPassword(link)) {
            if (password == null || password.isBlank()) {
                return ResponseEntity.status(403).build();
            }
            if (!passwordEncoder.matches(password, link.getPassword())) {
                return ResponseEntity.status(403).build();
            }
        }
        StorageFile storageFile = storageFileService.getActiveFileForLink(link.getStorageFileId());
        if (storageFile == null) {
            return ResponseEntity.notFound().build();
        }
        link.setDownloadTimes(link.getDownloadTimes() == null ? 1L : link.getDownloadTimes() + 1L);
        linkDao.save(link);
        return storageFileService.downloadSharedFile(storageFile.getId());
    }

    public Map<String, Object> buildLinkSummary(Link link) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", link.getId());
        map.put("linkId", link.getLinkId());
        map.put("storageFileId", link.getStorageFileId());
        map.put("ownerId", link.getOwnerId());
        map.put("ownerName", link.getOwnerName());
        map.put("downloadTimes", link.getDownloadTimes());
        map.put("hasPassword", hasPassword(link));
        map.put("deleted", link.isDeleted());
        map.put("createdAt", link.getCreatedAt());
        return map;
    }

    public Map<String, Object> buildLinkDetail(Link link) {
        Map<String, Object> map = buildLinkSummary(link);
        map.put("deletedAt", link.getDeletedAt());
        map.put("sharePath", "/link/" + link.getLinkId());
        map.put("downloadPath", "/link/" + link.getLinkId() + "/download");
        return map;
    }

    private Map<String, Object> buildPublicLinkDetail(Link link, StorageFile storageFile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("linkId", link.getLinkId());
        map.put("storageFileId", link.getStorageFileId());
        map.put("ownerName", link.getOwnerName());
        map.put("downloadTimes", link.getDownloadTimes());
        map.put("hasPassword", hasPassword(link));
        map.put("createdAt", link.getCreatedAt());
        map.put("fileName", storageFile.getName());
        map.put("fileSize", storageFile.getSize());
        map.put("sharePath", "/link/" + link.getLinkId());
        map.put("downloadPath", "/link/" + link.getLinkId() + "/download");
        return map;
    }

    private void softDeleteLink(Link link) {
        link.setDeleted(true);
        link.setDeletedAt(LocalDateTime.now());
        linkDao.save(link);
    }

    private boolean hasPassword(Link link) {
        return link.getPassword() != null && !link.getPassword().isBlank();
    }

    private String normalizePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            return null;
        }
        return passwordEncoder.encode(rawPassword.trim());
    }

    private String resolveOwnerName(Long ownerId, User operator) {
        if (ownerId == null) {
            return operator.getUsername();
        }
        Optional<User> targetOwner = userDao.findById(ownerId);
        return targetOwner.map(User::getUsername).orElse(operator.getUsername());
    }

    private String generateUniqueLinkId() {
        String candidate;
        do {
            candidate = randomLinkId();
        } while (linkDao.existsByLinkId(candidate));
        return candidate;
    }

    private String randomLinkId() {
        StringBuilder builder = new StringBuilder(LINK_ID_LENGTH);
        for (int index = 0; index < LINK_ID_LENGTH; index++) {
            int randomIndex = ThreadLocalRandom.current().nextInt(LINK_ID_ALPHABET.length());
            builder.append(LINK_ID_ALPHABET.charAt(randomIndex));
        }
        return builder.toString();
    }

    private Link getActiveLink(String linkId) {
        if (linkId == null || linkId.trim().isEmpty()) {
            return null;
        }
        return linkDao.findByLinkIdAndDeletedFalse(linkId.trim()).orElse(null);
    }

    private User getCheckedUser(LoginUser loginUser) {
        if (loginUser == null || loginUser.getId() == null || loginUser.getRole() == null) {
            return null;
        }
        Optional<User> optionalUser = userDao.findById(loginUser.getId());
        if (optionalUser.isEmpty()) {
            return null;
        }
        User user = optionalUser.get();
        if (!user.isEnabled()) {
            return null;
        }
        if (!Objects.equals(user.getUsername(), loginUser.getUsername())) {
            return null;
        }
        if (user.getRole() != loginUser.getRole()) {
            return null;
        }
        return user;
    }

    private User requireSuperAdmin(LoginUser loginUser) {
        User user = getCheckedUser(loginUser);
        if (user == null || user.getRole() != Role.SUPERADMIN) {
            return null;
        }
        return user;
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
}
