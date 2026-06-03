# 04-service

本目录用于存放与 `src/main/java/club/muimi/kicloud/service` 对应的业务服务层文档。

---

## `service`业务层说明

Service 层主要承接：

- 业务规则
- 状态校验
- 事务与组合逻辑
- 对 DAO、实体、权限链路的编排

当前 `service` 类主要以 Spring `@Service` 组件形式存在，由 Controller 调用，并在需要时参与认证、权限或跨模块规则校验。

---

## 当前文档索引

- [UserService](./UserService.md)
- [StorageFileService](./StorageFileService.md)
- [LinkService](./LinkService.md)
- [AdminService](./AdminService.md)
- [存储模块说明](./存储模块说明.md)
