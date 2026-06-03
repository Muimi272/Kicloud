package club.muimi.kicloud.service;

import club.muimi.kicloud.dao.StorageFileDao;
import club.muimi.kicloud.dao.UserDao;
import club.muimi.kicloud.entity.StorageFile;
import club.muimi.kicloud.entity.User;
import club.muimi.kicloud.model.FileType;
import club.muimi.kicloud.model.LoginUser;
import club.muimi.kicloud.model.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StorageFileService {
    private static final long USER_SINGLE_FILE_LIMIT = 200L * 1024L * 1024L;
    private static final long ADMIN_SINGLE_FILE_LIMIT = 1024L * 1024L * 1024L;
    private static final long SUPERADMIN_SINGLE_FILE_LIMIT = 10L * 1024L * 1024L * 1024L;
    private static final DateTimeFormatter STORAGE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

    private final StorageFileDao storageFileDao;
    private final UserDao userDao;
    private final Path storageRootPath;

    public StorageFileService(StorageFileDao storageFileDao,
                              UserDao userDao,
                              @Value("${kicloud.storage.root:./data/storage}") String storageRoot) {
        this.storageFileDao = storageFileDao;
        this.userDao = userDao;
        this.storageRootPath = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    @Transactional
    public Map<String, Object> uploadFile(MultipartFile multipartFile, Long parentId, LoginUser loginUser) {
        User operator = getCheckedUser(loginUser);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (multipartFile == null || multipartFile.isEmpty()) {
            return buildError("Missing File");
        }
        String originalFilename = sanitizeName(multipartFile.getOriginalFilename());
        if (originalFilename == null || originalFilename.isBlank()) {
            return buildError("Invalid File Name");
        }
        StorageFile parent = validateParentFolder(parentId, operator.getId());
        if (parentId != null && parent == null) {
            return buildError("Parent Folder does not exist.");
        }
        if (storageFileDao.existsActiveByOwnerIdAndParentIdAndName(operator.getId(), parentId, originalFilename)) {
            return buildError("File already exists in current folder.");
        }

        long fileSize = multipartFile.getSize();
        if (fileSize <= 0) {
            return buildError("Empty File is not allowed.");
        }
        long singleFileLimit = resolveSingleFileLimit(operator.getRole());
        if (fileSize > singleFileLimit) {
            return buildError("File exceeds role upload size limit.");
        }
        long usedSpace = operator.getUsedSpace() == null ? 0L : operator.getUsedSpace();
        long totalSpace = operator.getTotalSpace() == null ? Long.MAX_VALUE : operator.getTotalSpace();
        if (usedSpace + fileSize > totalSpace) {
            return buildError("Insufficient storage space.");
        }

        String storageKey = generateStorageKey(operator.getId(), originalFilename);
        Path targetPath = storageRootPath.resolve(storageKey).normalize();
        if (!targetPath.startsWith(storageRootPath)) {
            return buildError("Invalid storage path.");
        }

        try {
            Path parentPath = targetPath.getParent();
            if (parentPath != null) {
                Files.createDirectories(parentPath);
            }
            multipartFile.transferTo(targetPath);
        } catch (IOException exception) {
            return buildError("Store file failed.");
        }

        try {
            StorageFile storageFile = new StorageFile();
            storageFile.setName(originalFilename);
            storageFile.setParentId(parent == null ? null : parent.getId());
            storageFile.setStorageKey(storageKey);
            storageFile.setFileType(FileType.FILE);
            storageFile.setOwnerId(operator.getId());
            storageFile.setSize(fileSize);
            storageFile.setDeleted(false);
            storageFileDao.save(storageFile);

            operator.setUsedSpace(usedSpace + fileSize);
            userDao.save(operator);
            return buildSuccess(Map.of("file", buildStorageFileDetail(storageFile)), "File uploaded successfully.");
        } catch (RuntimeException exception) {
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException ignored) {
            }
            throw exception;
        }
    }

    @Transactional
    public Map<String, Object> createFolder(String name, Long parentId, LoginUser loginUser) {
        User operator = getCheckedUser(loginUser);
        if (operator == null) {
            return buildError("Invalid session");
        }
        String folderName = sanitizeName(name);
        if (folderName == null || folderName.isBlank()) {
            return buildError("Missing Folder Name");
        }
        StorageFile parent = validateParentFolder(parentId, operator.getId());
        if (parentId != null && parent == null) {
            return buildError("Parent Folder does not exist.");
        }
        if (storageFileDao.existsActiveByOwnerIdAndParentIdAndName(operator.getId(), parentId, folderName)) {
            return buildError("Folder already exists in current folder.");
        }

        StorageFile folder = new StorageFile();
        folder.setName(folderName);
        folder.setParentId(parent == null ? null : parent.getId());
        folder.setFileType(FileType.FOLDER);
        folder.setOwnerId(operator.getId());
        folder.setDeleted(false);
        folder.setSize(0L);
        storageFileDao.save(folder);
        return buildSuccess(Map.of("file", buildStorageFileDetail(folder)), "Folder created successfully.");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listFiles(Long parentId, LoginUser loginUser) {
        User operator = getCheckedUser(loginUser);
        if (operator == null) {
            return buildError("Invalid session");
        }
        StorageFile parent = validateParentFolder(parentId, operator.getId());
        if (parentId != null && parent == null) {
            return buildError("Parent Folder does not exist.");
        }

        return buildFileListResponse(operator, operator.getId(), parentId, "Files loaded successfully.");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> searchFiles(String keyword, LoginUser loginUser) {
        User operator = getCheckedUser(loginUser);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            return buildError("Missing Keyword");
        }

        List<Map<String, Object>> files = storageFileDao
                .findByOwnerIdAndNameContainingIgnoreCaseAndDeletedFalseOrderByFileTypeAscCreatedAtDesc(operator.getId(), keyword.trim())
                .stream()
                .map(this::buildStorageFileSummary)
                .toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("keyword", keyword.trim());
        data.put("files", files);
        data.put("storage", buildStorageSummary(operator));
        return buildSuccess(data, "Search completed successfully.");
    }

    @Transactional
    public Map<String, Object> renameFile(Long id, String newName, LoginUser loginUser) {
        User operator = getCheckedUser(loginUser);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (id == null) {
            return buildError("Missing File ID");
        }
        String finalName = sanitizeName(newName);
        if (finalName == null || finalName.isBlank()) {
            return buildError("Missing New Name");
        }

        StorageFile storageFile = storageFileDao.findByIdAndOwnerIdAndDeletedFalse(id, operator.getId()).orElse(null);
        if (storageFile == null) {
            return buildError("File does not exist.");
        }
        if (storageFileDao.existsActiveSiblingNameExcludingId(operator.getId(), storageFile.getParentId(), finalName, storageFile.getId())) {
            return buildError("File name already exists in current folder.");
        }

        storageFile.setName(finalName);
        storageFileDao.save(storageFile);
        return buildSuccess(Map.of("file", buildStorageFileDetail(storageFile)), "File renamed successfully.");
    }

    @Transactional
    public Map<String, Object> deleteFile(Long id, LoginUser loginUser) {
        User operator = getCheckedUser(loginUser);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (id == null) {
            return buildError("Missing File ID");
        }

        StorageFile storageFile = storageFileDao.findByIdAndOwnerIdAndDeletedFalse(id, operator.getId()).orElse(null);
        if (storageFile == null) {
            return buildError("File does not exist.");
        }

        if (storageFile.getFileType() == FileType.FOLDER) {
            List<StorageFile> children = storageFileDao.findActiveByOwnerIdAndParentId(operator.getId(), storageFile.getId());
            if (!children.isEmpty()) {
                return buildError("Folder is not empty.");
            }
        } else if (storageFile.getStorageKey() != null && !storageFile.getStorageKey().isBlank()) {
            Path targetPath = storageRootPath.resolve(storageFile.getStorageKey()).normalize();
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException exception) {
                return buildError("Delete file content failed.");
            }
            long size = storageFile.getSize() == null ? 0L : storageFile.getSize();
            long usedSpace = operator.getUsedSpace() == null ? 0L : operator.getUsedSpace();
            operator.setUsedSpace(Math.max(0L, usedSpace - size));
            userDao.save(operator);
        }

        storageFile.setDeleted(true);
        storageFile.setDeletedAt(LocalDateTime.now());
        storageFileDao.save(storageFile);
        return buildSuccess(Map.of("file", buildStorageFileDetail(storageFile)), "File deleted successfully.");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStorageSummary(LoginUser loginUser) {
        User operator = getCheckedUser(loginUser);
        if (operator == null) {
            return buildError("Invalid session");
        }
        return buildSuccess(Map.of("storage", buildStorageSummary(operator)), "Storage summary loaded successfully.");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listFilesByOwnerForSuperAdmin(Long ownerId, Long parentId, LoginUser loginUser) {
        User operator = requireSuperAdmin(loginUser);
        if (operator == null) {
            return buildError("Forbidden");
        }
        User targetUser = userDao.findById(ownerId).orElse(null);
        if (targetUser == null) {
            return buildError("User does not exist.");
        }
        StorageFile parent = validateParentFolder(parentId, targetUser.getId());
        if (parentId != null && parent == null) {
            return buildError("Parent Folder does not exist.");
        }
        return buildFileListResponse(targetUser, targetUser.getId(), parentId, "User files loaded successfully.");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> searchFilesByOwnerForSuperAdmin(Long ownerId, String keyword, LoginUser loginUser) {
        User operator = requireSuperAdmin(loginUser);
        if (operator == null) {
            return buildError("Forbidden");
        }
        if (ownerId == null) {
            return buildError("Missing User ID");
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            return buildError("Missing Keyword");
        }
        User targetUser = userDao.findById(ownerId).orElse(null);
        if (targetUser == null) {
            return buildError("User does not exist.");
        }

        List<Map<String, Object>> files = storageFileDao
                .findByOwnerIdAndNameContainingIgnoreCaseAndDeletedFalseOrderByFileTypeAscCreatedAtDesc(targetUser.getId(), keyword.trim())
                .stream()
                .map(this::buildStorageFileSummary)
                .toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("keyword", keyword.trim());
        data.put("owner", buildUserStorageProfile(targetUser));
        data.put("files", files);
        return buildSuccess(data, "User file search completed successfully.");
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadFile(Long id, LoginUser loginUser) {
        User operator = getCheckedUser(loginUser);
        if (operator == null) {
            return ResponseEntity.status(401).build();
        }
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

        StorageFile storageFile = storageFileDao.findByIdAndOwnerIdAndDeletedFalse(id, operator.getId()).orElse(null);
        if (storageFile == null) {
            return ResponseEntity.notFound().build();
        }
        return buildDownloadResponse(storageFile);
    }

    @Transactional(readOnly = true)
    public StorageFile getActiveFileForLink(Long id) {
        if (id == null) {
            return null;
        }
        StorageFile storageFile = storageFileDao.findByIdAndDeletedFalse(id).orElse(null);
        if (storageFile == null || storageFile.getFileType() != FileType.FILE) {
            return null;
        }
        if (storageFile.getStorageKey() == null || storageFile.getStorageKey().isBlank()) {
            return null;
        }
        Path targetPath = storageRootPath.resolve(storageFile.getStorageKey()).normalize();
        if (!targetPath.startsWith(storageRootPath) || !Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            return null;
        }
        return storageFile;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadSharedFile(Long id) {
        StorageFile storageFile = getActiveFileForLink(id);
        if (storageFile == null) {
            return ResponseEntity.notFound().build();
        }
        return buildDownloadResponse(storageFile);
    }

    public Map<String, Object> buildStorageFileSummary(StorageFile storageFile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", storageFile.getId());
        map.put("name", storageFile.getName());
        map.put("parentId", storageFile.getParentId());
        map.put("fileType", storageFile.getFileType());
        map.put("ownerId", storageFile.getOwnerId());
        map.put("size", storageFile.getSize());
        map.put("deleted", storageFile.isDeleted());
        map.put("createdAt", storageFile.getCreatedAt());
        map.put("updatedAt", storageFile.getUpdatedAt());
        return map;
    }

    private ResponseEntity<Resource> buildDownloadResponse(StorageFile storageFile) {
        if (storageFile == null || storageFile.getFileType() != FileType.FILE) {
            return ResponseEntity.notFound().build();
        }
        if (storageFile.getStorageKey() == null || storageFile.getStorageKey().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        Path targetPath = storageRootPath.resolve(storageFile.getStorageKey()).normalize();
        if (!targetPath.startsWith(storageRootPath) || !Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            return ResponseEntity.notFound().build();
        }

        try {
            Resource resource = new UrlResource(targetPath.toUri());
            String encodedFileName = URLEncoder.encode(storageFile.getName(), StandardCharsets.UTF_8).replace("+", "%20");
            String contentType = Files.probeContentType(targetPath);
            MediaType mediaType = (contentType == null || contentType.isBlank())
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(contentType);
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(Files.size(targetPath)))
                    .body(resource);
        } catch (IOException exception) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private Map<String, Object> buildStorageFileDetail(StorageFile storageFile) {
        Map<String, Object> map = buildStorageFileSummary(storageFile);
        map.put("storageKey", storageFile.getStorageKey());
        map.put("deletedAt", storageFile.getDeletedAt());
        return map;
    }

    private Map<String, Object> buildFileListResponse(User operator, Long ownerId, Long parentId, String message) {
        List<Map<String, Object>> files = storageFileDao.findActiveByOwnerIdAndParentId(ownerId, parentId)
                .stream()
                .map(this::buildStorageFileSummary)
                .collect(Collectors.toList());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("parentId", parentId);
        data.put("files", files);
        data.put("storage", buildStorageSummary(operator));
        data.put("owner", buildUserStorageProfile(operator));
        return buildSuccess(data, message);
    }

    private Map<String, Object> buildStorageSummary(User user) {
        Map<String, Object> summary = new LinkedHashMap<>();
        long usedSpace = user.getUsedSpace() == null ? 0L : user.getUsedSpace();
        long totalSpace = user.getTotalSpace() == null ? 0L : user.getTotalSpace();
        summary.put("usedSpace", usedSpace);
        summary.put("totalSpace", totalSpace);
        summary.put("remainingSpace", Math.max(0L, totalSpace - usedSpace));
        return summary;
    }

    private Map<String, Object> buildUserStorageProfile(User user) {
        Map<String, Object> owner = new LinkedHashMap<>();
        owner.put("id", user.getId());
        owner.put("username", user.getUsername());
        owner.put("role", user.getRole());
        owner.putAll(buildStorageSummary(user));
        return owner;
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

    private StorageFile validateParentFolder(Long parentId, Long ownerId) {
        if (parentId == null) {
            return null;
        }
        StorageFile parent = storageFileDao.findByIdAndOwnerIdAndDeletedFalse(parentId, ownerId).orElse(null);
        if (parent == null || parent.getFileType() != FileType.FOLDER) {
            return null;
        }
        return parent;
    }

    private long resolveSingleFileLimit(Role role) {
        if (role == Role.SUPERADMIN) {
            return SUPERADMIN_SINGLE_FILE_LIMIT;
        }
        if (role == Role.ADMIN) {
            return ADMIN_SINGLE_FILE_LIMIT;
        }
        return USER_SINGLE_FILE_LIMIT;
    }

    private String generateStorageKey(Long userId, String originalFilename) {
        String extension = extractExtension(originalFilename);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String fileName = System.currentTimeMillis() + "_" + userId + "_" + suffix + extension;
        String yearMonth = LocalDateTime.now().format(STORAGE_PATH_FORMATTER);
        return userId + "/" + yearMonth + "/" + fileName;
    }

    private String extractExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index);
    }

    private String sanitizeName(String rawName) {
        if (rawName == null) {
            return null;
        }
        String fileName = Paths.get(rawName).getFileName().toString().trim();
        if (fileName.isEmpty()) {
            return null;
        }
        if (fileName.equals(".") || fileName.equals("..")) {
            return null;
        }
        if (fileName.contains("/") || fileName.contains("\\")) {
            return null;
        }
        return fileName;
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
