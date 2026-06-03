# LoginInterceptor 类说明

代码位置：`src/main/java/club/muimi/kicloud/config/interceptor/LoginInterceptor.java`

---

## 1. 拦截器含义

`LoginInterceptor` 是当前系统的登录态拦截器，用于在进入大多数受保护页面和接口前校验：

- Session 是否存在
- Session 中是否有 `LoginUser`
- `LoginUser` 是否仍然合法

---

## 2. 当前拦截逻辑

`preHandle(...)` 当前流程如下：

1. 调用 `request.getSession(false)` 获取现有 Session
2. 若 Session 不存在或 `LoginUser` 缺失：
   - 调用 `userService.clearLoginState(session)`
   - 重定向到 `/user/login`
   - 返回 `false`
3. 若存在 `LoginUser`，则调用 `userService.isLoginUserIllegal(loginUser)` 做进一步校验
4. 如果登录态非法：
   - 清理登录态
   - 重定向到 `/user/login?reason=session-expired`
   - 返回 `false`
5. 校验通过则放行

---

## 3. 当前实现特点

### 3.1 不只看“有没有 Session”

当前并不是只要 Session 里有对象就放行，而是会进一步校验：

- 用户是否仍然存在
- 用户名和角色是否与当前 Session 一致
- 账号是否仍然启用

### 3.2 登录态非法时会主动清理

当发现 Session 失效或用户状态变化时，当前拦截器会调用：

- `userService.clearLoginState(session)`

避免残留的 Session 或 Spring Security 上下文继续被复用。
