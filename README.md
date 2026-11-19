# 基于大模型的实践类课程教学评估系统

面向高校体育类实践课程的教学评估系统，核心能力是用通用大模型对**八段锦练习视频**按团体标准**逐式评分**，并把评分纳入课程的教学评估流程。

> 本项目把《八段锦》团体标准中的文字规定转成模型可执行的判据，探索通用大模型在动作技能评价中的可用性，
> 并给出异步调度、输出清洗、失败兜底等工程实现，供同类场景参考。

---

## 一、要解决的问题

实践类课程的动作技能评价长期依赖教师现场逐人观察打分：

- 一个教学班通常数十名学生，教师需在一节课内完成全部记录与评分；
- 评分标准虽有统一要求，不同教师对动作细节的把握仍有差异；
- 成绩课后集中评定，学生数天后才拿到一个总分，不知道哪一式不达标。

本系统把《八段锦》团体标准中"动作、呼吸、意念"的文字规定编码为提示词，由大模型直接对练习视频逐式评分并生成评语，在不训练专用模型、不依赖标注数据的前提下，为课程过程性评价提供参考。

**注意：模型评分只作为参考，最终成绩仍由教师复核确认。**

---

## 二、系统功能

| 一级模块 | 子功能 |
|---|---|
| 用户与权限管理 | 用户管理、角色与菜单权限、部门管理、登录鉴权与个人中心 |
| 教学基础数据管理 | 学生建档、教师建档、课程建档、学生选课与退课、生成系统账号 |
| **成绩智能分析（核心）** | 练习视频提交、大模型智能评分、分析结果查询与导出、分析过程日志 |
| 教学资源管理 | 教学资料分类与上传下载、资料在线预览、知识地图 |
| 系统监控与运维 | 操作日志与登录日志、集成调用日志、定时任务、服务器与缓存监控 |

其中"成绩智能分析"承担核心评估手段，其余模块为评分业务提供评估对象、评估范围、评估依据与结果归档的支撑。

---

## 三、技术栈

| 层次 | 选型 |
|---|---|
| 后端 | Spring Boot 2.5.15、Spring Security、MyBatis、JDK 17 |
| 前端 | Vue 3、Element Plus、Vite（PC 端）；uni-app（移动端） |
| 存储 | MySQL 5.7、Redis |
| 模型服务 | OpenAI 兼容协议接入，支持通义千问（DashScope）与本地 Ollama 部署 |
| 架构 | RuoYi-Vue 前后端分离，JWT 无状态鉴权 |

---

## 四、评分链路

```
学生提交练习视频
      ↓
写入评分记录，状态置 0（待分析）
      ↓
点击提交 → 状态置 1（分析中）→ 立即返回 taskId（毫秒级）
      ↓
@Async 异步线程调用大模型，按团体标准逐式评分
      ↓
提示词约束 → 解析前清洗 Markdown 包裹 → 解析失败置 3 并记录原因
      ↓
状态置 2（已完成），写回分数、总评与逐式评语
      ↓
前端每 3 秒轮询任务状态，完成后展示逐式评语与改进建议
      ↓
教师复核确认
```

三个关键设计：

1. **异步执行 + 状态机 + 轮询**：单次推理需数十秒至数分钟，提交请求在毫秒级返回，避免请求线程被阻塞。
2. **三层防护**：提示词约束输出 JSON 结构 → 解析前清洗非结构化噪声 → 解析失败写回失败状态而非向前端抛异常。
3. **模板方法 + 策略 + 工厂**：`AbstractAIChatTemplate` 固定调用骨架，策略承载各场景提示词与输出约束，`AIChatFactory` 按"服务商 + 业务场景"组装实例，新增评分场景只需实现一个策略类。

---

## 五、目录结构

```
├── hms-admin            # Web 入口：启动类、系统级 Controller、配置文件
├── hms-framework        # 核心框架：Security 配置、JWT 过滤器、拦截器
├── hms-system           # 系统模块：用户/角色/菜单/部门
├── hms-common           # 通用工具：注解、异常、基类、XSS 过滤、Excel
├── hms-main             # 业务主模块（见下）
│   └── com/hms/main/
│       ├── account      # 生成系统账号
│       ├── student      # 学生建档
│       ├── teacher      # 教师建档
│       ├── course       # 课程建档与选课
│       ├── score        # 成绩智能分析（核心）
│       ├── document     # 教学资源与知识地图
│       └── integration  # 外部服务集成调用
│   └── com/hms/ai/
│       ├── template     # AI 调用骨架（模板方法）
│       ├── strategy     # 场景策略（含八段锦评分、成绩分析）
│       ├── factory      # 对话实例工厂
│       └── qwen/ollama  # 各服务商实现
├── hms-mobile           # 移动端服务：设备上报、推送、版本管理
├── hms-quartz           # 定时任务
├── hms-generator        # 代码生成器
├── RuoYi-Vue3           # PC 前端
├── RuoYi-App            # 移动端（uni-app）
└── sql                  # 建表与初始化脚本
```

---

## 六、论文与代码的对应关系

| 论文章节 | 代码位置 |
|---|---|
| 3.2 角色与权限设计 | `hms-framework`、`hms-system`、`RuoYi-Vue3/src/views/system` |
| 4.2 用户与权限管理 | `hms-admin`、`hms-system` |
| 4.3 学生建档、生成系统账号 | `hms-main/.../student`、`hms-main/.../account` |
| 4.4 教师建档 | `hms-main/.../teacher` |
| 4.5 课程与选课 | `hms-main/.../course` |
| 4.6 成绩智能分析 | `hms-main/.../score`、`hms-main/com/hms/ai` |
| 4.7 教学资源管理 | `hms-main/.../document` |
| 4.8 系统监控与运维 | `hms-admin/.../monitor`、`hms-main/.../integration` |
| 第五章 系统测试 | `hms-admin/src/test/java/com/hms/test` |

---

## 七、快速开始

**环境要求**：JDK 17、Maven 3.6+、Node 18+、MySQL 5.7、Redis

1. 初始化数据库
   ```bash
   mysql -uroot -p -e "create database ry default charset utf8mb4"
   mysql -uroot -p ry < sql/ry_20240629.sql
   mysql -uroot -p ry < sql/quartz.sql
   ```

2. 配置环境变量（不要把凭据写进配置文件）
   ```bash
   export QWEN_API_KEY=your_key
   export MYSQL_HOST=127.0.0.1
   export REDIS_HOST=127.0.0.1
   ```

3. 启动后端
   ```bash
   mvn clean package -Dmaven.test.skip=true
   java -jar hms-admin/target/hms-admin.jar
   ```

4. 启动前端
   ```bash
   cd RuoYi-Vue3 && npm install && npm run dev
   ```

5. 访问 `http://localhost:80`，默认管理员 `admin` / `admin123`

> 如需本地部署大模型，先启动 Ollama 并拉取模型：`ollama pull qwen3.5:9b`，并在 `application.yml` 中把服务商切换为 `OLLAMA`。

---

## 八、测试

```bash
# 单元测试：评分策略、提示词与输出清洗
mvn test -pl hms-admin -Dtest=BaduanjinScoringTest

# 端到端：提交视频 → 轮询 → 查看逐式评分
mvn test -pl hms-admin -Dtest=BaduanjinApiE2ETest
```

---

## 九、许可

基于 [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue)（MIT）二次开发，遵循 MIT 协议。
