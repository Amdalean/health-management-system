# 推送消息发送接口

> UniPush 2.0 推送通过 uniCloud 云函数（`uni-cloud-push` 扩展）发送。
> 云函数源码位置: `RuoYi-App/uniCloud-aliyun/cloudfunctions/push/`

## 接口列表

### 1. 向指定设备发送推送
- **URL**: `POST /mobile/push/message/single`
- **权限**: `mobile:pushDevice:send`
- **请求参数**:
```json
{
  "clientId": "设备CID",
  "title": "通知标题",
  "body": "通知内容"
}
```
- **返回**:
```json
{
  "code": 200,
  "msg": "推送已发送",
  "data": "{\"code\":0,\"msg\":\"success\",\"data\":{...}}"
}
```

### 2. 向所有设备发送推送
- **URL**: `POST /mobile/push/message/all`
- **权限**: `mobile:pushDevice:send`
- **请求参数**:
```json
{
  "title": "通知标题",
  "body": "通知内容"
}
```

### 3. 向指定用户的所有设备推送
- **URL**: `POST /mobile/push/message/user`
- **权限**: `mobile:pushDevice:send`
- **请求参数**:
```json
{
  "userId": "用户ID",
  "title": "通知标题",
  "body": "通知内容"
}
```

## 部署须知

1. 在 HBuilder X 中，右键 `uniCloud-aliyun/cloudfunctions/push/` → **上传部署**
2. 在 uniCloud 控制台 → 云函数 → push → **启用 URL化**
3. 将生成的 URL化地址填入 `application.yml` 的 `unipush.function-url`
