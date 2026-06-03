# CreateLinkRequest 模型类说明

代码位置：`src/main/java/club/muimi/kicloud/model/CreateLinkRequest.java`

---

## 1. 模型含义

`CreateLinkRequest` 用于承接分享链接创建操作的数据，是 `/link/create` 接口的请求体模型。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `storageFileId` | `Long` | 需要创建分享链接的文件 ID |
| `password` | `String` | 分享提取密码；为空时表示创建无密码分享 |
