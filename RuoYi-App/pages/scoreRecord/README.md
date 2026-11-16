# 移动端成绩分析功能

## 概述

移动端成绩分析功能与PC端功能保持一致，用户可以在移动端查看、新增、编辑、删除成绩智能分析，并提交视频进行AI分析。

## 功能特性

### 1. 成绩分析列表
- **路径**: `pages/scoreRecord/index.vue`
- **功能**:
  - 查看所有成绩智能分析
  - 支持按AI评分和分析状态搜索
  - 支持下拉刷新和上拉加载更多
  - 显示分析状态（待分析、分析中、已完成、分析失败）
  - 支持提交AI分析、编辑、删除操作

### 2. 成绩分析详情
- **路径**: `pages/scoreRecord/detail.vue`
- **功能**:
  - 查看成绩智能分析的完整信息
  - 显示基本信息、AI评分、AI评语、分析详情、文件信息等
  - 支持提交AI分析和编辑操作

### 3. 成绩分析新增/编辑
- **路径**: `pages/scoreRecord/add.vue`
- **功能**:
  - 新增成绩智能分析
  - 编辑已有成绩智能分析
  - 支持上传视频文件（mp4, avi, mov, wmv, flv, mkv）
  - 显示分析类型、AI评分、分析状态等信息
  - 支持添加备注

## API 接口

所有API接口定义在 `api/main/scoreRecord.js`：

| 接口 | 方法 | 说明 |
|------|------|------|
| `listScoreRecord(query)` | GET | 查询成绩智能分析列表 |
| `getScoreRecord(id)` | GET | 查询成绩智能分析详细 |
| `addScoreRecord(data)` | POST | 新增成绩智能分析 |
| `updateScoreRecord(data)` | PUT | 修改成绩智能分析 |
| `delScoreRecord(id)` | DELETE | 删除成绩智能分析 |
| `submitForAnalysis(data)` | POST | 提交视频进行AI分析 |
| `getTaskStatus(taskId)` | GET | 查询AI分析任务状态 |

## 后端接口

后端接口由PC端提供，无需修改：

- **Controller**: `hms-main/src/main/java/com/hms/main/score/controller/HmsScoreRecordController.java`
- **Service**: `hms-main/src/main/java/com/hms/main/score/service/impl/HmsScoreRecordServiceImpl.java`
- **Mapper**: `hms-main/src/main/java/com/hms/main/score/mapper/HmsScoreRecordMapper.java`

### 后端API端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/main/scoreRecord/list` | GET | 查询列表 |
| `/main/scoreRecord/{id}` | GET | 查询详情 |
| `/main/scoreRecord` | POST | 新增 |
| `/main/scoreRecord` | PUT | 修改 |
| `/main/scoreRecord/{id}` | DELETE | 删除 |
| `/main/scoreRecord/submit` | POST | 提交分析 |
| `/main/scoreRecord/task/{taskId}` | GET | 查询任务状态 |

## 数据模型

### HmsScoreRecord 实体

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| courseId | Long | 课程ID |
| studentId | Long | 学生ID |
| filePath | String | 文件路径 |
| fileName | String | 文件原始名称 |
| fileSize | Long | 文件大小(字节) |
| analysisType | String | 分析类型 |
| score | BigDecimal | AI评分 |
| status | String | 分析状态(0-待分析, 1-分析中, 2-已完成, 3-分析失败) |
| feedback | String | AI评语 |
| analysisDetails | String | 分析详情 |
| taskId | String | 异步任务ID |
| remark | String | 备注 |
| createTime | Date | 创建时间 |
| updateTime | Date | 更新时间 |

## 使用说明

### 访问入口

1. 打开移动端应用
2. 点击底部导航栏的"工作台"
3. 在宫格菜单中点击"成绩分析"图标

### 操作流程

#### 查看成绩智能分析
1. 进入成绩分析列表页面
2. 可以搜索和筛选记录
3. 下拉刷新获取最新数据
4. 上滑动加载更多记录

#### 新增成绩智能分析
1. 点击列表页的"新增"按钮
2. 上传视频文件（支持mp4, avi, mov, wmv, flv, mkv格式）
3. 选择分析类型（默认为"八段锦"）
4. 添加备注（可选）
5. 点击"提交"保存记录

#### 提交AI分析
1. 在列表页或详情页找到状态为"待分析"或"分析失败"的记录
2. 点击"提交分析"按钮
3. 确认提交
4. 系统会异步进行AI分析，分析完成后状态会更新

#### 查看详情
1. 在列表页点击任意记录卡片
2. 进入详情页查看完整信息
3. 可以查看AI评分、评语和分析详情

#### 编辑记录
1. 在列表页或详情页点击"编辑"按钮
2. 修改相关信息
3. 点击"提交"保存修改

#### 删除记录
1. 在列表页点击"删除"按钮
2. 确认删除操作
3. 记录会被从系统中删除

## 状态说明

| 状态码 | 状态名称 | 说明 | 可操作 |
|--------|----------|------|--------|
| 0 | 待分析 | 记录已创建，等待提交分析 | 可提交分析、编辑、删除 |
| 1 | 分析中 | AI正在分析视频 | 仅可查看 |
| 2 | 已完成 | 分析已完成，可查看结果 | 仅可查看、编辑、删除 |
| 3 | 分析失败 | AI分析过程中出现错误 | 可重新提交分析、编辑、删除 |

## 技术栈

- **框架**: uni-app (Vue 3)
- **UI组件**: uni-ui
- **样式**: SCSS
- **HTTP请求**: 自定义request封装
- **文件上传**: uni.uploadFile

## 注意事项

1. 视频文件上传可能需要较长时间，请耐心等待
2. AI分析是异步过程，通常需要几分钟时间
3. 确保网络连接稳定，避免上传中断
4. 视频文件大小建议不超过500MB
5. 分析类型目前仅支持"八段锦"，后续可扩展更多类型

## 更新日期

2026-04-08 by CYQ
