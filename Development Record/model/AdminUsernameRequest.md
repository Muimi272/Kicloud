# AdminUsernameRequest 模型类说明

代码位置：`src/main/java/club/muimi/kicloud/model/AdminUsernameRequest.java`

---

## 1. 模型含义

`AdminUsernameRequest` 用于承接管理员修改用户名操作的数据，是 `/admin/user/username` 接口的请求体模型。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `userId` | `Long` | 需要被修改的目标用户 ID |
| `username` | `String` | 修改后的用户名 |
