# config

本目录用于存放与 `src/main/java/club/muimi/kicloud/config` 及其 `interceptor` 子包对应的配置层文档。

---

## `config` 层职责

当前 `config` 目录主要负责：

- Spring Security 配置
- MVC 拦截器注册
- 登录拦截规则
- 管理员权限拦截规则
- 全局密码编码器等基础 Bean 定义

---

## 当前文档索引

- [SecurityConfigurer](./SecurityConfigurer.md)
- [WebConfigurer](./WebConfigurer.md)
- [LoginInterceptor](./LoginInterceptor.md)
- [AdminInterceptor](./AdminInterceptor.md)
