# LoginUser 模型类说明

代码位置：`src/main/java/club/muimi/kicloud/model/LoginUser.java`

---

## 1. 模型含义

`LoginUser` 是登录态中的轻量用户对象，用于保存到 `HttpSession`，并在拦截器、控制器、服务层之间传递当前登录用户信息。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 当前登录用户的主键标识 |
| `username` | `String` | 当前登录用户的用户名 |
| `role` | `Role` | 当前登录用户的系统角色，用于权限判断 |
