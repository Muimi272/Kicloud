# Slices 模型类说明

代码位置：`src/main/java/club/muimi/kicloud/model/Slices.java`

---

## 1. 模型含义

`Slices` 用于表示一个完整文件的分片上传会话，负责聚合该文件全部切片的状态、所属用户、完整文件 MD5、上传目录和超时信息。

它不是数据库实体，而是当前运行中的内存态会话对象。

---

## 2. 关键字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `slicesMap` | `Map<Integer, Slice>` | 以切片编号为键保存切片状态 |
| `count` | `int` | 当前会话应包含的总切片数 |
| `fullFileMD5` | `String` | 完整文件的 MD5，用于合并后校验 |
| `ownerId` | `Long` | 当前上传任务所属用户 ID |
| `uploadId` | `String` | 后端生成的独立上传会话标识，用于区分相同 MD5 的并发上传 |
| `fileName` | `String` | 最终保存到存储系统中的原始文件名 |
| `parentId` | `Long` | 目标父目录 ID，为空时表示根目录 |
| `totalSize` | `long` | 完整文件总大小 |
| `createdAt` | `LocalDateTime` | 会话创建时间 |
| `updatedAt` | `LocalDateTime` | 最近一次成功接收到切片的时间 |

---

## 3. 主要方法说明

- `addSlice(int sliceNum, int size, String path)`
> 把指定编号的切片标记为完成，并更新切片大小、临时文件路径和 `updatedAt`。

- `isAllDone()`
> 判断当前会话中的全部切片是否都已经上传完成。

- `containsSlice(int sliceNum)`
> 判断某个切片编号是否在本次会话的合法范围内。

- `getSlice(int sliceNum)`
> 返回指定编号的切片状态对象。

- `isExpired(LocalDateTime now)`
> 按“创建 20 分钟后”或“最近更新 2 分钟后”判断当前上传会话是否已经过期。

- `getSlices()`
> 按切片编号顺序返回全部切片，便于服务端合并。

---

## 4. 当前使用方式

`Slices` 当前由 `StorageFileService.uploadSlices(...)` 创建，并存入服务层的 `activeSlices` 内存映射中。

会话生命周期大致如下：

1. 前端先请求 `/storage/uploadSlices`
2. 服务端创建 `Slices` 对象并生成 `uploadId`
3. 前端继续调用 `/storage/uploadSlice` 上传每个切片
4. 全部切片完成后，服务端按切片顺序合并、校验 MD5、保存完整文件
5. 上传成功或失败后，服务端都会删除临时切片文件并移除该会话

当前会话数据只保存在内存中，因此应用重启后未完成的分片上传任务不会被恢复。
