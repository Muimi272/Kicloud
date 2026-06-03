# Role 枚举说明

代码位置：`src/main/java/club/muimi/kicloud/model/Role.java`

---

## 1. 枚举含义

`Role` 用于表示系统中的用户角色，是登录态、Spring Security 授权、管理员权限划分和后台能力控制的基础枚举。

---

## 2. 当前枚举值

| 枚举值 | 说明 |
|---|---|
| `USER` | 普通用户 |
| `ADMIN` | 管理员 |
| `SUPERADMIN` | 超级管理员 |
