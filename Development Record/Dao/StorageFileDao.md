# StorageFileDao接口说明

代码位置：`src/main/java/club/muimi/kicloud/dao/StorageFileDao.java`

---

## 业务含义

`StorageFileDao` 用于实现数据库中 `StorageFile` 表和业务之间的连接，由JPA自动实现。

## 抽象方法说明

- `Optional<StorageFile> findByIdAndDeletedFalse(Long id)`
> 通过主键 `id` 查询未被逻辑删除的文件记录，并返回 `Optional` 对象。

- `Optional<StorageFile> findByIdAndOwnerIdAndDeletedFalse(Long id, Long ownerId)`
> 通过主键 `id` 和所属用户 `ownerId` 查询未被逻辑删除的文件记录，并返回 `Optional` 对象。

- `List<StorageFile> findActiveByOwnerIdAndParentId(Long ownerId, Long parentId)`
> 查询指定用户在某一父目录下的全部未删除文件记录；当 `parentId` 为空时，表示查询根目录内容，并按照文件类型升序、创建时间降序返回。

- `List<StorageFile> findByOwnerIdAndNameContainingIgnoreCaseAndDeletedFalseOrderByFileTypeAscCreatedAtDesc(Long ownerId, String keyword)`
> 按所属用户和文件名关键字进行忽略大小写的模糊查询，仅返回未删除记录，并按照文件类型升序、创建时间降序返回。

- `boolean existsActiveByOwnerIdAndParentIdAndName(Long ownerId, Long parentId, String name)`
> 查询指定用户在某一父目录下，是否已经存在同名且未删除的文件或文件夹记录。

- `boolean existsActiveSiblingNameExcludingId(Long ownerId, Long parentId, String name, Long id)`
> 查询指定用户在某一父目录下，除当前 `id` 外是否存在其他同名且未删除的同级文件记录。
