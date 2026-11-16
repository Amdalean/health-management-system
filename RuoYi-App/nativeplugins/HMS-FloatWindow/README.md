# HMS-FloatWindow 悬浮窗截屏识图插件

## 功能

系统级悬浮按钮 + AI 识图记账：
1. 在「设置」页面开启悬浮窗
2. 切换到其他 App（如微信/支付宝支付完成页）
3. **双击**悬浮按钮触发截屏
4. 自动调用 AI 识图 API 解析消费信息
5. 跳转随手记页面自动填充

## 平台支持

| 平台 | 悬浮窗 | 截屏 | 说明 |
|------|--------|------|------|
| Android | ✅ | ✅ (MediaProjection) | 首次需授予"显示在其他应用上层"权限 |
| HarmonyOS | ✅ | ⚠️ (受限) | 截屏非系统 App 受限制，回退到拍照识图 |
| iOS | ❌ | ❌ | 系统不支持悬浮窗 |

## 目录结构

```
HMS-FloatWindow/
├── package.json              # 插件清单
├── android/
│   ├── AndroidManifest.xml   # 组件声明
│   └── src/com/hms/plugin/floatwindow/
│       ├── FloatWindowModule.java    # JS 桥接模块
│       ├── FloatWindowService.java   # 悬浮窗前台服务
│       └── ScreenshotActivity.java   # 透明截屏 Activity
└── harmony/
    ├── index.ets                      # 插件入口
    └── src/
        └── FloatWindowModule.ets      # 鸿蒙浮窗模块
```

## JS 调用

```js
import { startFloatWindow, stopFloatWindow } from '@/utils/float-window.js'

// 启动
await startFloatWindow()

// 停止
await stopFloatWindow()
```
