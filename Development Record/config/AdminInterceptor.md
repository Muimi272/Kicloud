# AdminInterceptor 类说明

代码位置：`src/main/java/club/muimi/kicloud/config/interceptor/AdminInterceptor.java`

---

## 1. 拦截器含义

`AdminInterceptor` 是后台管理模块的权限拦截器，用于在访问 `/admin/**` 路由前进一步确认当前用户是否仍然具备管理员权限。

---

## 2. 当前拦截逻辑

`preHandle(...)` 当前流程如下：

1. 获取当前 Session
2. 若 Session 不存在或缺少 `LoginUser`：
   - 清理登录态
   - 重定向到 `/user/login`
   - 返回 `false`
3. 调用 `userService.isLoginUserIllegal(loginUser)` 校验登录态是否合法
4. 若登录态非法：
   - 清理登录态
   - 重定向到 `/user/login?reason=session-expired`
   - 返回 `false`
5. 调用 `userService.getUserByLoginUser(loginUser)` 获取数据库中的真实用户
6. 若用户不存在，或角色不是 `ADMIN` / `SUPERADMIN`：
   - 清理登录态
   - 重定向到 `/forbidden`
   - 返回 `false`
7. 以上都通过后放行

---

## 3. 当前实现特点

### 3.1 与 Spring Security 的后台鉴权形成双层保护

当前后台管理路由除了会经过 Spring Security 的：

- `/admin/**`.hasAnyRole("ADMIN", "SUPERADMIN")

之外，还会再经过 `AdminInterceptor`。

这样可以在 Session 与数据库用户状态不一致时，及时拦截并清理登录态。

### 3.2 无管理员权限时跳到 `/forbidden`

与普通未登录跳回登录页不同，当前如果用户已经登录但不具备管理员权限，会被重定向到：

- `/forbidden`

用于展示无权限页面。
