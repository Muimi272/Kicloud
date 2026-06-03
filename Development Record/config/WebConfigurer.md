# WebConfigurer 类说明

代码位置：`src/main/java/club/muimi/kicloud/config/WebConfigurer.java`

---

## 1. 配置类含义

`WebConfigurer` 是当前系统的 Spring MVC 配置类，主要负责注册自定义拦截器。

它实现了 `WebMvcConfigurer`，并在 `addInterceptors(...)` 中声明：

- 哪些路径要执行登录校验
- 哪些路径要执行管理员权限校验

---

## 2. 当前注册的拦截器

- `LoginInterceptor`
> 负责登录态和 Session 合法性校验。

- `AdminInterceptor`
> 负责管理员权限校验。

---

## 3. 当前路径规则

### 3.1 LoginInterceptor

当前注册方式：

- `addPathPatterns("/**")`
- 再排除以下路径：
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

这意味着除了上述公开路径之外，其余请求都会先经过登录态校验。

### 3.2 AdminInterceptor

当前注册方式：

- `addPathPatterns("/admin/**")`

因此所有后台管理路由都会额外经过管理员权限检查。

---

## 4. 当前实现特点

### 4.1 与 Spring Security 配置形成互补

`WebConfigurer` 中的拦截器注册不是替代 Spring Security，而是与其形成双层校验：

- Spring Security 负责路由级认证与授权
- MVC Interceptor 负责 Session 中 `LoginUser` 的有效性和业务态一致性校验

### 4.2 `/link/**` 被视为公开入口

由于分享页和公开下载入口都挂在 `/link/**` 下，所以登录拦截器对这一整段路径做了排除。
