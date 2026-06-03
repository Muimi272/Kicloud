# AdminService类说明

代码位置：`src/main/java/club/muimi/kicloud/service/AdminService.java`

---

## 业务含义

`AdminService` 用于处理管理后台中的用户管理、邀请码管理、分享链接管理与超级管理员文件查看能力。

当前类上声明了 `@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")`，因此它的公开方法默认要求调用方具备管理员或超级管理员身份。

## 公开方法说明

- `Invitation createInvitation(HttpSession session)`
> 校验当前后台操作者后生成新的邀请码记录，并保存邀请码生成者 ID。

- `Map<String, Object> getAllUsers(HttpSession session)`
> 查询系统内全部用户，并返回适合后台列表展示的摘要数据。

- `Map<String, Object> getUserDetail(HttpSession session, Long userId)`
> 查询指定用户的详细信息；当用户不存在时返回错误结果。

- `Map<String, Object> getAllInvitations(HttpSession session)`
> 查询系统内全部邀请码记录，并按统一结构返回后台列表数据。

- `Map<String, Object> getInvitationDetail(HttpSession session, Long invitationId)`
> 查询指定邀请码的详细信息；当邀请码不存在时返回错误结果。

- `Map<String, Object> changeInvitationStatus(HttpSession session, Long invitationId, Boolean valid)`
> 修改邀请码启用状态；普通管理员只能管理自己生成的邀请码，且已使用的邀请码不能重新启用。

- `Map<String, Object> changeUserStatus(HttpSession session, Long userId, Boolean enabled)`
> 修改用户启用状态；会校验操作者是否有权限管理目标用户，并阻止越权操作。

- `Map<String, Object> changeUserSpace(HttpSession session, Long userId, Long totalSpace)`
> 修改用户总容量限制；会校验新容量不能为负数，且不能小于当前已使用容量。

- `Map<String, Object> changeUserPassword(HttpSession session, Long userId, String password)`
> 为指定用户重置密码，并把新密码保存为哈希值。

- `Map<String, Object> changeUsername(HttpSession session, Long userId, String username)`
> 修改指定用户的用户名；会校验重名冲突，并在管理员修改自己用户名时同步更新 Session 中的 `LoginUser`。

- `Map<String, Object> changeUserRole(HttpSession session, Long userId, Role role)`
> 修改指定用户角色；会结合操作者身份、目标用户原角色与目标角色判断是否允许变更。

- `Map<String, Object> getUserFiles(HttpSession session, Long userId, Long parentId)`
> 仅允许超级管理员查看指定用户某一目录下的文件列表，内部委托给 `StorageFileService` 完成目录查询。

- `Map<String, Object> searchUserFiles(HttpSession session, Long userId, String keyword)`
> 仅允许超级管理员搜索指定用户名下的文件或文件夹，内部委托给 `StorageFileService` 完成搜索。

- `Map<String, Object> getAllLinks(HttpSession session)`
> 仅允许超级管理员查看系统内全部未删除分享链接。

- `Map<String, Object> deleteLink(HttpSession session, Long linkId)`
> 仅允许超级管理员删除指定分享链接，本质上执行逻辑删除。

- `Map<String, Object> buildUserSummary(User user)`
> 将用户对象转换为后台用户列表所需的摘要结构，并计算剩余可用容量。

- `Map<String, Object> buildUserDetail(User user)`
> 当前直接复用用户摘要结构作为详情结构返回。

- `Map<String, Object> buildInvitationSummary(Invitation invitation)`
> 将邀请码对象转换为后台列表或详情所需的基础结构。

- `Map<String, Object> buildInvitationDetail(Invitation invitation)`
> 当前直接复用邀请码摘要结构作为详情结构返回。

- `Map<String, Object> buildLinkSummary(club.muimi.kicloud.entity.Link link)`
> 将分享链接对象转换为后台展示所需的摘要结构，并包含删除时间等管理信息。
