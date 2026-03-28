# 健康管理系统 (HMS) - 项目上下文

## 项目概述

**HMS (Health Management System)** 是一个基于 RuoYi 框架的综合性健康管理系统。这是一个基于 Spring Boot + Vue 的系统，专为健康管理目的设计，采用前后端分离架构。系统包含多个模块，用于用户管理、健康记录、文档管理以及AI集成。

### 关键技术:
- **前端**: Vue.js, Element UI
- **后端**: Spring Boot, Spring Security, Redis, JWT
- **数据库**: MySQL
- **AI 集成**: Ollama4j, 通义千问 API
- **构建工具**: Maven

### 架构:
- **前端层**: Uniapp, Node.js, Vue.js
- **后端层**: Java (Spring Boot)
- **数据库层**: MySQL 配合 Redis 缓存
- **AI 服务**: 集成 Ollama 和 通义千问模型

### 重要规范：
- 单元测试代码请写到：health-management-system\hms-admin\src\test\java
- 所有新增的RestController接口，需要在同目录下新增/修改md文件
- 创建的类和关键方法，需要写创建时间，参考 add by CYQ 日期 说明
- 代码请遵循mvc规范

### 项目结构:
```
health-management-system/
├── hms-admin/              # 主应用程序入口点
├── hms-common/             # 通用工具和组件
├── hms-framework/          # 核心框架和基础组件
├── hms-generator/          # 代码生成模块
├── hms-main/               # 主业务逻辑模块
├── hms-quartz/             # 任务调度模块
├── hms-system/             # 系统模块 (用户、角色等)
├── RuoYi-Vue3/             # 前端 Vue.js 应用
├── RuoYi-APP/              # 移动端应用
├── bin/                    # 可执行文件目录
├── doc/                    # 文档目录
├── sql/                    # 数据库初始化脚本
├── .gitignore
├── LICENSE
├── QWEN.md
├── README.md
├── pom.xml
├── ry.bat
└── ry.sh
```

## 核心功能

1. **用户管理**: 完整的用户生命周期管理
2. **部门管理**: 层次化组织结构
3. **角色管理**: 基于角色的访问控制
4. **菜单管理**: 动态菜单和权限配置
5. **健康记录**: 学生/教师建档功能
6. **文档管理**: 健康文档处理
7. **AI 集成**: 集成 Ollama 和 通义千问进行智能处理
8. **任务调度**: 基于 Quartz 的任务调度
9. **API 文档**: 基于 Swagger 的 API 文档
10. **财务摘要**: 财务数据管理及存款规划预测功能

## 开发设置

### 后端设置:
1. **前置条件**:
   - Java 8+
   - Maven 3.6+
   - MySQL 5.7+
   - Redis

2. **数据库设置**:
   - 从 `sql/` 目录导入 SQL 文件
   - 更新 `hms-admin/src/main/resources/application-prod.yml` 中的数据库配置

3. **运行后端**:
   ```bash
   # 导航到项目根目录
   cd D:\\space\\IDE\\health-management-system
   
   # 编译和打包项目
   mvn clean package
   
   # 使用批处理脚本运行应用程序
   ry.bat (并选择选项 1 启动)
   
   # 或直接使用 Java 运行
   java -Dname=hms-admin.jar -Duser.timezone=Asia/Shanghai -Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m -jar hms-admin/target/hms-admin.jar
   ```

4. **配置文件**:
   - 主配置: `hms-admin/src/main/resources/application.yml`
   - 数据库配置: `hms-admin/src/main/resources/application-prod.yml`
   - Druid 配置: `hms-admin/src/main/resources/application-druid.yml`

### 前端设置:
1. **前置条件**:
   - Node.js (>=8.9)
   - npm (>=3.0.0)

2. **运行前端**:
   ```bash
   cd D:\\space\\IDE\\health-management-system\\hms-ui
   
   # 安装依赖
   npm install
   
   # 启动开发服务器
   npm run dev
   
   # 构建生产版本
   npm run build:prod
   ```

