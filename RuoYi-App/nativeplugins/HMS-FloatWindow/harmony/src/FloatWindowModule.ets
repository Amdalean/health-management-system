/**
 * HMS 悬浮窗截屏识图 — HarmonyOS 原生模块
 * add by CYQ 2026-06-03
 *
 * 说明：
 * - 悬浮窗使用 window.createWindow + TYPE_SUB_WINDOW 实现
 *   （第三方应用无法使用 TYPE_FLOAT 系统级悬浮窗）
 * - 子窗口为全屏透明，内部仅渲染一个可拖拽的圆形按钮
 * - 透明区域触摸穿透到主窗口，按钮区域拦截触摸
 * - 双击按钮触发截屏识图流程
 * - 切换到其他 App 后子窗口随主窗口隐藏
 */

import { window } from '@kit.ArkUI';
import { common } from '@kit.AbilityKit';
import { BusinessError } from '@kit.BasicServicesKit';

const TAG = 'HMS-FloatWindow';

// 全局状态
let floatWindow: window.Window | null = null;
let isRunning: boolean = false;
let eventListener: (() => void) | null = null;
let uiContext: common.UIAbilityContext | null = null;
// 截屏结果缓存，供 JS 端轮询读取
let screenshotBase64: string | null = null;

export class FloatWindowModule {

  /**
   * 启动悬浮窗
   * @param options 启动参数
   * @returns Promise<{code: number, msg: string}>
   */
  start(options: Record<string, Object>): Promise<Record<string, Object>> {
    return new Promise(async (resolve, reject) => {
      try {
        if (isRunning) {
          resolve({ code: 0, msg: '悬浮窗已运行' });
          return;
        }

        // 获取 UIAbility 上下文
        const ctx = getContext() as common.UIAbilityContext;
        if (!ctx) {
          reject({ code: -1, msg: '无法获取应用上下文' });
          return;
        }
        uiContext = ctx;

        // 创建全屏透明子窗口
        const config: window.Configuration = {
          name: 'hms_float_sub_window',
          windowType: window.WindowType.TYPE_SUB_WINDOW,
          ctx: ctx
        };

        floatWindow = await window.createWindow(config);

        // 全屏布局
        await floatWindow.setWindowLayoutFullScreen(true);
        // 透明背景
        await floatWindow.setBackgroundColor('#00000000');
        // 窗口属性：透明、不可聚焦
        const winProps: window.WindowProperties = {
          isTransparent: true,
          alpha: 1.0
        };
        await floatWindow.setWindowProperties(winProps);

        // 加载悬浮按钮页面
        floatWindow.loadContent('pages/float/FloatButton', (err: BusinessError) => {
          if (err && err.code !== 0) {
            console.error(TAG + ' 加载页面失败: ' + JSON.stringify(err));
            cleanup();
            reject({ code: -1, msg: `加载悬浮窗UI失败: ${err.message}` });
            return;
          }

          // 显示子窗口
          floatWindow?.showWindow().then(() => {
            console.log(TAG + ' 子窗口已显示');
          }).catch((e: BusinessError) => {
            console.error(TAG + ' 显示子窗口失败: ' + JSON.stringify(e));
          });

          isRunning = true;

          // 注册双击事件监听
          registerDoubleClickListener(ctx);

          resolve({ code: 0, msg: '悬浮窗已启动' });
        });

      } catch (err) {
        const error = err as BusinessError;
        console.error(TAG + ' 启动失败: ' + JSON.stringify(error));
        cleanup();
        reject({ code: -1, msg: `启动失败: ${error.message || '未知错误'}` });
      }
    });
  }

  /**
   * 停止悬浮窗
   */
  stop(): Promise<Record<string, Object>> {
    return new Promise(async (resolve) => {
      try {
        unregisterDoubleClickListener();

        if (floatWindow) {
          await floatWindow.hideWindow();
          await floatWindow.destroyWindow();
          floatWindow = null;
        }
        isRunning = false;
        screenshotBase64 = null;
        resolve({ code: 0, msg: '悬浮窗已停止' });
      } catch (err) {
        isRunning = false;
        floatWindow = null;
        screenshotBase64 = null;
        resolve({ code: 0, msg: '悬浮窗已停止' });
      }
    });
  }

