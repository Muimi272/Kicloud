# UserDao接口说明

代码位置：`src/main/java/club/muimi/kicloud/dao/UserDao.java`

---

## 业务含义

`UserDao` 用于实现数据库中User表和业务之间的连接，由JPA自动实现。

## 抽象方法说明

- `Optional<User> findByUsername(String name)`
> 通过Username字段查询信息，并返回 `Optinal` 对象。

- `boolean existsByRole(Role role)`
> 查询表中是否含有 `Role` 字段为指定值的信息。