# StorageFileService类说明

代码位置：`src/main/java/club/muimi/kicloud/service/StorageFileService.java`

---

## 业务含义

`StorageFileService` 是 KiCloud 文件存储模块的核心服务，负责：

- 单文件上传
- 分片上传会话创建、切片接收、合并校验与过期清理
- 文件夹创建
- 目录列表与搜索
- 文件 / 文件夹重命名
- 文件 / 空文件夹删除
- 用户存储空间统计
- 普通下载与分享下载前置校验

如果需要查看跨 `Controller`、`Service`、`Dao` 的完整文件存储流程，可进一步参考本目录中的 [存储模块说明](./存储模块说明.md)。

---

## 公开方法说明

- `Map<String, Object> uploadFile(MultipartFile multipartFile, Long parentId, LoginUser loginUser)`
> 处理单个完整文件上传；会完成登录用户校验、父目录校验、文件名清理、同目录重名校验、大小与容量校验，并把文件落盘后写入数据库。

- `Map<String, Object> uploadSlices(UploadSlicesRequest request, LoginUser loginUser)`
> 创建一次分片上传会话；会校验完整文件 MD5、切片数量、文件名、父目录、角色上传上限和剩余容量，并返回后续上传切片所需的 `uploadId`。

- `Map<String, Object> uploadSlice(UploadSliceRequest request, LoginUser loginUser)`
> 接收单个切片内容；会根据当前用户和 `uploadId` 定位分片会话，把切片写入临时目录，更新切片状态，并在全部切片到齐后触发合并、MD5 校验和最终入库。

- `void cleanupExpiredSliceUploads()`
> 定时清理过期的分片上传会话和残留切片文件；同时也会在开始上传分片或继续上传切片时被主动调用一次。

- `Map<String, Object> createFolder(String name, Long parentId, LoginUser loginUser)`
> 为当前用户创建新的文件夹节点；会校验名称合法性、父目录合法性与同级重名冲突。

- `Map<String, Object> listFiles(Long parentId, LoginUser loginUser)`
> 查询当前登录用户指定目录下的文件列表；当 `parentId` 为空时表示查询根目录。

- `Map<String, Object> searchFiles(String keyword, LoginUser loginUser)`
> 按关键字搜索当前登录用户名下的未删除文件或文件夹，并返回存储空间摘要。

- `Map<String, Object> renameFile(Long id, String newName, LoginUser loginUser)`
> 修改当前用户文件或文件夹名称；会校验目标节点存在、名称合法以及同级不重名。

- `Map<String, Object> deleteFile(Long id, LoginUser loginUser)`
> 删除当前用户的文件或文件夹；文件夹仅允许在为空时删除，普通文件删除时会同步删除磁盘内容并回收已使用容量。

- `Map<String, Object> getStorageSummary(LoginUser loginUser)`
> 返回当前登录用户的已用容量、总容量与剩余容量摘要。

- `Map<String, Object> listFilesByOwnerForSuperAdmin(Long ownerId, Long parentId, LoginUser loginUser)`
> 仅允许超级管理员按指定用户和目录查询文件列表，用于后台查看其他用户存储内容。

- `Map<String, Object> searchFilesByOwnerForSuperAdmin(Long ownerId, String keyword, LoginUser loginUser)`
> 仅允许超级管理员搜索指定用户名下的文件或文件夹。

- `ResponseEntity<Resource> downloadFile(Long id, LoginUser loginUser)`
> 允许当前登录用户下载自己的文件；若文件不存在、无权访问或不是普通文件，则返回对应 HTTP 错误响应。

- `StorageFile getActiveFileForLink(Long id)`
> 返回一个仍可被公开分享下载的文件节点；会检查文件未删除、类型为普通文件、`storageKey` 合法且真实磁盘文件存在。

- `ResponseEntity<Resource> downloadSharedFile(Long id)`
> 基于分享模块传入的文件 ID 执行公开下载，内部复用统一的下载响应构造逻辑。

- `Map<String, Object> buildStorageFileSummary(StorageFile storageFile)`
> 将文件节点对象转换为列表展示或接口响应使用的摘要结构。
