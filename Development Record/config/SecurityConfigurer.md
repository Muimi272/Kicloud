# SecurityConfigurer 类说明

代码位置：`src/main/java/club/muimi/kicloud/config/SecurityConfigurer.java`

---

## 1. 配置类含义

`SecurityConfigurer` 是当前系统的 Spring Security 主配置类，负责：

- 定义密码编码器
- 定义路由访问规则
- 关闭默认表单登录和 HTTP Basic
- 定义未登录与无权限时的跳转行为

---

## 2. 当前 Bean 说明

- `PasswordEncoder passwordEncoder()`
> 返回 `BCryptPasswordEncoder`，供注册、登录和密码重置场景复用。

- `SecurityFilterChain securityFilterChain(HttpSecurity http)`
> 定义当前系统的 Web 安全链路，包括放行路由、管理员路由限制、登录入口和无权限页面。

- `UserDetailsService userDetailsService()`
> 返回一个总是抛出 `UsernameNotFoundException` 的占位实现；因为当前系统并不使用 Spring Security 默认表单登录流程。

---

## 3. 当前安全规则

### 3.1 已关闭的默认能力

当前显式关闭了：

- CSRF
- Spring Security 默认表单登录
- HTTP Basic

这意味着当前项目使用的是自定义登录接口和 Session 登录态写入逻辑。

### 3.2 放行的路由

当前直接 `permitAll()` 的路径包括：

- `/user/login`
- `/user/logout`
- `/user/register`
- `/css/**`
- `/js/**`
- `/images/**`
- `/webjars/**`
- `/error`
- `/forbidden`
- `/link/**`

### 3.3 受限的路由

- `/admin/**`
> 必须具备 `ADMIN` 或 `SUPERADMIN` 角色。

- 其他所有路径
> 必须先通过认证。

### 3.4 异常处理

当前约定：

- 未登录访问受保护资源时，跳转到 `/user/login`
- 已登录但无权访问时，跳转到 `/forbidden`

---

## 4. 当前实现特点

### 4.1 Session 与 Spring Security 双轨协同

虽然 Spring Security 负责路由层保护，但当前系统的登录动作并不是由其默认登录表单完成，而是由 `UserController + UserService` 手动写入：

- Session 中的 `LoginUser`
- Spring Security 上下文

### 4.2 `/link/**` 整体放行

分享模块之所以整体放行，是因为其中既有公开页面，也有公开下载行为；更细粒度的“密码校验、文件是否存在、分享是否有效”等逻辑由 `LinkService` 再判断。
