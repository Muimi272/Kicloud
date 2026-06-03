# AdminInvitationStatusRequest 模型类说明

代码位置：`src/main/java/club/muimi/kicloud/model/AdminInvitationStatusRequest.java`

---

## 1. 模型含义

`AdminInvitationStatusRequest` 用于承接管理员调整邀请码状态的数据，是 `/admin/invitation/status` 接口的请求体模型。

---

## 2. 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `invitationId` | `Long` | 需要修改状态的邀请码记录 ID |
| `valid` | `Boolean` | 邀请码是否有效；`true` 表示有效，`false` 表示失效 |
