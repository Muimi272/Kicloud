# InvitationDao接口说明

代码位置：`src/main/java/club/muimi/kicloud/dao/InvitationDao.java`

---

## 业务含义

`InvitationDao` 用于实现数据库中 `Invitation` 表和业务之间的连接，由JPA自动实现。

## 抽象方法说明

- `boolean existsByInviteCode(String inviteCode)`
> 查询表中是否已存在指定邀请码 `inviteCode` 的记录。

- `Optional<Invitation> findByInviteCode(String inviteCode)`
> 通过邀请码 `inviteCode` 查询信息，并返回 `Optional` 对象。
