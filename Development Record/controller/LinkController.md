# LinkController 类说明

代码位置：`src/main/java/club/muimi/kicloud/controller/LinkController.java`

---

## 1. 控制器含义

`LinkController` 是分享模块的控制器入口，既负责登录用户自己的分享管理接口，也负责公开分享页面和公开下载入口。

它统一挂载在 `/link/**` 路由下。

---

## 2. 当前接口说明

- `POST /link/create`
> 接收 `CreateLinkRequest`，为当前用户指定文件创建分享链接。

- `GET /link/my`
> 返回当前登录用户自己创建的分享链接列表。

- `POST /link/delete`
> 接收 `DeleteLinkRequest`，删除当前登录用户自己的分享链接。

- `GET /link/{linkId}`
> 打开正式分享页面；会把分享链接可用性、预填密码和下载错误信息写入模板模型，最终渲染 `share-file` 页面。

- `GET /link/{linkId}/detail`
> 返回公开分享页所需的 JSON 详情数据。

- `POST /link/{linkId}/download`
> 处理表单提交式下载请求；若密码或链接状态不合法，则重定向回分享页并带上错误信息。

- `GET /link/{linkId}/download`
> 处理查询参数式下载请求；内部逻辑与 `POST` 版本一致。

---

## 3. 当前实现特点

### 3.1 同时存在“用户接口”和“公开入口”

`LinkController` 既服务于已登录用户，也服务于匿名访问者：

- `/link/create`、`/link/my`、`/link/delete` 面向登录用户
- `/link/{linkId}`、`/link/{linkId}/detail`、`/link/{linkId}/download` 面向公开访问

这也是为什么 Spring Security 中对 `/link/**` 整体放行，而具体业务由 `LinkService` 再做细化校验。

### 3.2 下载前先执行统一校验

`POST /{linkId}/download` 和 `GET /{linkId}/download` 都会先调用：

- `linkService.validateDownloadRequest(linkId, password)`

如果返回错误消息，就通过 `redirectWithError(...)` 重定向回分享页，而不是直接开始下载。

### 3.3 分享页面依赖模板模型

`openSharePage(...)` 当前会向模板写入：

- `linkId`
- `presetPassword`
- `downloadError`
- `linkAvailable`
- `linkData`

由此驱动前端公开分享页的展示。

---

## 4. 辅助方法

- `redirectWithError(String linkId, String password, String message)`
> 把错误消息和可选密码编码进查询参数后，重定向回正式分享页。

- `getLoginUser(HttpSession session)`
> 从 Session 中提取 `LoginUser`，供登录态相关接口使用。

- `missingInformation()`
> 构造统一的缺少请求信息错误返回结构。
