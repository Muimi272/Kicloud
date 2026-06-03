# User 实体类说明

代码位置：`src/main/java/club/muimi/kicloud/entity/User.java`

---

## 1. 实体含义

`User` 实体用于表示系统中的登录用户，是认证、授权和容量控制的核心数据模型。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 用户主键，数据库自增生成 |
| `username` | `String` | 用户名，唯一且不能为空，作为主要登录标识 |
| `passwordHash` | `String` | 用户密码的哈希值 |
| `enabled` | `boolean` | 账号是否启用，默认值为 `true` |
| `totalSpace` | `Long` | 用户总容量限制，单位为B，默认值为 `1GB` |
| `usedSpace` | `Long` | 用户已使用容量，单位为B，默认值为 `0` |
| `role` | `Role` | 用户角色，默认值为 `Role.USER` |
| `createdAt` | `LocalDateTime` | 用户创建时间 |

