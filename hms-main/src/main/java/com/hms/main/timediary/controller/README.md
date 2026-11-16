# 时间管理局 API

## 概述
时间管理局模块，用于记录每天的时间分配情况，支持快速录入时间段和事项，并提供可视化统计。

## 接口列表

### 1. 查询时间管理局列表
- **URL**: `GET /main/timeDiary/list`
- **权限**: `main:timeDiary:list`
- **参数**:
  - `diaryDate` - 日期（yyyy-MM-dd）
  - `category` - 分类（工作/学习/生活/其他）
  - `title` - 事项标题（模糊搜索）
  - `params[beginTime]` / `params[endTime]` - 日期范围
  - `pageNum` / `pageSize` - 分页
- **返回**: `TableDataInfo` 分页列表

### 2. 查询统计（分类占比 + 每日趋势）
- **URL**: `GET /main/timeDiary/stats`
- **权限**: `main:timeDiary:list`
- **参数**:
  - `params[beginTime]` / `params[endTime]` - 日期范围
- **返回**: `{ categoryDistribution: [{category, totalMinutes}], dailyTrend: [{diaryDate, totalMinutes}] }`

### 3. 查询某天时间轴
- **URL**: `GET /main/timeDiary/timeline`
- **权限**: `main:timeDiary:list`
- **参数**:
  - `diaryDate` - 日期（必填）
- **返回**: `AjaxResult` 当天按 start_time 排序的日记列表

### 4. 获取详情
- **URL**: `GET /main/timeDiary/{diaryId}`
- **权限**: `main:timeDiary:query`

### 5. 新增时间管理局
- **URL**: `POST /main/timeDiary`
- **权限**: `main:timeDiary:add`
- **Body**:
```json
{
  "diaryDate": "2026-08-04",
  "startTime": "08:30",
  "endTime": "10:00",
  "category": "工作",
  "title": "项目管理",
  "description": "处理项目进度跟进",
  "durationMin": 90
}
```

### 6. 修改时间管理局
- **URL**: `PUT /main/timeDiary`
- **权限**: `main:timeDiary:edit`

### 7. 删除时间管理局
- **URL**: `DELETE /main/timeDiary/{diaryIds}`
- **权限**: `main:timeDiary:remove`

### 8. 导出
- **URL**: `POST /main/timeDiary/export`
- **权限**: `main:timeDiary:export`

## 数据表
`hms_time_diary` — 时间管理局表
