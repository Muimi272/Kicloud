# AdminUserSpaceRequest 模型类说明

代码位置：`src/main/java/club/muimi/kicloud/model/AdminUserSpaceRequest.java`

---

## 1. 模型含义

`AdminUserSpaceRequest` 用于承接管理员调整用户容量配额的数据，是 `/admin/user/space` 接口的请求体模型。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `userId` | `Long` | 需要调整容量的目标用户 ID |
| `totalSpace` | `Long` | 调整后的总容量上限，单位为字节 |
