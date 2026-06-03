# CreateFolderRequest 模型类说明

代码位置：`src/main/java/club/muimi/kicloud/model/CreateFolderRequest.java`

---

## 1. 模型含义

`CreateFolderRequest` 用于承接创建文件夹操作的数据，是 `/storage/folder` 接口的请求体模型。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `name` | `String` | 要创建的文件夹名称 |
| `parentId` | `Long` | 父级目录 ID；为空时通常表示在根目录下创建 |
