# 微信小程序一键登录接口 (移动端专用)

## 流程说明

1. 小程序端 `wx.login()` 拿到 `code`，调 `wxLogin`
2. `openid` 已绑定系统账号 → 直接返回 `token`（与 `/login` 同规格，后续请求头带 `Authorization: Bearer {token}`）
3. `openid` 未绑定 → 返回 `needBind=true + openid`，前端跳绑定页，用户输入系统账号密码调 `bindLogin`
4. 绑定成功返回 `token`，之后该微信可一键登录
5. 会话过期自动续登（前端）：已登录后 token 到期、接口返回 401 时，小程序端 `RuoYi-App/utils/wxAuth.js` 会先自动 `wx.login` 调 `wxLogin` 静默换发新 token 并重放原请求，用户无感知；仅当未绑定/换取失败时才弹「登录状态已过期」确认框。主动退出登录（本地 token 已清）不会自动登回。

## 接口列表

### 1. 微信一键登录 (小程序调用，无需登录)

- **URL**: `POST /mobile/wx/login/wxLogin`
- **权限**: @Anonymous
- **请求参数**:
```json
{
  "code": "wx.login() 返回的 code"
}
```
- **返回（已绑定，直接登录）**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "token": "eyJhbGciOi..."
}
```
- **返回（未绑定，需先绑账号）**:
```json
{
  "code": 200,
  "msg": "该微信尚未绑定账号，请先绑定系统账号",
  "needBind": true,
  "openid": "oXXX123456"
}
```

### 2. 绑定系统账号并登录 (小程序调用，无需登录)

- **URL**: `POST /mobile/wx/login/bindLogin`
- **权限**: @Anonymous
- **请求参数**:
```json
{
  "openid": "wxLogin 返回的 openid",
  "username": "系统账号",
  "password": "系统密码"
}
```
- **返回**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "token": "eyJhbGciOi..."
}
```
- **说明**: 账号密码校验复用 `UserDetailsServiceImpl`（含停用/删除/密码错误重试锁定）。openid 主记录在 `sys_user.wx_openid`（SQL 见 `sql/alter/add_wx_openid_to_sys_user.sql`，唯一索引），绑定时如该 `openid` 曾归属其他账号会自动解绑，保证同一时间只归属一个用户。推送设备表 `hms_push_device.wx_openid` 同步一份（订阅消息发送用）。

### 3. 静默检查微信绑定状态 (小程序调用，无需登录，登录页显隐按钮用)

- **URL**: `POST /mobile/wx/login/checkBound`
- **权限**: @Anonymous
- **请求参数**:
```json
{
  "code": "wx.login() 返回的 code"
}
```
- **返回**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "bound": true
}
```
- **说明**: add by CYQ 2026-09-09 仅返回 `bound` 布尔值，不签发 `token`、不跳转。`bound=true` 才显示微信一键登录按钮；审核员新微信永远 `false`，只看到账号密码登录，满足“先浏览后登录”。

### 4. 设置页手动关联微信 (需登录)

- **URL**: `POST /mobile/wx/login/bind`
- **请求参数**:
```json
{
  "openid": "先调 /mobile/wx/subscribe/code2session 换到的 openid"
}
```
- **说明**: add by CYQ 2026-09-09 已登录态下绑定当前微信，同样先清该 `openid` 在其他账号下的归属，保证唯一。

### 5. 设置页手动解绑微信 (需登录)

- **URL**: `POST /mobile/wx/login/unbind`
- **请求参数**: 无
- **说明**: add by CYQ 2026-09-09 清空当前用户 `sys_user.wx_openid` 与推送设备表对应记录。

### 6. 查询当前用户微信绑定状态 (需登录，设置页展示用)

- **URL**: `POST /mobile/wx/login/status`
- **请求参数**: 无
- **返回**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "bound": true
}
```
