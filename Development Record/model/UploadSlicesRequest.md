# UploadSlicesRequest 模型类说明

代码位置：`src/main/java/club/muimi/kicloud/model/UploadSlicesRequest.java`

---

## 1. 模型含义

`UploadSlicesRequest` 用于承接“开始一次分片上传任务”的请求体数据，是 `/storage/uploadSlices` 接口的请求模型。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `sliceCount` | `Integer` | 完整文件总切片数 |
| `fullFileMD5` | `String` | 完整文件 MD5，用于最终合并后的完整性校验 |
| `fileName` | `String` | 原始文件名，用于最终保存文件节点 |
| `parentId` | `Long` | 目标父目录 ID；为空时表示上传到根目录 |
| `totalSize` | `Long` | 完整文件总大小，单位为字节 |

---

## 3. 当前使用方式

当前前端在文件大小超过 `10MB` 时，会先计算完整文件 MD5，再把上述字段提交到 `/storage/uploadSlices`。

服务端会基于这些数据：

1. 校验文件名、父目录、容量和角色上传上限
2. 创建 `Slices` 会话对象
3. 生成独立 `uploadId`
4. 返回给前端继续上传单个切片
