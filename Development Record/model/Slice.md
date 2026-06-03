# Slice 模型类说明

代码位置：`src/main/java/club/muimi/kicloud/model/Slice.java`

---

## 1. 模型含义

`Slice` 用于表示一次分片上传任务中的单个切片状态，是 `Slices` 会话对象内部维护的最小切片单元。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `size` | `int` | 当前切片的大小，单位为字节 |
| `path` | `String` | 当前切片在临时目录中的物理文件路径 |
| `done` | `boolean` | 当前切片是否已经成功落盘并记入会话 |

---

## 3. 当前使用方式

当前 `Slice` 主要由 `StorageFileService.uploadSlice(...)` 在接收单个切片时更新：

1. 前端把切片内容上传到 `/storage/uploadSlice`
2. 服务端先把切片写入 `./data/temp`
3. 再把切片大小、切片路径和完成状态写回对应的 `Slice`

`Slice` 本身不负责校验顺序、过期时间或最终合并，这些都由 `Slices` 和 `StorageFileService` 负责。
