# 03-dao

本目录用于存放与 `src/main/java/club/muimi/kicloud/dao` 对应的数据访问层文档。

---

## `dao`实现方式

本项目中，`dao`对象通过以接口继承 `JpaRepository<T, Long>` 来实现。

同时，`dao` 中的抽象方法也将由JPA进行自动识别与实现。

`dao` 中的抽象方法为补充JPA未自动创建的方法。JPA会在无声明时自动实现以下方法：

- `save(S entity)`
- `saveAll(Iterable<S> entities)`
- `findById(ID id)`
- `existsById(ID id)`
- `findAll()`
- `findAllById(Iterable<ID> ids)`
- `count()`
- `deleteById(ID id)`
- `delete(T entity)`
- `deleteAllById(Iterable<? extends ID> ids)`
- `deleteAll(Iterable<? extends T> entities)`
- `deleteAll()`


---

## 当前文档索引

- [UserDao](./UserDao.md)
- [StorageFileDao](./StorageFileDao.md)
- [InvitationDao](./InvitationDao.md)
- [LinkDao](./LinkDao.md)
