# community.yonyou.com 社区登录 - 部署说明

## 概述

通过 Playwright 浏览器自动化实现 community.yonyou.com 社区登录，获取 TGC cookie。

由于用友 CAS 登录采用 RSA + 浏览器指纹 + 多段密码加密，纯 HTTP 请求无法复现，因此使用浏览器自动化方案。

## Linux 服务器部署步骤

### 1. 安装 Node.js (>=18)

```bash
# Ubuntu/Debian
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# CentOS/RHEL
curl -fsSL https://rpm.nodesource.com/setup_18.x | sudo bash -
sudo yum install -y nodejs
```

### 2. 安装 Playwright 和 Chromium

```bash
# 安装 Playwright 依赖
cd /path/to/health-management-system/hms-quartz/src/main/resources/playwright
npm install playwright

# 安装 Chromium 浏览器（无头模式）
npx playwright install chromium

# 安装系统依赖（Chromium 需要的库）
npx playwright install-deps chromium
```

### 3. 测试脚本

```bash
node login-community.js
```

期望输出：
```json
{"success":true,"tgc":"eyJhbGciOiJIUzUxMiJ9...","jsessionId":"pTyr12p8...","cookies":[...]}
```

### 4. 配置环境变量（可选）

在 Java 应用启动时设置脚本绝对路径：

```bash
export PLAYWRIGHT_SCRIPT=/path/to/health-management-system/hms-quartz/src/main/resources/playwright/login-community.js
```

或在 systemd service 文件中配置：
```ini
Environment=PLAYWRIGHT_SCRIPT=/opt/hms/playwright/login-community.js
```

### 5. 自定义凭证（可选）

```bash
COMMUNITY_USER=your-account COMMUNITY_PWD=your-password node login-community.js
```

## Java 调用

```java
NCCPunchCard punchCard = new NCCPunchCard();
String tgc = punchCard.communityLogin();
// tgc 可用于后续 API 调用的 Cookie 认证
```

## 常见问题排查

### `Executable doesn't exist at .../ms-playwright/chromium-XXXX/chrome-linux/chrome`

原因：`playwright` 包已安装，但对应的 Chromium 浏览器二进制没有下载（或被清理），
`XXXX` 是 revision 号，与 playwright 版本一一对应（如 1140 → 1.48.x，1223 → 1.60.x）。
**注意浏览器缓存在运行应用的那个用户 HOME 下**（报错路径含 `/home/www/...` 说明进程以 `www` 用户运行）。

```bash
# 1. 确认应用运行用户
ps -ef | grep hms-admin.jar | grep -v grep

# 2. 进入部署机的 playwright 目录，以该用户身份安装浏览器
cd /www/wwwroot/hms/playwright          # 按实际路径调整
npx playwright install chromium

# 3. 若应用以 www 运行、当前是 root，需切用户安装（HOME 决定缓存目录）
sudo -u www -H bash -lc 'cd /www/wwwroot/hms/playwright && npx playwright install chromium'

# 4. 补系统依赖库（需要 root）
npx playwright install-deps chromium    # OpenCloudOS/CentOS 系如不支持，见下方手工安装
```

版本务必对齐仓库 `package.json`（当前 `playwright@1.60.0` → chromium-1223）：

```bash
cd /www/wwwroot/hms/playwright
rm -rf node_modules
npm install
npx playwright install chromium
```

若服务器无法下载浏览器，可复用系统已装 Chrome，通过环境变量指定：

```bash
export PLAYWRIGHT_CHROMIUM_EXECUTABLE=/usr/bin/google-chrome   # 或 chromium-browser
```

校验是否可用：

```bash
sudo -u www -H bash -lc 'cd /www/wwwroot/hms/playwright && node login-community.js'
```

## 注意事项

- 脚本在无头模式(headless=true)下运行，Linux 服务器无需 X Window 系统
- 已添加 `--no-sandbox` 参数，适配 Docker/容器化环境
- 建议在 Linux 服务器上单独测试脚本后再配置定时任务
- TGC cookie 有效期较长，可考虑缓存以减少浏览器启动开销
- 定时任务建议错峰执行，避免高峰期页面加载缓慢