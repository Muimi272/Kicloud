# LoginRequest 模型类说明

代码位置：`src/main/java/club/muimi/kicloud/model/LoginRequest.java`

---

## 1. 模型含义

`LoginRequest` 用于承接用户登录接口提交的数据，是 `/user/login` 接口的请求体模型。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 预留的用户标识字段，当前登录流程主要仍以用户名和密码校验为主 |
| `username` | `String` | 登录用户名 |
| `password` | `String` | 登录明文密码，服务端接收后用于认证校验 |
