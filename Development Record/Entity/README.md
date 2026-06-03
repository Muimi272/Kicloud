# 01-entity

本目录存放 `src/main/java/club/muimi/kicloud/entity` 对应的实体文档。

---

## 当前文档索引

- [User](./User.md)
- [StorageFile](./StorageFile.md)
- [Invitation](./Invitation.md)
- [Link](./Link.md)

---

## JPA使用说明

本项目的所有实体类都加有 `@Entity` 注释，JPA将自动根据实体类的字段创建数据库。

由于在 `src/main/resources/application.properties` 中配置了 `spring.jpa.hibernate.ddl-auto=none=none`，因此在无数据库部署时应当先显式覆盖数据库初始化策略为`update`，来要求应用自动创建数据库。（在创建数据库前，请检查 `./data` 目录存在，否则应用将无法创建数据库）

由于 `Spring JDBC` 和 `SQLite` 的适配问题，`SQLite` 中的 64 位整数返回的类型为 `Integer` ，而 `Spring JDBC` 期待的数据类型为 `BIGINT`，因此如果使用 `validate` 对自动生成的数据库进行检验，将不通过。