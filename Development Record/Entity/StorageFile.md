# StorageFile 实体类说明

代码位置：`src/main/java/club/muimi/kicloud/entity/StorageFile.java`

---

## 1. 实体含义

`StorageFile` 实体用于统一表示系统中的文件节点，包括：

- 普通文件
- 文件夹

当前项目不拆分文件实体与文件夹实体，而是通过 `fileType` 字段统一管理节点类型。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 文件记录主键，数据库自增生成 |
| `name` | `String` | 文件或文件夹名称 |
| `parentId` | `Long` | 父目录 ID，可为空；为空表示位于根层 |
| `storageKey` | `String` | 文件真实存储标识，可为空 |
| `fileType` | `FileType` | 节点类型，用于区分普通文件和文件夹 |
| `ownerId` | `Long` | 所属用户 ID |
| `size` | `Long` | 文件大小，可为空 |
| `deleted` | `boolean` | 是否已软删除，默认值为 `false` |
| `createdAt` | `LocalDateTime` | 创建时间 |
| `updatedAt` | `LocalDateTime` | 更新时间 |
| `deletedAt` | `LocalDateTime` | 删除时间，未删除时为空 |
