# 推送设备信息接口 (移动端专用)

## 接口列表

### 1. 上报推送设备信息 (移动端调用，无需登录)
- **URL**: `POST /mobile/push/device`
- **权限**: @Anonymous
- **请求参数**:
```json
{
  "clientId": "推送客户端ID (CID)",
  "platform": "平台 android/ios",
  "appVersion": "应用版本号"
}
```
- **返回**:
```json
{
  "code": 200,
  "msg": "设备信息上报成功"
}
```
- **说明**: 移动端获取到CID后调用此接口注册设备。支持未登录状态上报（userId为null），登录后再次调用会自动绑定userId。

### 2. 根据客户端ID获取推送设备信息
- **URL**: `GET /mobile/push/device/client/{clientId}`
- **权限**: @Anonymous
- **返回**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "userId": 100,
    "clientId": "xxx",
    "platform": "android",
    "appVersion": "1.3.2",
    "createTime": "2026-05-17 14:55:00",
    "updateTime": "2026-06-06 10:00:00"
  }
}
```

### 3. 查询推送设备信息列表 (PC管理后台)
- **URL**: `GET /mobile/push/device/list`
- **权限**: `system:push:list`
- **请求参数**: pageNum, pageSize, userId, clientId, platform
- **返回**: 分页表格数据

### 4. 获取推送设备详情 (PC管理后台)
- **URL**: `GET /mobile/push/device/{id}`
- **权限**: `system:push:query`

### 5. 修改推送设备信息 (PC管理后台)
- **URL**: `PUT /mobile/push/device`
- **权限**: `system:push:edit`

### 6. 删除推送设备信息 (PC管理后台)
- **URL**: `DELETE /mobile/push/device/{ids}`
- **权限**: `system:push:remove`
