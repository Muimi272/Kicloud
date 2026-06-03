package club.muimi.kicloud.service;

import club.muimi.kicloud.dao.StorageFileDao;
import club.muimi.kicloud.dao.UserDao;
import club.muimi.kicloud.entity.StorageFile;
import club.muimi.kicloud.entity.User;
import club.muimi.kicloud.model.FileType;
import club.muimi.kicloud.model.LoginUser;
import club.muimi.kicloud.model.Role;
import club.muimi.kicloud.model.Slice;
import club.muimi.kicloud.model.Slices;
import club.muimi.kicloud.model.UploadSliceRequest;
import club.muimi.kicloud.model.UploadSlicesRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
    private final Path tempRootPath;
    private final ConcurrentMap<String, Slices> activeSlices = new ConcurrentHashMap<>();

    public StorageFileService(StorageFileDao storageFileDao,
                              UserDao userDao,
                              @Value("${kicloud.storage.root:./data/storage}") String storageRoot,
                              @Value("${kicloud.storage.temp-root:./data/temp}") String tempRoot) {
        this.storageFileDao = storageFileDao;
        this.userDao = userDao;
        this.storageRootPath = Paths.get(storageRoot).toAbsolutePath().normalize();
        this.tempRootPath = Paths.get(tempRoot).toAbsolutePath().normalize();
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

        UploadValidation validation = validateUpload(operator, multipartFile.getOriginalFilename(), parentId, multipartFile.getSize());
        if (!validation.isValid()) {
            return buildError(validation.errorMessage());
        }

        StorageSaveTarget saveTarget = prepareStorageSaveTarget(operator.getId(), validation.fileName());
        try {
            Path parentPath = saveTarget.targetPath().getParent();
            if (parentPath != null) {
                Files.createDirectories(parentPath);
            }
            multipartFile.transferTo(saveTarget.targetPath());
        } catch (IOException exception) {
            return buildError("Store file failed.");
        }

        try {
            StorageFile storageFile = saveStorageFileRecord(
                    operator,
                    validation.parent(),
                    validation.fileName(),
                    validation.fileSize(),
                    saveTarget.storageKey(),
                    validation.usedSpace()
            );
            return buildSuccess(Map.of("file", buildStorageFileDetail(storageFile)), "File uploaded successfully.");
        } catch (RuntimeException exception) {
            try {
                Files.deleteIfExists(saveTarget.targetPath());
            } catch (IOException ignored) {
            }
            throw exception;
        }
    }

    @Transactional
    public Map<String, Object> uploadSlices(UploadSlicesRequest request, LoginUser loginUser) {
        User operator = getCheckedUser(loginUser);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (request == null) {
            return buildError("Missing Information");
        }

        cleanupExpiredSliceUploads();

        Integer sliceCount = request.getSliceCount();
        if (sliceCount == null || sliceCount <= 0) {
            return buildError("Invalid slice count.");
        }

        String normalizedMd5 = normalizeMd5(request.getFullFileMD5());
        if (normalizedMd5 == null) {
            return buildError("Invalid file MD5.");
        }

        long totalSize = request.getTotalSize() == null ? -1L : request.getTotalSize();
        if (totalSize <= 0L) {
            return buildError("Invalid file size.");
        }

        UploadValidation validation = validateUpload(operator, request.getFileName(), request.getParentId(), totalSize);
        if (!validation.isValid()) {
            return buildError(validation.errorMessage());
        }

        try {
            Files.createDirectories(tempRootPath);
        } catch (IOException exception) {
            return buildError("Prepare slice upload failed.");
        }

        String uploadId = UUID.randomUUID().toString().replace("-", "");
        String sessionKey = buildSlicesKey(operator.getId(), uploadId);
        Slices slices = new Slices(
                sliceCount,
                normalizedMd5,
                operator.getId(),
                uploadId,
                validation.fileName(),
                request.getParentId(),
                totalSize
        );
        Slices previous = activeSlices.put(sessionKey, slices);
        if (previous != null) {
            cleanupSliceFiles(previous);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("uploadId", uploadId);
        data.put("sliceCount", sliceCount);
        data.put("fullFileMD5", normalizedMd5);
        data.put("fileName", validation.fileName());
        data.put("parentId", request.getParentId());
        data.put("totalSize", totalSize);
        return buildSuccess(data, "Slice upload session created.");
    }

    @Transactional
    public Map<String, Object> uploadSlice(UploadSliceRequest request, LoginUser loginUser) {
        User operator = getCheckedUser(loginUser);
        if (operator == null) {
            return buildError("Invalid session");
        }
        if (request == null) {
            return buildError("Missing Information");
        }

        cleanupExpiredSliceUploads();

        MultipartFile sliceFile = request.getSlice();
        if (sliceFile == null || sliceFile.isEmpty()) {
            return buildError("Missing Slice");
        }

        Integer sliceNum = request.getSliceNum();
        Integer sliceSize = request.getSize();
        if (sliceNum == null || sliceNum < 0) {
            return buildError("Invalid slice number.");
        }
        if (sliceSize == null || sliceSize <= 0) {
            return buildError("Invalid slice size.");
        }
        if (sliceFile.getSize() != sliceSize.longValue()) {
            return buildError("Slice size mismatch.");
        }

        String normalizedMd5 = normalizeMd5(request.getFullFileMD5());
        if (normalizedMd5 == null) {
            return buildError("Invalid file MD5.");
        }

        String uploadId = normalizeUploadId(request.getUploadId());
        if (uploadId == null) {
            return buildError("Invalid upload ID.");
        }

        String sessionKey = buildSlicesKey(operator.getId(), uploadId);
        Slices slices = activeSlices.get(sessionKey);
        if (slices == null) {
            return buildError("Slice upload session does not exist or has expired.");
        }

        synchronized (slices) {
            if (!Objects.equals(slices.getOwnerId(), operator.getId())) {
                return buildError("Slice upload session does not exist.");
            }
            if (!Objects.equals(slices.getUploadId(), uploadId)) {
                return buildError("Slice upload session does not exist.");
            }
            if (!Objects.equals(slices.getFullFileMD5(), normalizedMd5)) {
                return buildError("Slice upload session does not match file MD5.");
            }
            if (!slices.containsSlice(sliceNum)) {
                return buildError("Invalid slice number.");
            }
            if (slices.isExpired(LocalDateTime.now()) && !slices.isAllDone()) {
                if (activeSlices.remove(sessionKey, slices)) {
                    cleanupSliceFiles(slices);
                }
                return buildError("Slice upload session has expired.");
            }

            Slice existingSlice = slices.getSlice(sliceNum);
            if (existingSlice != null && existingSlice.isDone()) {
                return buildSliceUploadResponse(slices, sliceNum, "Slice already uploaded.");
            }

            Path slicePath = resolveTempPath(operator.getId() + "-" + uploadId + "-" + sliceNum + ".part");
            try {
                Files.createDirectories(tempRootPath);
                sliceFile.transferTo(slicePath);
            } catch (IOException exception) {
                deleteTempFile(slicePath);
                return buildError("Store slice failed.");
            }

            slices.addSlice(sliceNum, sliceSize, slicePath.toString());
            if (!slices.isAllDone()) {
                return buildSliceUploadResponse(slices, sliceNum, "Slice uploaded successfully.");
            }

            return finalizeSliceUpload(sessionKey, slices, operator);
        }
    }

    @Scheduled(fixedDelay = 60000L)
    public void cleanupExpiredSliceUploads() {
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, Slices> entry : activeSlices.entrySet()) {
            String sessionKey = entry.getKey();
            Slices slices = entry.getValue();
            if (slices == null || slices.isAllDone() || !slices.isExpired(now)) {
                continue;
            }
            synchronized (slices) {
                if (slices.isAllDone() || !slices.isExpired(LocalDateTime.now())) {
                    continue;
                }
                if (activeSlices.remove(sessionKey, slices)) {
                    cleanupSliceFiles(slices);
                }
            }
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

    private Map<String, Object> finalizeSliceUpload(String sessionKey, Slices slices, User operator) {
        Path mergedPath = null;
        Path storedPath = null;
        try {
            mergedPath = mergeSlices(slices);
            String mergedMd5 = calculateMd5(mergedPath);
            if (!slices.getFullFileMD5().equals(mergedMd5)) {
                return buildError("Merged file MD5 verification failed.");
            }

            long mergedSize = Files.size(mergedPath);
            UploadValidation validation = validateUpload(operator, slices.getFileName(), slices.getParentId(), mergedSize);
            if (!validation.isValid()) {
                return buildError(validation.errorMessage());
            }

            StorageSaveTarget saveTarget = prepareStorageSaveTarget(operator.getId(), validation.fileName());
            storedPath = saveTarget.targetPath();
            Path parentPath = storedPath.getParent();
            if (parentPath != null) {
                Files.createDirectories(parentPath);
            }
            moveFile(mergedPath, storedPath);
            mergedPath = null;

            try {
                StorageFile storageFile = saveStorageFileRecord(
                        operator,
                        validation.parent(),
                        validation.fileName(),
                        mergedSize,
                        saveTarget.storageKey(),
                        validation.usedSpace()
                );
                return buildSuccess(Map.of("file", buildStorageFileDetail(storageFile)), "File uploaded successfully.");
            } catch (RuntimeException exception) {
                deleteTempFile(storedPath);
                throw exception;
            }
        } catch (IOException exception) {
            return buildError("Merge slices failed.");
        } finally {
            activeSlices.remove(sessionKey, slices);
            cleanupSliceFiles(slices);
            deleteTempFile(mergedPath);
        }
    }

    private StorageFile saveStorageFileRecord(User operator,
                                              StorageFile parent,
                                              String originalFilename,
                                              long fileSize,
                                              String storageKey,
                                              long usedSpaceBeforeSave) {
        StorageFile storageFile = new StorageFile();
        storageFile.setName(originalFilename);
        storageFile.setParentId(parent == null ? null : parent.getId());
        storageFile.setStorageKey(storageKey);
        storageFile.setFileType(FileType.FILE);
        storageFile.setOwnerId(operator.getId());
        storageFile.setSize(fileSize);
        storageFile.setDeleted(false);
        storageFileDao.save(storageFile);

        operator.setUsedSpace(usedSpaceBeforeSave + fileSize);
        userDao.save(operator);
        return storageFile;
    }

    private UploadValidation validateUpload(User operator, String rawFileName, Long parentId, long fileSize) {
        if (operator == null) {
            return new UploadValidation("Invalid session", null, null, fileSize, 0L);
        }
        if (fileSize <= 0L) {
            return new UploadValidation("Empty File is not allowed.", null, null, fileSize, 0L);
        }

        String originalFilename = sanitizeName(rawFileName);
        if (originalFilename == null || originalFilename.isBlank()) {
            return new UploadValidation("Invalid File Name", null, null, fileSize, 0L);
        }

        StorageFile parent = validateParentFolder(parentId, operator.getId());
        if (parentId != null && parent == null) {
            return new UploadValidation("Parent Folder does not exist.", null, null, fileSize, 0L);
        }
        if (storageFileDao.existsActiveByOwnerIdAndParentIdAndName(operator.getId(), parentId, originalFilename)) {
            return new UploadValidation("File already exists in current folder.", null, null, fileSize, 0L);
        }

        long singleFileLimit = resolveSingleFileLimit(operator.getRole());
        if (fileSize > singleFileLimit) {
            return new UploadValidation("File exceeds role upload size limit.", null, null, fileSize, 0L);
        }

        long usedSpace = operator.getUsedSpace() == null ? 0L : operator.getUsedSpace();
        long totalSpace = operator.getTotalSpace() == null ? Long.MAX_VALUE : operator.getTotalSpace();
        if (usedSpace + fileSize > totalSpace) {
            return new UploadValidation("Insufficient storage space.", null, null, fileSize, 0L);
        }

        return new UploadValidation(null, originalFilename, parent, fileSize, usedSpace);
    }

    private StorageSaveTarget prepareStorageSaveTarget(Long userId, String originalFilename) {
        String storageKey = generateStorageKey(userId, originalFilename);
        Path targetPath = storageRootPath.resolve(storageKey).normalize();
        if (!targetPath.startsWith(storageRootPath)) {
            throw new IllegalStateException("Invalid storage path.");
        }
        return new StorageSaveTarget(storageKey, targetPath);
    }

    private Path mergeSlices(Slices slices) throws IOException {
        Path mergedPath = resolveTempPath(slices.getOwnerId() + "-" + slices.getUploadId() + ".merged");
        try (OutputStream outputStream = Files.newOutputStream(
                mergedPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            for (Slice slice : slices.getSlices()) {
                if (slice == null || !slice.isDone() || slice.getPath() == null || slice.getPath().isBlank()) {
                    throw new IOException("Missing slice content.");
                }
                Path slicePath = Paths.get(slice.getPath()).toAbsolutePath().normalize();
                if (!slicePath.startsWith(tempRootPath) || !Files.exists(slicePath) || !Files.isRegularFile(slicePath)) {
                    throw new IOException("Missing slice file.");
                }
                try (InputStream inputStream = Files.newInputStream(slicePath)) {
                    inputStream.transferTo(outputStream);
                }
            }
        }
        return mergedPath;
    }

    private void moveFile(Path sourcePath, Path targetPath) throws IOException {
        try {
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException moveException) {
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(sourcePath);
        }
    }

    private String calculateMd5(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("MD5 algorithm is unavailable.", exception);
        }
    }

    private Path resolveTempPath(String fileName) {
        Path path = tempRootPath.resolve(fileName).normalize();
        if (!path.startsWith(tempRootPath)) {
            throw new IllegalArgumentException("Invalid temp path.");
        }
        return path;
    }

    private void cleanupSliceFiles(Slices slices) {
        if (slices == null) {
            return;
        }
        for (Slice slice : slices.getSlices()) {
            if (slice == null || slice.getPath() == null || slice.getPath().isBlank()) {
                continue;
            }
            deleteTempFile(Paths.get(slice.getPath()).toAbsolutePath().normalize());
        }
    }

    private void deleteTempFile(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private Map<String, Object> buildSliceUploadResponse(Slices slices, int sliceNum, String message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sliceNum", sliceNum);
        data.put("allDone", slices.isAllDone());
        return buildSuccess(data, message);
    }

    private String normalizeMd5(String rawMd5) {
        if (rawMd5 == null) {
            return null;
        }
        String normalized = rawMd5.trim().toLowerCase();
        if (!normalized.matches("[a-f0-9]{32}")) {
            return null;
        }
        return normalized;
    }

    private String normalizeUploadId(String rawUploadId) {
        if (rawUploadId == null) {
            return null;
        }
        String normalized = rawUploadId.trim().toLowerCase();
        if (!normalized.matches("[a-f0-9]{32}")) {
            return null;
        }
        return normalized;
    }

    private String buildSlicesKey(Long ownerId, String uploadId) {
        return ownerId + ":" + uploadId;
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

    private record UploadValidation(String errorMessage,
                                    String fileName,
                                    StorageFile parent,
                                    long fileSize,
                                    long usedSpace) {
        private boolean isValid() {
            return errorMessage == null;
        }
    }

    private record StorageSaveTarget(String storageKey, Path targetPath) {
    }
}
