# DeleteLinkRequest 模型类说明

代码位置：`src/main/java/club/muimi/kicloud/model/DeleteLinkRequest.java`

---

## 1. 模型含义

`DeleteLinkRequest` 用于承接删除分享链接操作的数据，既被普通用户的 `/link/delete` 接口使用，也被管理员的 `/admin/link/delete` 接口复用。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 需要删除的分享链接记录 ID |
