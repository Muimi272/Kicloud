# UserService类说明

代码位置：`src/main/java/club/muimi/kicloud/service/UserService.java`

---

## 业务含义

`UserService` 用于处理用户登录、注册、登录态同步、Session 清理与当前登录用户合法性校验，是当前认证链路中的核心业务服务。

## 公开方法说明

- `Map<String, Object> login(Long id, String password, HttpSession session)`
> 通过用户 ID 和密码执行登录校验；成功时写入 `LoginUser` 与 Spring Security 上下文，失败时返回统一的登录失败结果。

- `Map<String, Object> login(String username, String password, HttpSession session)`
> 通过用户名和密码执行登录校验；成功后的登录态写入逻辑与按 ID 登录一致。

- `boolean isLoginUserIllegal(LoginUser loginUser)`
> 校验 Session 中的 `LoginUser` 是否仍然可信；会检查登录对象完整性、数据库用户是否存在、用户是否启用，以及用户名和角色是否与数据库一致。

- `User getUserByLoginUser(LoginUser loginUser)`
> 在 `LoginUser` 合法时返回数据库中的真实用户对象；若登录态不合法，则返回 `null`。

- `boolean logout(HttpSession session)`
> 清理当前 Session 中的登录态与 Spring Security 上下文；返回值表示清理前是否存在可识别的登录信息。

- `void clearLoginState(HttpSession session)`
> 清空 `LoginUser`、`SPRING_SECURITY_CONTEXT` 与 `SecurityContextHolder`，用于登出和会话失效场景。

- `Map<String, Object> register(String username, String password, String invitationCode)`
> 校验用户名、密码与邀请码后创建新用户，并把邀请码标记为已使用，同时记录使用时间与被邀请用户 ID。
