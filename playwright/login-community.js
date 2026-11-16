/**
 * 社区登录 + 打卡脚本
 *
 * 登录 community.yonyou.com 并完成每日签到
 * 
 * 用法: node login-community.js [--user=手机号] [--pwd=密码] [--clock]
 * 
 * 输出: JSON
 *   { success, tgc, clocked: true/false, clockResult: "打卡成功" }
 *
 * add by CYQ 2026年5月20日
 */

const { chromium } = require('playwright');

const args = process.argv.slice(2);
// 凭据从环境变量或命令行参数读取，勿写死在代码里
let username = process.env.COMMUNITY_USER || '';
let password = process.env.COMMUNITY_PWD || '';
let doClock = args.includes('--clock');
for (const arg of args) {
  if (arg.startsWith('--user=')) username = arg.split('=')[1];
  if (arg.startsWith('--pwd=')) password = arg.split('=')[1];
}

/**
 * 构造 Chromium 启动参数
 * - 优先使用环境变量指定的浏览器可执行文件，便于服务器复用系统 Chrome
 *   PLAYWRIGHT_CHROMIUM_EXECUTABLE 或 CHROMIUM_PATH
 * - 无沙箱 + 关闭 /dev/shm，适配 root/容器环境
 *
 * add by CYQ 2026年10月13日 修复服务器缺少 playwright 浏览器导致启动失败
 */
function buildLaunchOptions() {
  const execPath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE || process.env.CHROMIUM_PATH;
  const opts = {
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage', '--disable-gpu']
  };
  if (execPath) opts.executablePath = execPath;
  return opts;
}

(async () => {
  let browser;
  try {
    try {
      browser = await chromium.launch(buildLaunchOptions());
    } catch (launchErr) {
      // 给出可操作的中文指引（原始信息见 error）
      if (/Executable doesn't exist|please run the following command|playwright install/i.test(launchErr.message)) {
        throw new Error(
          'Chromium 浏览器未安装。请在应用运行用户下执行：'
          + 'cd <playwright目录> && npx playwright install chromium '
          + '（若权限不足可加 sudo -u <运行用户> -H）。原始错误: ' + launchErr.message
        );
      }
      throw launchErr;
    }
    const context = await browser.newContext();
    const page = await context.newPage();

    // ========== 1. 登录 ==========
    await page.goto('https://community.yonyou.com/login?redirect=%2Findex', {
      waitUntil: 'load', timeout: 30000
    });
    await page.waitForSelector('#yonbip_login_id', { timeout: 10000 });
    await page.waitForTimeout(2000);

    const frameLocator = page.frameLocator('#yonbip_login_id');
    await frameLocator.locator('#username').waitFor({ state: 'visible', timeout: 10000 });
    await frameLocator.locator('#username').fill(username);
    await frameLocator.locator('#password').fill(password);
    await page.waitForTimeout(500);

    let loginFrame = null;
    for (const f of page.frames()) {
      if (f.url().includes('euc.yonyoucloud.com/cas/login')) {
        loginFrame = f;
        break;
      }
    }

    if (!loginFrame) {
      process.stdout.write(JSON.stringify({ success: false, error: '未找到CAS登录iframe' }));
      return;
    }

    await page.waitForTimeout(1000);
    await loginFrame.evaluate(() => {
      if (typeof doLogin === 'function') doLogin();
      else throw new Error('doLogin未定义');
    });

    try {
      await page.waitForURL('**/index', { timeout: 30000 });
    } catch (e) {
      process.stdout.write(JSON.stringify({ success: false, error: '登录超时', url: page.url() }));
      return;
    }

    // ========== 2. 获取 TGC ==========
    const cookies = await context.cookies();
    const tgc = cookies.find(c => c.name === 'TGC' && c.domain.includes('yonyoucloud.com'));

    // ========== 3. 打卡（可选） ==========
    let clockResult = null;
    if (doClock) {
      try {
        // 获取 community-web-Token（Bearer token）
        const allCookies = await context.cookies();
        const tokenCookie = allCookies.find(c => c.name === 'community-web-Token');
        const bearerToken = tokenCookie ? tokenCookie.value : null;

        if (!bearerToken) {
          clockResult = { clocked: false, msg: '未获取到 community-web-Token' };
        } else {
          // 访问打卡页面
          await page.goto('https://community.yonyou.com/profile/index?showcomp=sign', {
            waitUntil: 'domcontentloaded', timeout: 20000
          });
          await page.waitForTimeout(2000);

          // 用 fetch 调打卡 API（带 Bearer token）
          const clockResp = await page.evaluate(async (token) => {
            const headers = {
              'Authorization': 'Bearer ' + token,
              'Accept': 'application/json'
            };
            // 先检查今天是否已打卡
            const check = await fetch('/iuap-ptc-ybdc/system/clock/isClockToday', { headers });
            const checkData = await check.json();
            if (checkData.data === true) {
              return { already: true, msg: '今日已打卡' };
            }
            // 执行打卡
            const resp = await fetch('/iuap-ptc-ybdc/system/clock/clock', { method: 'POST', headers });
            const data = await resp.json();
            return { already: false, msg: data.msg, code: data.code };
          }, bearerToken);

          clockResult = {
            clocked: true,
            msg: clockResp.msg || (clockResp.already ? '今日已打卡' : '打卡完成')
          };
        }
      } catch (clockErr) {
        clockResult = { clocked: false, msg: '打卡异常: ' + clockErr.message };
      }
    }

    // ========== 4. 输出 ==========
    process.stdout.write(JSON.stringify({
      success: true,
      tgc: tgc ? tgc.value : null,
      clocked: clockResult ? clockResult.clocked : false,
      clockMsg: clockResult ? clockResult.msg : null
    }));

  } catch (e) {
    process.stdout.write(JSON.stringify({ success: false, error: e.message }));
  } finally {
    if (browser) await browser.close();
  }
})();