# 用户所在城市上报 API 文档

**创建时间**: 2026-10-26  
**作者**: Qwen

用于天气推送按用户实际所在城市取数：移动端 App 启动（或回到前台）时静默获取定位，逆地理编码后上报城市，直接写入 `sys_user`，天气定时任务据此选择高德天气接口的 `adcode`。**不新建表。**

## 存储字段（sys_user）

| 字段 | 说明 |
|------|------|
| `current_city` | 当前所在城市（直辖市用省名兜底） |
| `current_district` | 当前所在区县 |
| `current_adcode` | 高德行政区划编码，天气接口 `city` 参数 |
| `current_latitude` / `current_longitude` | 上报坐标（GCJ-02） |
| `location_time` | 位置上报时间 |

## 配置项

| 配置项 | 说明 |
|--------|------|
| `weather.location.userId` | 天气推送取哪个用户的位置，默认 `108`；未配置、未上报或上报超过 30 天时回退默认城市（厦门市湖里区 350206） |

## 接口列表

### 1. 上报当前所在城市

**接口地址**: `POST /mobile/location/report`

**权限**: 登录即可

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| province | String | 否 | 省，如「福建省」 |
| city | String | 否 | 市，如「厦门市」；直辖市传省名 |
| district | String | 否 | 区/县，如「湖里区」 |
| adcode | String | 否 | 高德行政区划编码，如「350206」；**不传时由服务端用经纬度逆地理编码补全** |
| latitude | Double | 否 | 纬度（GCJ-02） |
| longitude | Double | 否 | 经度（GCJ-02） |
| source | String | 否 | 来源，默认 `app` |

**请求示例**:

```json
{
  "province": "福建省",
  "city": "厦门市",
  "district": "湖里区",
  "adcode": "350206",
  "latitude": 24.512345,
  "longitude": 118.123456,
  "source": "app"
}
```

**响应示例**:

```json
{
  "code": 200,
  "msg": "位置上报成功"
}
```

### 2. 查询当前用户最近上报的城市

**接口地址**: `GET /mobile/location/current`

**响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "city": "厦门市",
  "district": "湖里区",
  "adcode": "350206",
  "latitude": 24.512345,
  "longitude": 118.123456,
  "locationTime": "2026-10-26 08:30:00"
}
```

## 客户端调用位置

`RuoYi-App/App.vue`：`onLaunch` / `onShow` 调用 `reportCity()`，节流 6 小时一次；定位失败或用户未授权定位时静默跳过，不影响使用。
