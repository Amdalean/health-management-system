# 成绩分析记录 API 文档

**创建时间**: 2026-03-01  
**作者**: CYQ

## 接口列表

### 1. 提交视频进行 AI 分析

**接口地址**: `POST /main/scoreRecord/submit`

**权限**: `main:scoreRecord:submit`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| courseId | Long | 是 | 课程 ID |
| filePath | String | 是 | 视频文件路径 |
| fileName | String | 否 | 视频文件原始名称 |
| fileSize | Long | 否 | 文件大小（字节） |
| analysisType | String | 否 | 分析类型（如：八段锦） |

**请求示例**:

```json
{
  "courseId": 1001,
  "filePath": "/profile/upload/2026/03/01/baduanjin_video.mp4",
  "fileName": "八段锦练习视频.mp4",
  "fileSize": 52428800,
  "analysisType": "八段锦完整套路"
}
```

**响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "courseId": 1001,
    "studentId": 2,
    "filePath": "/profile/upload/2026/03/01/baduanjin_video.mp4",
    "fileName": "八段锦练习视频.mp4",
    "fileSize": 52428800,
    "analysisType": "八段锦完整套路",
    "taskId": "a1b2c3d4e5f6g7h8i9j0",
    "status": "1",
    "createTime": "2026-03-01 10:00:00"
  }
}
```

**处理流程**:

1. 生成唯一任务 ID（taskId）
2. 设置分析状态为 `1`（分析中）
3. 保存记录到数据库
4. 异步调用八段锦 AI 打分服务
5. AI 打分完成后自动更新状态：
   - 成功：状态改为 `2`（已完成），写入分数和评语
   - 失败：状态改为 `3`（分析失败），写入错误信息

---

### 2. 查询 AI 分析任务状态

**接口地址**: `GET /main/scoreRecord/task/{taskId}`

**权限**: `main:scoreRecord:query`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | String | 是 | 任务 ID（提交时返回） |

**响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "courseId": 1001,
    "studentId": 2,
    "filePath": "/profile/upload/2026/03/01/baduanjin_video.mp4",
    "taskId": "a1b2c3d4e5f6g7h8i9j0",
    "status": "2",
    "score": 95.5,
    "feedback": "整体表现优秀，动作流畅自然",
    "analysisDetails": "{\"totalScore\":95.5,\"pass\":true,\"level\":\"优秀\",...}",
    "updateTime": "2026-03-01 10:05:00"
  },
  "taskId": "a1b2c3d4e5f6g7h8i9j0",
  "status": "2",
  "statusDesc": "已完成"
}
```

**状态说明**:

| 状态码 | 状态说明 |
|--------|---------|
| 0 | 待分析 |
| 1 | 分析中 |
| 2 | 已完成 |
| 3 | 分析失败 |

---

### 3. 查询成绩分析记录列表

**接口地址**: `GET /main/scoreRecord/list`

**权限**: `main:scoreRecord:list`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页数量，默认 10 |
| studentId | Long | 否 | 学生 ID |
| score | BigDecimal | 否 | AI 评分 |
| status | String | 否 | 分析状态 |

**响应示例**:

```json
{
  "code": 200,
  "msg": "查询成功",
  "total": 10,
  "rows": [
    {
      "id": 1,
      "courseId": 1001,
      "studentId": 2,
      "filePath": "/profile/upload/2026/03/01/video1.mp4",
      "score": 95.5,
      "status": "2",
      "feedback": "整体表现优秀",
      "createTime": "2026-03-01 10:00:00"
    }
  ]
}
```

---

### 4. 获取成绩分析记录详情

**接口地址**: `GET /main/scoreRecord/{id}`

**权限**: `main:scoreRecord:query`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 记录 ID |

**响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "courseId": 1001,
    "studentId": 2,
    "filePath": "/profile/upload/2026/03/01/video1.mp4",
    "fileName": "八段锦练习视频.mp4",
    "fileSize": 52428800,
    "analysisType": "八段锦完整套路",
    "score": 95.5,
    "status": "2",
    "feedback": "整体表现优秀，动作流畅自然",
    "analysisDetails": "{\"totalScore\":95.5,\"pass\":true,\"level\":\"优秀\",\"movements\":[...]}",
    "taskId": "a1b2c3d4e5f6g7h8i9j0",
    "createTime": "2026-03-01 10:00:00",
    "updateTime": "2026-03-01 10:05:00"
  }
}
```

---

## AI 评分详情格式

`analysisDetails` 字段包含完整的 AI 评分 JSON 数据，格式如下：

```json
{
  "totalScore": 95.5,
  "pass": true,
  "level": "优秀",
  "movements": [
    {
      "name": "两手托天理三焦",
      "score": 95,
      "accuracy": 92,
      "completeness": 96,
      "timing": 97,
      "posture": 95,
      "feedback": "动作舒展，托举到位"
    },
    {
      "name": "左右开弓似射雕",
      "score": 93,
      "accuracy": 90,
      "completeness": 94,
      "timing": 95,
      "posture": 93,
      "feedback": "开弓姿势标准，眼神到位"
    }
  ],
  "overallFeedback": "整体表现优秀，动作流畅自然",
  "suggestions": [
    "第三式'调理脾胃须单举'时，上举手下按的力度可以更强一些",
    "注意呼吸与动作的配合"
  ],
  "highlights": [
    "起势收势规范",
    "动作连贯流畅"
  ]
}
```

---

## 异步处理说明

### 流程图

```
用户提交视频
    ↓
生成 taskId
    ↓
状态设为 1(分析中)
    ↓
保存记录到数据库
    ↓
返回 taskId 给用户
    ↓
[异步线程]
    ↓
调用八段锦 AI 打分
    ↓
┌───────────┬───────────┐
│   成功    │    失败    │
└───────────┴───────────┘
    ↓             ↓
状态=2(已完成)   状态=3(分析失败)
写入分数和评语    写入错误信息
```

### 前端轮询建议

前端可以使用 taskId 轮询查询任务状态：

```javascript
// 提交后开始轮询
const taskId = response.data.taskId;
const timer = setInterval(() => {
  axios.get(`/main/scoreRecord/task/${taskId}`).then(res => {
    const status = res.data.status;
    if (status === '2') {
      // 已完成
      clearInterval(timer);
      // 刷新列表或显示结果
    } else if (status === '3') {
      // 分析失败
      clearInterval(timer);
      // 显示错误信息
    }
    // status === '1' 继续等待
  });
}, 3000); // 每 3 秒轮询一次
```

---

## 相关文件

- Controller: `hms-main/src/main/java/com/hms/main/score/controller/HmsScoreRecordController.java`
- Service: `hms-main/src/main/java/com/hms/main/score/service/impl/HmsScoreRecordServiceImpl.java`
- Mapper: `hms-main/src/main/java/com/hms/main/score/mapper/HmsScoreRecordMapper.java`
- AI 策略：`hms-main/src/main/java/com/hms/ai/strategy/BaduanjinScoringStrategy.java`
