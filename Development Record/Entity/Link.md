# Link 实体类说明

代码位置：`src/main/java/club/muimi/kicloud/entity/Link.java`

---

## 1. 实体含义

`Link` 实体用于表示文件分享链接记录，是公开分享下载能力的核心数据模型。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 主键 |
| `linkId` | `String` | 对外分享标识，唯一，长度为 12 |
| `storageFileId` | `Long` | 被分享文件的 `StorageFile` ID |
| `password` | `String` | 分享密码哈希值，可为空 |
| `ownerId` | `Long` | 分享所属用户 ID |
| `ownerName` | `String` | 分享所属用户名 |
| `downloadTimes` | `Long` | 下载次数 |
| `deleted` | `boolean` | 是否已删除 |
| `createdAt` | `LocalDateTime` | 创建时间 |
| `deletedAt` | `LocalDateTime` | 删除时间 |
