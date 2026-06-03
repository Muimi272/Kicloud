# AdminUserIdRequest 模型类说明

代码位置：`src/main/java/club/muimi/kicloud/model/AdminUserIdRequest.java`

---

## 1. 模型含义

`AdminUserIdRequest` 是管理员侧最基础的用户定位请求模型，用于只需要提供目标用户 ID 的后台操作，例如封禁用户。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `userId` | `Long` | 目标用户 ID |
