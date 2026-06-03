# LinkDao接口说明

代码位置：`src/main/java/club/muimi/kicloud/dao/LinkDao.java`

---

## 业务含义

`LinkDao` 用于实现数据库中 `Link` 表和业务之间的连接，由JPA自动实现。

## 抽象方法说明

- `Optional<Link> findByIdAndDeletedFalse(Long id)`
> 通过主键 `id` 查询未被逻辑删除的分享链接记录，并返回 `Optional` 对象。

- `Optional<Link> findByIdAndOwnerIdAndDeletedFalse(Long id, Long ownerId)`
> 通过主键 `id` 和所属用户 `ownerId` 查询未被逻辑删除的分享链接记录，并返回 `Optional` 对象。

- `Optional<Link> findByLinkIdAndDeletedFalse(String linkId)`
> 通过外部分享标识 `linkId` 查询未被逻辑删除的分享链接记录，并返回 `Optional` 对象。

- `boolean existsByLinkId(String linkId)`
> 查询表中是否已存在指定分享标识 `linkId` 的记录。

- `List<Link> findByOwnerIdAndDeletedFalseOrderByCreatedAtDesc(Long ownerId)`
> 查询指定用户创建的全部未删除分享链接，并按照创建时间倒序返回。

- `List<Link> findByDeletedFalseOrderByCreatedAtDesc()`
> 查询系统中全部未删除分享链接，并按照创建时间倒序返回。
