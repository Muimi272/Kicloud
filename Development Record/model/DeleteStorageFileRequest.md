# DeleteStorageFileRequest 模型类说明

代码位置：`src/main/java/club/muimi/kicloud/model/DeleteStorageFileRequest.java`

---

## 1. 模型含义

`DeleteStorageFileRequest` 用于承接删除文件或文件夹操作的数据，是 `/storage/delete` 接口的请求体模型。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 需要删除的文件或文件夹 ID |
