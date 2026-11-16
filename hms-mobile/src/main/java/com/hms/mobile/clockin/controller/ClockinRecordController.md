# 打卡记录 Controller API 文档

## 接口列表

### 移动端接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /mobile/clockin/officeLocations | 获取办公地点列表及内勤外勤区分开关 | 无 |
| POST | /mobile/clockin | 打卡上报 | 登录即可 |
| GET | /mobile/clockin/list | 查询当前用户打卡记录 | 登录即可 |
| GET | /mobile/clockin/{id} | 查询打卡记录详情 | 登录即可 |

### 管理后台接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /mobile/clockin/admin/export | 导出打卡记录 | mobile:clockin:export |
| GET | /mobile/clockin/admin/list | 分页查询打卡记录 | mobile:clockin:list |
| GET | /mobile/clockin/admin/{id} | 获取打卡详情 | mobile:clockin:query |
| DELETE | /mobile/clockin/admin/{ids} | 删除打卡记录 | mobile:clockin:remove |
| GET | /mobile/clockin/admin/statistics | 打卡统计（按日期汇总考勤用户打卡情况） | mobile:clockin:statistics |
| GET | /mobile/clockin/admin/dailyDetail | 指定日期打卡明细（已打卡/未打卡用户 + 工作日状态） | mobile:clockin:statistics |

## 办公地点接口

`GET /mobile/clockin/officeLocations` 返回办公地点列表及是否启用内勤外勤区分：

```json
{
  "code": 200,
  "data": {
    "enableOfficeType": true,
    "locations": [
      { "id": 1, "name": "默认办公点", "latitude": 39.9042, "longitude": 116.4074, "radius": 500 }
    ]
  }
}
```

- `enableOfficeType`: 是否启用内勤外勤区分（对应参数 `mobile.clockin.enableOfficeType`，false 时移动端打卡不校验内勤外勤）
- `locations`: 启用的办公地点列表

## 打卡统计接口

### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| beginDate | String | 否 | 开始日期，格式 yyyy-MM-dd |
| endDate | String | 否 | 结束日期，格式 yyyy-MM-dd |

### 返回示例

```json
{
  "code": 200,
  "data": {
    "list": [
      { "clockinDate": "2026-06-10", "clockinCount": 12 }
    ],
    "totalAssigned": 14
  }
}
```

- `totalAssigned`: 考勤角色（role_id=105）的用户总数
- `list.clockinDate`: 打卡日期
- `list.clockinCount`: 当日打卡的考勤用户数（去重）

## 每日打卡明细接口

### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| date | String | 是 | 日期，格式 yyyy-MM-dd |

### 返回示例

```json
{
  "code": 200,
  "data": {
    "date": "2026-06-10",
    "clockedInUsers": [
      {
        "userId": 101, "userName": "CYQ", "nickName": "管理员",
        "deptName": "研发部", "clockinType": "内勤",
        "clockinTime": "2026-06-10 08:30:00",
        "address": "北京市朝阳区...", "latitude": 39.9, "longitude": 116.4
      }
    ],
    "notClockedInUsers": [
      { "userId": 102, "userName": "user1", "nickName": "张三", "deptName": "市场部" }
    ],
    "isWorkday": true,
    "typeName": "工作日",
    "holidayName": ""
  }
}
```

- `isWorkday`: 是否为工作日（调用互联网节假日API查询）
- `typeName`: 日期类型名称（工作日/休息日/节假日/调休）
- `holidayName`: 节假日名称（如："春节"），非节假日为空
- 工作日API: http://timor.tech/api/holiday/info/$date

## 变更记录

| 日期 | 说明 |
|------|------|
| 2026-06-05 | 初始创建，支持移动端打卡 |
| 2026-06-06 | 新增PC端导出、列表、详情、删除接口 |
| 2026-06-10 | 新增打卡统计接口 `/admin/statistics` |
| 2026-06-10 | 新增每日打卡明细接口 `/admin/dailyDetail`，集成工作日查询 |
| 2026-08-13 | `officeLocations` 返回结构增加 `enableOfficeType` 开关；新增参数 `mobile.clockin.enableOfficeType` 控制是否校验内勤外勤 |
