# AdminUserRoleRequest 模型类说明

代码位置：`src/main/java/club/muimi/kicloud/model/AdminUserRoleRequest.java`

---

## 1. 模型含义

`AdminUserRoleRequest` 用于承接管理员调整用户角色的数据，是 `/admin/user/role` 接口的请求体模型。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `userId` | `Long` | 需要调整角色的目标用户 ID |
| `role` | `Role` | 目标角色值，例如 `USER`、`ADMIN`、`SUPERADMIN` |
