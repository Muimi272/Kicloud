# UploadSliceRequest 模型类说明

代码位置：`src/main/java/club/muimi/kicloud/model/UploadSliceRequest.java`

---

## 1. 模型含义

`UploadSliceRequest` 用于承接单个切片上传的数据，是 `/storage/uploadSlice` 接口的请求模型。

它配合 `UploadSlicesRequest` 使用，负责把某一次分片上传会话中的具体切片内容发送到后端。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `uploadId` | `String` | 后端在创建分片上传会话时返回的唯一上传标识 |
| `fullFileMD5` | `String` | 完整文件 MD5，用于确认当前切片属于哪个完整文件 |
| `sliceNum` | `Integer` | 当前切片编号，从 `0` 开始 |
| `size` | `Integer` | 当前切片大小，单位为字节 |
| `slice` | `MultipartFile` | 当前切片的二进制内容 |

---

## 3. 当前使用方式

前端会对完整文件切片后，按顺序循环调用 `/storage/uploadSlice`。

服务端接收到该请求后会：

1. 根据 `uploadId` 和当前登录用户定位具体分片会话
2. 校验 `fullFileMD5`、切片编号和切片大小
3. 将切片暂存到 `./data/temp`
4. 更新会话中的对应 `Slice`
5. 当全部切片完成时触发合并、MD5 校验与最终入库
