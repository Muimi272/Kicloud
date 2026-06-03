# RenameStorageFileRequest 模型类说明

代码位置：`src/main/java/club/muimi/kicloud/model/RenameStorageFileRequest.java`

---

## 1. 模型含义

`RenameStorageFileRequest` 用于承接文件或文件夹重命名操作的数据，是 `/storage/rename` 接口的请求体模型。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 需要重命名的文件或文件夹 ID |
| `newName` | `String` | 重命名后的新名称 |
