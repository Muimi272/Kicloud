# KiCloud

[![Version](https://img.shields.io/badge/version-1.2.0-blue)](./pom.xml)
[![Java](https://img.shields.io/badge/Java-25-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-6DB33F)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-green)](./LICENSE)

KiCloud 是一个基于 Spring Boot 的轻量级私有网盘项目，采用前后端一体架构，提供用户认证、文件管理、分片上传、分享链接和后台管理等能力。

当前实现以单机部署为目标：

- 数据库存储使用 SQLite
- 文件内容存储在本地磁盘
- 登录态基于 Session
- 页面由 Thymeleaf 模板渲染

## 功能特性

- 用户登录、登出、注册
- 邀请码注册
- 普通用户、管理员、超级管理员角色控制
- 文件上传、分片上传、下载
- 用户控制台多文件顺序上传
- 大文件切片上传失败自动重试
- 文件夹创建、搜索、重命名、删除
- 文件分享链接创建、公开访问、密码校验与下载
- 管理后台用户、邀请码、文件和分享链接管理

## 技术栈

- Java 25
- Spring Boot 4
- Spring MVC
- Spring Security
- Spring Data JPA
- Hibernate
- SQLite
- Thymeleaf
- Lombok

## 运行环境

- JDK 25
- Maven Wrapper（仓库已包含 `mvnw` / `mvnw.cmd`）

## 快速启动

1. 确保本机安装并启用了 JDK 25。
2. 在项目根目录执行编译：

```powershell
./mvnw.cmd -q -DskipTests compile
```

3. 启动项目：

```powershell
./mvnw.cmd spring-boot:run
```

4. 浏览器访问：

- 登录页：`http://localhost:8080/user/login`
- 注册页：`http://localhost:8080/user/register`
- 用户控制台：`http://localhost:8080/dashboard`
- 管理后台：`http://localhost:8080/admin/back`

## 默认配置

当前默认配置位于 `src/main/resources/application.properties`：

- 数据库：`./data/kicloud.db`
- 文件存储目录：`./data/storage`
- Session 超时时间：`30m`
- 单次请求上传上限：`10GB`

开发环境默认会初始化一个超级管理员账号（如果数据库中尚不存在 `SUPERADMIN`）：

- 用户名：`Admin`
- 密码：`123456`

建议在正式使用前立即修改该配置或登录后重置密码。

## 上传说明

- 文件大小 `<= 10MB` 时，前端走单文件上传接口。
- 文件大小 `> 10MB` 时，前端会先计算完整文件 MD5，再走分片上传。
- 分片上传使用本地临时目录 `./data/temp`。
- 单个切片上传失败时，前端会自动重试；连续 5 次失败才终止该文件上传。

## 项目结构

```text
src/main/java/club/muimi/kicloud
├─ controller    # 页面路由与 JSON 接口入口
├─ service       # 核心业务逻辑
├─ dao           # JPA Repository
├─ entity        # 数据库实体
├─ model         # 请求模型、会话模型、枚举
├─ config        # Security / MVC 配置与拦截器
└─ tool          # 辅助工具
```

## 开发文档

项目内已经维护了较完整的开发文档，入口在：

- [Development Record/README.md](./Development%20Record/README.md)

建议阅读顺序：

1. [Development Record/总览/架构.md](./Development%20Record/%E6%80%BB%E8%A7%88/%E6%9E%B6%E6%9E%84.md)
2. [Development Record/service/存储模块说明.md](./Development%20Record/service/%E5%AD%98%E5%82%A8%E6%A8%A1%E5%9D%97%E8%AF%B4%E6%98%8E.md)
3. 对应的 `controller`、`config`、`service`、`model` 文档

## 许可证

本项目基于 [MIT License](./LICENSE) 开源。
