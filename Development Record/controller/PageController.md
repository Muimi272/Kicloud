# PageController 类说明

代码位置：`src/main/java/club/muimi/kicloud/controller/PageController.java`

---

## 1. 控制器含义

`PageController` 负责系统中的基础页面跳转和模板入口，是一个典型的“页面路由控制器”。

与其他 JSON API 控制器不同，它主要返回模板名或重定向字符串。

---

## 2. 当前页面路由说明

- `GET /user/login`
> 返回登录页模板 `login`。

- `GET /dashboard`
> 返回用户控制台模板 `dashboard`。

- `GET /share`
> 返回分享管理页模板 `share`。

- `GET /share/file/{linkId}`
> 兼容旧式分享文件入口；会把请求重定向到正式的 `/link/{linkId}` 分享页面，并透传可选密码参数。

- `GET /user/register`
> 返回注册页模板 `register`。

- `GET /admin/back`
> 返回后台页模板 `back`。

- `GET /forbidden`
> 返回无权限提示页模板 `forbidden`。

---

## 3. 当前实现特点

### 3.1 主要服务于 Thymeleaf 页面

`PageController` 不直接返回 JSON，而是负责把 URL 映射到对应模板。

### 3.2 保留了旧分享入口兼容跳转

`/share/file/{linkId}` 当前不是正式分享页面，而是一个兼容入口。

它的作用是：

1. 接收旧路径访问
2. 用 `UriComponentsBuilder` 拼装新路径
3. 重定向到 `/link/{linkId}`
4. 如果请求里带了 `password`，则一并透传

这避免旧链接失效。

### 3.3 权限是否要求登录不在本类判断

当前页面本身是否要求登录，主要由：

- Spring Security
- MVC Interceptor

决定，`PageController` 本身不做显式权限判断。
