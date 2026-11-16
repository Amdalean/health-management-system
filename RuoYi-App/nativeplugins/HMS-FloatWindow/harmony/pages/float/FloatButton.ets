/**
 * HMS 悬浮窗截屏识图 — 鸿蒙悬浮按钮页面
 * add by CYQ 2026-06-03
 *
 * 说明：
 * - 加载在全屏透明子窗口中作为悬浮按钮
 * - 双击触发截屏识图（通过 EventHub 通知原生模块）
 * - 支持拖拽移动按钮位置
 * - 透明区域不拦截触摸（HitTestMode.Transparent）
 */

import { common } from '@kit.AbilityKit';

@Entry
@Component
struct FloatButton {
  @State btnX: number = 280
  @State btnY: number = 400

  private dragStartX: number = 0
  private dragStartY: number = 0

  aboutToAppear(): void {
    // 初始位置：右侧偏上
    // 子窗口为全屏，坐标是相对子窗口（等于屏幕坐标）
  }

  build() {
    Stack() {
      // 悬浮按钮
      Stack() {
        Circle()
          .width(52)
          .height(52)
          .fill('#409EFF')
          .shadow({ radius: 10, color: '#33000000', offsetX: 0, offsetY: 4 })

        Text('+')
          .fontSize(28)
          .fontColor(Color.White)
          .fontWeight(FontWeight.Bold)
          .textAlign(TextAlign.Center)
      }
      .width(56)
      .height(56)
      .borderRadius(28)
      .position({ x: this.btnX, y: this.btnY })
      .hitTestBehavior(HitTestMode.Block)
      .gesture(
        GestureGroup(GestureMode.Parallel,
          // 双击 → 截屏识图
          TapGesture({ count: 2 })
            .onAction(() => {
              this.emitDoubleClick()
            }),
          // 拖拽 → 移动按钮位置
          PanGesture({ direction: PanDirection.All, distance: 5 })
            .onActionStart(() => {
              this.dragStartX = this.btnX
              this.dragStartY = this.btnY
            })
            .onActionUpdate((event: GestureEvent) => {
              this.btnX = this.dragStartX + event.offsetX
              this.btnY = this.dragStartY + event.offsetY
            })
        )
      )
    }
    .width('100%')
    .height('100%')
    .hitTestBehavior(HitTestMode.Transparent) // 透明区域触摸穿透到主窗口
  }

  /**
   * 双击事件 → 通过 EventHub 通知原生模块
   */
  emitDoubleClick(): void {
    try {
      const ctx = getContext() as common.UIAbilityContext
      ctx?.eventHub?.emit('floatWindowDoubleClick')
    } catch (e) {
      console.error('[FloatButton] emitDoubleClick: ' + JSON.stringify(e))
    }
  }
}