  /**
   * 检查是否正在运行
   */
  isShowing(): Record<string, Object> {
    return { showing: isRunning };
  }

  /**
   * 检查悬浮窗权限 — 子窗口方式无需额外权限
   */
  checkPermission(): Record<string, Object> {
    return { granted: true };
  }

  /**
   * 获取截屏结果（JS 端轮询调用）
   * 双击按钮后，模块尝试截屏并缓存结果；
   * JS 端定时调用此方法获取截屏 base64 数据
   */
  getScreenshotResult(): Record<string, Object> {
    if (screenshotBase64) {
      const data = screenshotBase64;
      screenshotBase64 = null; // 读后清空，避免重复消费
      return { code: 0, imageBase64: data };
    }
    return { code: -1, msg: '暂无截屏数据' };
  }
}

// ======================== 内部函数 ========================

/**
 * 注册双击事件监听 — FloatButton 页面通过 EventHub 回调
 */
function registerDoubleClickListener(ctx: common.UIAbilityContext): void {
  unregisterDoubleClickListener();

  eventListener = () => {
    console.log(TAG + ' 收到双击事件');
    onFloatButtonDoubleClick();
  };

  try {
    ctx.eventHub.on('floatWindowDoubleClick', eventListener);
  } catch (e) {
    console.error(TAG + ' 注册事件监听失败: ' + JSON.stringify(e));
  }
}

/**
 * 注销双击事件监听
 */
function unregisterDoubleClickListener(): void {
  if (eventListener && uiContext) {
    try {
      uiContext.eventHub.off('floatWindowDoubleClick', eventListener);
    } catch (e) {
      // ignore
    }
    eventListener = null;
  }
}

/**
 * 悬浮按钮双击处理
 */
function onFloatButtonDoubleClick(): void {
  // 截屏并缓存结果，供 JS 端 getScreenshotResult() 读取
  captureMainWindow().then((base64) => {
    if (base64) {
      screenshotBase64 = base64;
      console.log(TAG + ' 截屏成功，数据长度: ' + base64.length);
    } else {
      console.warn(TAG + ' 截屏返回空数据');
      screenshotBase64 = null;
    }
  }).catch((err) => {
    console.error(TAG + ' 截屏失败: ' + JSON.stringify(err));
    screenshotBase64 = null;
  });
}

/**
 * 截取主窗口画面
 */
async function captureMainWindow(): Promise<string | null> {
  try {
    const ctx = getContext() as common.UIAbilityContext;
    if (!ctx) return null;

    const mainWindow = await window.getLastWindow(ctx);
    const screenshot = await mainWindow.screenshot();

    // PixelMap → JPEG → base64
    const packer = image.createImagePacker();
    const packOpts: image.PackingOption = {
      format: 'image/jpeg',
      quality: 85
    };

    const arrayBuffer = await packer.packing(screenshot, packOpts);
    const base64Data = arrayBufferToBase64(arrayBuffer);

    packer.release();
    screenshot.release();

    return base64Data;
  } catch (err) {
    console.error(TAG + ' captureMainWindow 失败: ' + JSON.stringify(err));
    return null;
  }
}

/**
 * ArrayBuffer → base64
 */
function arrayBufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}

/**
 * 清理资源
 */
function cleanup(): void {
  unregisterDoubleClickListener();
  if (floatWindow) {
    try {
      floatWindow.destroyWindow();
    } catch (e) {
      // ignore
    }
    floatWindow = null;
  }
  isRunning = false;
  screenshotBase64 = null;
}

// 需要引入 image 模块（captureMainWindow 使用）
import { image } from '@kit.ImageKit';