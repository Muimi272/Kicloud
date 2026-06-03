# controller

本目录用于存放与 `src/main/java/club/muimi/kicloud/controller` 对应的控制器层文档。

---

## `controller` 层职责

当前项目中的 Controller 主要负责：

- 暴露页面路由和 JSON 接口
- 接收请求参数、请求体和路径参数
- 从 `HttpSession` 中提取当前登录用户信息
- 对请求对象是否为空做轻量校验
- 把业务处理委托给对应的 Service

当前 Controller 基本不承载复杂业务规则，真正的权限和状态校验主要由：

- Spring Security
- MVC Interceptor
- 对应 Service

共同完成。

---

## 当前文档索引

- [UserController](./UserController.md)
- [StorageController](./StorageController.md)
- [LinkController](./LinkController.md)
- [AdminController](./AdminController.md)
- [PageController](./PageController.md)
