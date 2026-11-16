<script>
  import config from './config'
  import store from '@/store'
  import { getToken } from '@/utils/auth'
  import Push from '@/utils/push.js'
  import { reportLocation } from '@/api/system/location'
  import { ensurePrivacyAuthorized } from '@/utils/privacy'

  // 城市上报节流：6 小时内只上报一次
  const CITY_REPORT_INTERVAL = 6 * 3600 * 1000
  const CITY_REPORT_TIME_KEY = 'city_report_time_v2'
  // 上报失败后的重试间隔：5 分钟
  const CITY_REPORT_RETRY_INTERVAL = 5 * 60 * 1000
  const CITY_REPORT_ATTEMPT_KEY = 'city_report_attempt_v2'

  export default {
    onLaunch: function() {
      // 初始化主题
      this.$store.commit('theme/INIT_THEME')
      this.applyTheme()
      this.initApp()
      // 初始化 UniPush 2.0 推送服务
      this.initPush()
    },
    onShow: function() {
      // 从后台回到前台时也尝试上报一次（内部有 6 小时节流）
      this.reportCity(3000)
    },
    methods: {
      applyTheme() {
        const theme = this.$store.state.theme.theme
        // #ifdef H5
        document.documentElement.className = 'theme-' + theme
        // #endif
      },
      initApp() {
        this.initConfig()
        //#ifdef H5
        this.checkLogin()
        //#endif
        // 静默上报当前城市，供天气推送使用
        // add by Qwen 2026-10-25
        this.reportCity(3000)
      },
      initConfig() {
        this.globalData.config = config
      },
      checkLogin() {
        if (!getToken()) {
          this.$tab.reLaunch('/pages/login')
        }
      },
      initPush() {
        // #ifdef APP-PLUS
        try {
          Push.init()
          console.log('[App] UniPush 2.0 已启动初始化')
        } catch (e) {
          console.error('[App] UniPush 初始化异常:', e)
        }
        // #endif
      },
      /**
       * 静默上报当前城市
       * 仅 APP / 鸿蒙 APP / 微信小程序 端执行，定位失败或未授权时静默跳过
       * add by Qwen 2026-10-25
       */
      reportCity(delay = 0) {
        // #ifdef APP-PLUS || APP-HARMONY || MP-WEIXIN
        try {
          if (!getToken()) return
          // 成功上报后 6 小时内不再重复
          const last = uni.getStorageSync(CITY_REPORT_TIME_KEY)
          if (last && Date.now() - Number(last) < CITY_REPORT_INTERVAL) return
          // 失败重试降频：5 分钟内不重复尝试，避免反复弹权限框
          const lastTry = uni.getStorageSync(CITY_REPORT_ATTEMPT_KEY)
          if (lastTry && Date.now() - Number(lastTry) < CITY_REPORT_RETRY_INTERVAL) return
          uni.setStorageSync(CITY_REPORT_ATTEMPT_KEY, Date.now())
          setTimeout(() => this.doReportCity(), delay)
        } catch (e) {
          console.log('[城市上报] 初始化异常:', e)
        }
        // #endif
      },
      async doReportCity() {
        // 微信小程序：调隐私接口前先确认《用户隐私保护指引》已授权
        // #ifdef MP-WEIXIN
        const privacy = await ensurePrivacyAuthorized()
        if (!privacy.ok) {
          console.log('[城市上报] 隐私授权未通过，跳过:', privacy.err && privacy.err.errMsg)
          return
        }
        // #endif
        uni.getLocation({
          type: 'gcj02',
          success: (res) => {
            console.log('[城市上报] 定位成功:', res.longitude + ',' + res.latitude)
            this.reportCoordinates(res.longitude, res.latitude)
          },
          fail: (err) => {
            console.log('[城市上报] 定位失败，跳过:', err && err.errMsg)
          }
        })
      },
      /**
       * 上报坐标，城市由服务端逆地理编码解析
       * 这样小程序端不需要配置 restapi.amap.com 域名白名单
       * add by Qwen 2026-10-26
       */
      reportCoordinates(lng, lat) {
        const payload = { latitude: lat, longitude: lng, source: 'app' }
        console.log('[城市上报] 上报坐标:', JSON.stringify(payload))
        reportLocation(payload).then((res) => {
          // 仅成功后记时间，失败可在 5 分钟后重试
          uni.setStorageSync(CITY_REPORT_TIME_KEY, Date.now())
          console.log('[城市上报] 成功:', res && res.msg)
        }).catch((e) => {
          console.log('[城市上报] 接口失败:', (e && (e.message || e.msg)) || e)
        })
      }
    }
  }
</script>

<style lang="scss">
@import '@/static/scss/index.scss';
@import '@/styles/themes.css';
</style>
