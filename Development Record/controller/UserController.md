# UserController 类说明

代码位置：`src/main/java/club/muimi/kicloud/controller/UserController.java`

---

## 1. 控制器含义

`UserController` 是用户认证模块的控制器入口，负责处理登录、登出和注册请求。

它的特点是：

- 路由统一挂在 `/user/**`
- 返回 JSON 结构
- 对登录和注册请求做基础参数判空
- 将真正的认证、注册和登录态维护交给 `UserService`

---

## 2. 当前接口说明

- `POST /user/login`
> 接收 `LoginRequest`；支持通过用户 ID 或用户名登录，并要求密码非空。若请求体缺失或关键信息不足，则直接返回统一的登录失败结果。

- `POST /user/logout`
> 基于当前 `HttpSession` 执行登出；内部委托 `UserService.logout(...)` 清理 Session 和 Spring Security 登录态。

- `POST /user/register`
> 接收 `RegisterRequest`；会校验用户名和密码非空，并把注册逻辑交给 `UserService.register(...)`。

---

## 3. 当前实现特点

### 3.1 登录支持两种标识

`login(...)` 当前允许：

1. 传 `id + password`
2. 传 `username + password`

如果两者都不满足，则直接返回：

```json
{ "code": 0, "msg": "Login Failed" }
```

### 3.2 Controller 层只做轻量校验

当前在 `UserController` 中只做以下基础判断：

- 请求体是否为空
- 密码是否为空
- 用户名是否为空

真正的用户存在性、密码校验、邀请码校验和登录态写入都由 `UserService` 完成。

### 3.3 登出结果有明确语义

`logout(...)` 会根据 `UserService.logout(session)` 的返回值区分：

- 已成功登出：`Logout Successful`
- 本来就没登录：`Not Logged In`

---

## 4. 辅助方法

- `loginFailedResponse()`
> 构造统一的登录失败返回结构，当前固定为 `code = 0` 和 `msg = "Login Failed"`。
