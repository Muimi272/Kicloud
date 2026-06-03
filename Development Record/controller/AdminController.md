# AdminController 类说明

代码位置：`src/main/java/club/muimi/kicloud/controller/AdminController.java`

---

## 1. 控制器含义

`AdminController` 是管理后台的主控制器，负责邀请码管理、用户管理、跨用户文件查看和全站分享链接管理。

它统一挂载在 `/admin/**` 路由下，并通过类级别：

```java
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
```

要求访问者至少具备 `ADMIN` 角色。

---

## 2. 当前接口说明

### 2.1 邀请码相关

- `POST /admin/invite`
> 创建邀请码。

- `GET /admin/invitations`
> 查询全部邀请码。

- `GET /admin/invitation/{invitationId}`
> 查询单个邀请码详情。

- `POST /admin/invitation/status`
> 修改邀请码可用状态。

### 2.2 用户管理相关

- `GET /admin/users`
> 查询全部用户列表。

- `GET /admin/user/{userId}`
> 查询单个用户详情。

- `POST /admin/user/status`
> 修改用户启用状态。

- `POST /admin/user/ban`
> 快速封禁用户，本质上等价于把用户状态改为 `false`。

- `POST /admin/user/space`
> 修改用户总空间额度。

- `POST /admin/user/password`
> 重置指定用户密码。

- `POST /admin/user/username`
> 修改指定用户名。

- `POST /admin/user/role`
> 修改指定用户角色。

### 2.3 跨用户文件查看

- `GET /admin/user/{userId}/files`
> 查询指定用户某个目录下的文件列表。

- `GET /admin/user/{userId}/files/search`
> 搜索指定用户名下的文件。

### 2.4 分享链接管理

- `GET /admin/links`
> 查询系统中的分享链接列表。

- `POST /admin/link/delete`
> 删除指定分享链接。

---

## 3. 当前实现特点

### 3.1 Controller 侧只有轻量判空

`AdminController` 当前只对请求体是否为空做基础判断；真正的角色边界、目标用户合法性、是否只能由超级管理员执行等规则，都由 `AdminService` 继续校验。

### 3.2 类级别权限与拦截器双重生效

访问 `/admin/**` 时，当前系统同时依赖：

- Spring Security 路由级 `hasAnyRole("ADMIN", "SUPERADMIN")`
- `@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")`
- `AdminInterceptor`

形成多层权限保护。

### 3.3 部分后台能力实际只允许超级管理员

虽然 `AdminController` 类级别允许 `ADMIN` 和 `SUPERADMIN` 进入，但具体到某些业务时，例如：

- 查看其他用户文件
- 查看全站链接
- 修改用户角色

真正是否放行还要看 `AdminService` 内部规则。

---

## 4. 辅助方法

- `missingInformation()`
> 统一返回 `code = 0`、`msg = "Missing Information"` 的缺参错误结构。
