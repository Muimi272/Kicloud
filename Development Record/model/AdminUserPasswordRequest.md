# AdminUserPasswordRequest 模型类说明

代码位置：`src/main/java/club/muimi/kicloud/model/AdminUserPasswordRequest.java`

---

## 1. 模型含义

`AdminUserPasswordRequest` 用于承接管理员重置或修改用户密码操作的数据，是 `/admin/user/password` 接口的请求体模型。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `userId` | `Long` | 需要被修改密码的目标用户 ID |
| `password` | `String` | 新的明文密码，服务端会再转换为密码哈希保存 |
