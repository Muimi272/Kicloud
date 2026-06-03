# StorageController 类说明

代码位置：`src/main/java/club/muimi/kicloud/controller/StorageController.java`

---

## 1. 控制器含义

`StorageController` 是文件存储模块的主控制器，负责暴露 `/storage/**` 路由，包括：

- 单文件上传
- 分片上传会话创建
- 单个切片上传
- 创建文件夹
- 文件列表与搜索
- 重命名与删除
- 存储空间摘要
- 文件下载

---

## 2. 当前接口说明

- `POST /storage/upload`
> 接收完整文件和可选的 `parentId`，并调用 `StorageFileService.uploadFile(...)` 处理单文件上传。

- `POST /storage/uploadSlices`
> 接收 `UploadSlicesRequest`，创建一次分片上传会话，并返回后续上传切片所需的 `uploadId`。

- `POST /storage/uploadSlice`
> 接收 `UploadSliceRequest`，上传单个切片内容；全部切片完成后由服务层自动合并和入库。

- `POST /storage/folder`
> 接收 `CreateFolderRequest`，为当前用户创建文件夹节点。

- `GET /storage/list`
> 按 `parentId` 查询当前目录文件列表；未传时表示根目录。

- `GET /storage/search`
> 按关键字搜索当前登录用户自己的文件 / 文件夹。

- `POST /storage/rename`
> 接收 `RenameStorageFileRequest`，修改当前用户文件或文件夹名称。

- `POST /storage/delete`
> 接收 `DeleteStorageFileRequest`，删除当前用户文件或空文件夹。

- `GET /storage/summary`
> 返回当前登录用户的存储空间摘要。

- `GET /storage/download`
> 按文件 ID 下载当前登录用户自己的文件。

---

## 3. 当前实现特点

### 3.1 统一从 Session 读取登录用户

`StorageController` 内部通过：

- `getLoginUser(HttpSession session)`

统一从 Session 中读取 `LoginUser`，再传给 `StorageFileService`。

这意味着该控制器本身不直接访问数据库用户对象。

### 3.2 请求体为空时统一返回缺失信息

以下接口在请求体为 `null` 时都会返回统一错误：

- `uploadSlices(...)`
- `uploadSlice(...)`
- `createFolder(...)`
- `rename(...)`
- `delete(...)`

统一返回结构为：

```json
{ "code": 0, "msg": "Missing Information" }
```

### 3.3 分片上传入口已正式接入

当前 `StorageController` 已支持：

1. `POST /storage/uploadSlices`
2. `POST /storage/uploadSlice`

这两条路由用于配合前端大文件分片上传流程。

---

## 4. 辅助方法

- `getLoginUser(HttpSession session)`
> 从 Session 的 `LoginUser` 属性中提取当前登录用户；若 Session 不存在或类型不匹配则返回 `null`。

- `missingInformation()`
> 构造统一的缺少请求信息错误返回结构。
