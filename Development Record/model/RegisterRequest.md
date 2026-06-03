# RegisterRequest 模型类说明

代码位置：`src/main/java/club/muimi/kicloud/model/RegisterRequest.java`

---

## 1. 模型含义

`RegisterRequest` 用于承接用户注册时提交的数据，是 `/user/register` 接口的请求体模型。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `invitationCode` | `String` | 注册邀请码，用于校验是否允许创建账号 |
| `username` | `String` | 待注册用户名 |
| `password` | `String` | 待注册明文密码，服务端会进一步转换为密码哈希后保存 |
