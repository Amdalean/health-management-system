<p align="center">
	<img alt="logo" src="https://oscimg.oschina.net/oscnet/up-d3d0a9303e11d522a06cd263f3079027715.png">
</p>
<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">RuoYi v3.8.8</h1>
<h4 align="center">基于SpringBoot+Vue前后端分离的Java快速开发框架</h4>
<p align="center">
	<a href="https://gitee.com/y_project/RuoYi-Vue/stargazers"><img src="https://gitee.com/y_project/RuoYi-Vue/badge/star.svg?theme=dark"></a>
	<a href="https://gitee.com/y_project/RuoYi-Vue"><img src="https://img.shields.io/badge/RuoYi-v3.8.8-brightgreen.svg"></a>
	<a href="https://gitee.com/y_project/RuoYi-Vue/blob/master/LICENSE"><img src="https://img.shields.io/github/license/mashape/apistatus.svg"></a>
</p>

## 平台简介

若依是一套全部开源的快速开发平台，毫无保留给个人及企业免费使用。

* 前端采用Vue、Element UI。
* 后端采用Spring Boot、Spring Security、Redis & Jwt。
* 权限认证使用Jwt，支持多终端认证系统。
* 支持加载动态权限菜单，多方式轻松权限控制。
* 高效率开发，使用代码生成器可以一键生成前后端代码。

## 内置功能

1.  用户管理：用户是系统操作者，该功能主要完成系统用户配置。
2.  部门管理：配置系统组织机构（公司、部门、小组），树结构展现支持数据权限。
3.  岗位管理：配置系统用户所属担任职务。
4.  菜单管理：配置系统菜单，操作权限，按钮权限标识等。
5.  角色管理：角色菜单权限分配、设置角色按机构进行数据范围权限划分。
6.  字典管理：对系统中经常使用的一些较为固定的数据进行维护。
7.  参数管理：对系统动态配置常用参数。
8.  通知公告：系统通知公告信息发布维护。
9.  操作日志：系统正常操作日志记录和查询；系统异常信息日志记录和查询。
10. 登录日志：系统登录日志记录查询包含登录异常。
11. 在线用户：当前系统中活跃用户状态监控。
12. 定时任务：在线（添加、修改、删除)任务调度包含执行结果日志。
13. 代码生成：前后端代码的生成（java、html、xml、sql）支持CRUD下载 。
14. 系统接口：根据业务代码自动生成相关的api接口文档。
15. 服务监控：监视当前系统CPU、内存、磁盘、堆栈等相关信息。
16. 缓存监控：对系统的缓存信息查询，命令统计等。
17. 在线构建器：拖动表单元素生成相应的HTML代码。
18. 连接池监视：监视当前系统数据库连接池状态，可进行分析SQL找出系统性能瓶颈。


## 演示图

<table>
    <tr>
        <td><img src="https://oscimg.oschina.net/oscnet/cd1f90be5f2684f4560c9519c0f2a232ee8.jpg"/></td>
        <td><img src="https://oscimg.oschina.net/oscnet/1cbcf0e6f257c7d3a063c0e3f2ff989e4b3.jpg"/></td>
    </tr>
    <tr>
        <td><img src="https://oscimg.oschina.net/oscnet/up-8074972883b5ba0622e13246738ebba237a.png"/></td>
        <td><img src="https://oscimg.oschina.net/oscnet/up-9f88719cdfca9af2e58b352a20e23d43b12.png"/></td>
    </tr>
    <tr>
        <td><img src="https://oscimg.oschina.net/oscnet/up-39bf2584ec3a529b0d5a3b70d15c9b37646.png"/></td>
        <td><img src="https://oscimg.oschina.net/oscnet/up-936ec82d1f4872e1bc980927654b6007307.png"/></td>
    </tr>
	<tr>
        <td><img src="https://oscimg.oschina.net/oscnet/up-b2d62ceb95d2dd9b3fbe157bb70d26001e9.png"/></td>
        <td><img src="https://oscimg.oschina.net/oscnet/up-d67451d308b7a79ad6819723396f7c3d77a.png"/></td>
    </tr>	 
    <tr>
        <td><img src="https://oscimg.oschina.net/oscnet/5e8c387724954459291aafd5eb52b456f53.jpg"/></td>
        <td><img src="https://oscimg.oschina.net/oscnet/644e78da53c2e92a95dfda4f76e6d117c4b.jpg"/></td>
    </tr>
	<tr>
        <td><img src="https://oscimg.oschina.net/oscnet/up-8370a0d02977eebf6dbf854c8450293c937.png"/></td>
        <td><img src="https://oscimg.oschina.net/oscnet/up-49003ed83f60f633e7153609a53a2b644f7.png"/></td>
    </tr>
	<tr>
        <td><img src="https://oscimg.oschina.net/oscnet/up-d4fe726319ece268d4746602c39cffc0621.png"/></td>
        <td><img src="https://oscimg.oschina.net/oscnet/up-c195234bbcd30be6927f037a6755e6ab69c.png"/></td>
    </tr>
    <tr>
        <td><img src="https://oscimg.oschina.net/oscnet/b6115bc8c31de52951982e509930b20684a.jpg"/></td>
        <td><img src="https://oscimg.oschina.net/oscnet/up-5e4daac0bb59612c5038448acbcef235e3a.png"/></td>
    </tr>