## 关键配置

### 应用配置 (application.yml):
- **端口**: 8080
- **文件上传大小**: 最大 500MB
- **Redis**: localhost:6379
- **JWT Token**: 30分钟过期
- **数据库**: MySQL 在 82.156.236.247:3306 (ry 数据库)
- **AI 集成**: 已配置通义千问 API

### 默认用户:
- **管理员**: 用户名: YHC, 密码: 123456
- **标准用户**: 用户名: user01, 密码: user01

## 开发规范

1. **代码结构**: 遵循 RuoYi 框架约定，不同关注点分模块
2. **安全性**: 基于 JWT 的认证和基于角色的访问控制
3. **数据库**: 使用 MyBatis 进行 ORM，Druid 连接池
4. **API 设计**: RESTful API，配有 Swagger 文档
5. **前端**: 使用 Element UI 组件的 Vue.js

## AI 集成

系统集成了多种 AI 服务:
- Ollama4j 用于本地 AI 模型集成
- 通义千问 API 用于云端 AI 处理
- 通过应用属性配置，具有特定的 API 端点

## 财务摘要模块增强

`hms-main` 中的财务摘要模块已增强了智能预测功能:
- **存款规划**: 使用预测分析可视化计划与实际存款
- **历史增长分析**: 从历史数据计算平均月度增长
- **预测建模**: 对于没有实际数据的月份，系统现在使用历史趋势预测预期存款
- **动态预测**: 基于过去表现自动预测未来存款值

### HsmSummaryServiceImpl 中的关键更新:
- `getDepositPlanData()` 方法现在计算历史平均月度增长
- 如果当前月份数据尚未维护，系统将基于历史趋势预测值
- API 现在每月返回三个值：实际、预测和计划存款
- 添加了 `calculateAverageMonthlyGrowth()` 辅助方法分析历史数据模式

### DepositPlanChart 组件的前端更新:
- 更新了 `DepositPlanChart/index.vue` 以消费来自后端的新预测存款数据
- 修改了图表配置，显示实际、预测和计划存款为三条不同的线
- 移除了客户端预测计算逻辑，因为预测现在由服务器端处理

## 项目依赖

### 后端依赖 (来自 pom.xml):
- Spring Boot 2.5.15
- Spring Security 5.7.12
- MyBatis Plus
- Druid 连接池
- JWT 认证
- FastJSON 2 JSON 处理
- Quartz 调度

### 前端依赖 (来自 package.json):
- Vue 2.6.12
- Element UI 2.15.14
- Axios HTTP 请求
- ECharts 数据可视化
- Vue Router 和 Vuex

## 运行应用程序

可以使用以下方式启动应用程序:
- Windows: `ry.bat` (选择适当选项)
- Unix/Linux: `ry.sh` (带有 start/stop/restart/status 命令)

应用程序默认可通过 `http://localhost:8080` 访问。

## 特殊说明

1. 项目通过本地 Ollama 模型和云通义千问集成了 AI 功能
2. 数据库凭证目前在配置文件中硬编码
3. 项目使用 RuoYi 框架作为基础，提供了许多内置功能
4. 系统支持基于 JWT 令牌的多租户认证
5. 文件上传功能配置为支持大文件（最大 500MB）
6. 财务摘要模块现在包含存款规划的预测分析

该项目结构良好，遵循企业应用程序的现代开发实践，并具有 AI 集成用于健康数据处理和智能财务预测的附加优势。

## Qwen Added Memories
- 健康管理系统 (HMS) 是一个基于 RuoYi 框架的综合性健康管理系统，采用前后端分离架构，包含用户管理、健康记录、文档管理以及AI集成等功能。系统使用Spring Boot + Vue技术栈，包含多个模块如hms-admin、hms-common、hms-framework、hms-system、hms-quartz、hms-generator、hms-main、hms-ui等。
