# LinkService类说明

代码位置：`src/main/java/club/muimi/kicloud/service/LinkService.java`

---

## 业务含义

`LinkService` 用于处理文件分享链接的创建、查询、删除、公开访问校验与下载，是分享模块的核心业务服务。

## 公开方法说明

- `Map<String, Object> createLink(Long storageFileId, String password, LoginUser loginUser)`
> 为指定文件创建分享链接；会校验当前登录用户身份、目标文件是否可分享，并在需要时保存分享密码哈希。

- `Map<String, Object> getMyLinks(LoginUser loginUser)`
> 查询当前登录用户创建的全部未删除分享链接，并按创建时间倒序返回。

- `Map<String, Object> getAllLinksForSuperAdmin(LoginUser loginUser)`
> 仅允许超级管理员查看系统内全部未删除分享链接。

- `Map<String, Object> getPublicLinkDetail(String linkId)`
> 面向公开分享页，返回指定分享链接及其对应文件的展示信息；当链接或文件失效时返回错误结果。

- `Map<String, Object> getPublicLinkView(String linkId)`
> 返回公开分享页所需的轻量数据；若链接不可访问则直接返回 `null`。

- `String validateDownloadRequest(String linkId, String password)`
> 校验公开下载请求是否合法；若存在错误则返回可直接展示的中文提示信息，校验通过时返回 `null`。

- `Map<String, Object> deleteOwnLink(Long id, LoginUser loginUser)`
> 允许普通用户删除自己创建的分享链接，本质上执行逻辑删除。

- `Map<String, Object> deleteLinkForSuperAdmin(Long id, LoginUser loginUser)`
> 允许超级管理员删除任意分享链接，本质上执行逻辑删除。

- `ResponseEntity<Resource> downloadByLinkId(String linkId, String password)`
> 按分享标识发起公开下载；会校验密码、检查目标文件是否仍存在，并在成功下载前累加下载次数。

- `Map<String, Object> buildLinkSummary(Link link)`
> 将分享链接对象转换为列表展示所需的摘要结构。

- `Map<String, Object> buildLinkDetail(Link link)`
> 在摘要结构基础上补充删除时间、公开访问路径与下载路径，生成更完整的响应数据。