</table>


## 时序图
```mermaid
sequenceDiagram
participant User
participant WebApp
participant Server
participant Database

    User->>WebApp: 输入用户名和密码
    WebApp->>Server: 发送登录请求（用户名、密码）
    Server->>Database: 查询用户名和密码
    Database-->>Server: 返回查询结果（用户信息）
    Server->>WebApp: 登录验证结果（成功或失败）
    WebApp-->>User: 显示登录结果（成功或失败）
```
```mermaid
graph TD
    A["前端层"] --> B["应用层"]
    B --> C["数据层"]
    B --> D["智能化支持层"]
    C --> E["数据库(MySQL)"]
    D --> F["ollama4大模型"]
    D --> G["qwen2通义千问模型"]

%%graph TD
%%    participant "前端层" as A
%%    participant "应用层" as B
%%    participant "数据层" as C
%%    participant "智能化支持层" as D
%%    participant "数据库(MySQL)" as E
%%    participant "ollama4大模型" as F
%%    participant "qwen2通义千问模型" as G
%%    A[test] ->> B: 调用
%%    B ->> C: 数据请求
%%    B ->> D: 智能化请求
%%    C ->> E: 数据存取
%%    D ->> F: 大模型调用
%%    D ->> G: 通义千问调用
```

系统总体架构分为多个层次，包括访问层、编程语言层、服务层、数据层、存储层和基础设施层。

- **访问层**：包括ToC和ToB的用户访问，分别通过WEB和PC端进行。
- **编程语言层**：WEB端使用Uniapp、Node、Vue，PC端使用Vue、Java。
- **服务层**：包含系统管理、数据池、业务服务、鉴权服务和系统监控等模块。
- **数据层**：负责数据缓存、存储过程、事务和消息管道的管理。
- **存储层**：使用Cache和Redis进行缓存，使用MySQL作为数据库。
- **基础设施层**：基于Linux和Windows操作系统，使用Docker进行容器化部署，并集成ollama中间件。

- **系统管理**：包括组织管理、用户管理、字典管理和菜单管理。
- **数据池**：包括学生建档、教师建档、课程建档和教学资料库。
- **业务服务**：包括资料检索、消息推送、专属大模型和成绩录入。
- **鉴权服务**：负责用户权限的管理和验证。
- **系统监控**：负责系统的日志记录和监控。


```plaintext
+---------------------+
|    用户前端界面     |
+---------------------+
           |
           v
+---------------------+
|      后端服务       |  <---> 数据库
|  (Spring Boot)      |
+---------------------+
           |
           v
+---------------------+
|   第三方服务模块   |
|  (AI处理, API调用) |
+---------------------+
```

```mermaid
sequenceDiagram
    participant 用户
    participant 学生系统
    participant 数据库

    用户->>学生系统: 输入学生信息
    学生系统->>数据库: 保存学生信息
    数据库-->>学生系统: 数据保存成功
    学生系统-->>用户: 提示保存成功
```



