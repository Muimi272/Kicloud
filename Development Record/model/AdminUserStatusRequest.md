# AdminUserStatusRequest 模型类说明

代码位置：`src/main/java/club/muimi/kicloud/model/AdminUserStatusRequest.java`

---

## 1. 模型含义

`AdminUserStatusRequest` 用于承接管理员启用或停用用户账号的数据，是 `/admin/user/status` 接口的请求体模型。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `userId` | `Long` | 需要修改状态的目标用户 ID |
| `enabled` | `Boolean` | 账号是否启用；`true` 表示启用，`false` 表示停用 |
