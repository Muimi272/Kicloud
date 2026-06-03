# Invitation 实体类说明

代码位置：`src/main/java/club/muimi/kicloud/entity/Invitation.java`

---

## 1. 实体含义

`Invitation` 实体用于表示系统中的邀请码记录，是用户注册的核心数据模型。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 主键 |
| `inviteCode` | `String` | 邀请码内容，唯一且不能为空 |
| `used` | `boolean` | 是否已被使用 |
| `valid` | `boolean` | 当前邀请码是否有效 |
| `userId` | `Long` | 使用该邀请码注册的用户 ID |
| `generatorId` | `Long` | 生成该邀请码的管理员用户 ID |
| `createdAt` | `LocalDateTime` | 邀请码创建时间 |
| `usedAt` | `LocalDateTime` | 邀请码使用时间 |
