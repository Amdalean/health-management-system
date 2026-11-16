/**
 * 主题 Mixin - 跨端主题应用
 * 每个页面 mix 这个，根 view 自动带上 theme-xxx 类名
 */
export default {
  computed: {
    // 当前主题类名，供页面根视图使用
    themeClass() {
      return 'theme-' + (this.$store.state.theme.theme || 'default')
    }
  },
  // 页面显示时确保主题生效（小程序/App）
  onShow() {
    this.$nextTick(() => {
      this.applyThemeGlobally()
    })
  },
  methods: {
    applyThemeGlobally() {
      // #ifdef H5
      document.documentElement.className = this.themeClass
      // #endif
      // #ifdef MP-WEIXIN || MP-ALIPAY
      // 小程序通过页面根 view 的 class 生效，无需额外操作
      // #endif
    }
  }
}
