<template>
  <view class="login-container">
    <!-- 网络错误提示 -->
    <view class="network-error" v-if="networkError" @click="retryNetwork">
      <text class="error-icon">⚠️</text>
      <text class="error-text">网络连接失败，请检查网络后点击此处重试</text>
    </view>
    <view class="logo-area">
      <image style="width: 100rpx; height: 100rpx;" :src="globalConfig.appInfo.logo" mode="widthFix"></image>
      <text class="app-name">Caelum 登录</text>
    </view>
    <view class="form-area">
      <view class="input-row">
        <view class="iconfont icon-user icon"></view>
        <input v-model="loginForm.username" class="input" type="text" placeholder="请输入账号" maxlength="30" />
      </view>
      <view class="input-row">
        <view class="iconfont icon-password icon"></view>
        <input v-model="loginForm.password" password class="input" placeholder="请输入密码" maxlength="20" />
      </view>
      <view class="input-row captcha-row" v-if="captchaEnabled">
        <view class="iconfont icon-code icon"></view>
        <input v-model="loginForm.code" type="number" class="input" placeholder="请输入验证码" maxlength="4" />
        <image v-if="!codeError" :src="codeUrl" @click="getCode" @error="onCodeError" class="captcha-img"></image>
        <view v-else class="captcha-fail" @click="getCode">
          <text>加载失败</text>
          <text class="retry-link">点击重试</text>
        </view>
      </view>
      <view class="remember-row" @click="toggleRememberPassword">
        <view class="checkbox" :class="{ 'checked': rememberPassword }">
          <text v-if="rememberPassword" class="check-mark">✓</text>
        </view>
        <text class="remember-text">记住密码</text>
      </view>
      <button @click="handleLogin" class="login-btn">登录</button>
      <!-- #ifdef MP-WEIXIN -->
      <!-- 仅已关联微信才显示，避免未绑定用户进门即被索要微信授权 -->
      <!-- add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录 -->
      <button v-if="showWxLogin" @click="handleWxLogin" class="login-btn wx-login-btn">微信一键登录</button>
      <!-- #endif -->
      <view class="register-row" v-if="register">
        <text class="gray-text">没有账号？</text>
        <text @click="handleUserRegister" class="link-text">立即注册</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getCodeImg, checkWxBound } from '@/api/login'
import { getToken } from '@/utils/auth'

export default {
  data() {
    return {
      codeUrl: "",
      codeError: false,
      captchaEnabled: false,
      networkError: false,
      register: true,
      // 登录请求进行中标记，防重复点击刷出并发请求
      // add by CYQ 2026-09-09 修复登录失败转圈不消失
      logging: false,
      // 是否显示微信一键登录：默认隐藏，静默查到已绑定才显示
      // add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
      showWxLogin: false,
      globalConfig: getApp().globalData.config,
      loginForm: {
        username: "",
        password: "",
        code: "",
        uuid: ''
      },
      rememberPassword: false
    }
  },
  created() {
    // 已登录则自动跳转首页
    if (getToken()) {
      this.$store.dispatch('GetInfo').then(() => {
        this.$tab.reLaunch('/pages/index')
      }).catch(() => {
        this.getCode()
        this.loadSavedCredentials()
      })
      return
    }
    this.getCode()
    this.loadSavedCredentials()
    this.checkWxBoundSilent()
  },
  methods: {
    // 静默检查当前微信是否已绑定：已绑定才显示一键登录按钮
    // wx.login 不弹授权框，合规；失败/未绑定则保持隐藏，只走账号密码登录
    // add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
    checkWxBoundSilent() {
      // #ifdef MP-WEIXIN
      uni.login({
        success: (loginRes) => {
          if (!loginRes.code) return
          checkWxBound(loginRes.code).then((res) => {
            this.showWxLogin = res.bound === true
          }).catch(() => {
            this.showWxLogin = false
          })
        },
        fail: () => {
          this.showWxLogin = false
        }
      })
      // #endif
    },
    loadSavedCredentials() {
      const savedUsername = uni.getStorageSync('saved_username')
      const savedPassword = uni.getStorageSync('saved_password')
      if (savedUsername && savedPassword) {
        this.loginForm.username = savedUsername
        this.loginForm.password = savedPassword
        this.rememberPassword = true
      }
    },
    toggleRememberPassword() {
      this.rememberPassword = !this.rememberPassword
    },
    saveCredentials() {
      if (this.rememberPassword) {
        uni.setStorageSync('saved_username', this.loginForm.username)
        uni.setStorageSync('saved_password', this.loginForm.password)
      } else {
        uni.removeStorageSync('saved_username')
        uni.removeStorageSync('saved_password')
      }
    },
    handleUserRegister() {
      this.$tab.redirectTo(`/pages/register`)
    },
    getCode() {
      this.codeError = false
      getCodeImg().then(res => {
        this.networkError = false
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          this.codeUrl = 'data:image/gif;base64,' + res.img
          this.loginForm.uuid = res.uuid
        }
      }).catch(() => {
        this.networkError = true
        this.codeError = true
      })
    },
    onCodeError() {
      this.codeError = true
    },
    retryNetwork() {
      this.networkError = false
      this.getCode()
    },
    async handleLogin() {
      if (this.logging) return
      if (this.loginForm.username === "") {
        this.$modal.msgError("请输入您的账号")
      } else if (this.loginForm.password === "") {
        this.$modal.msgError("请输入您的密码")
      } else if (this.loginForm.code === "" && this.captchaEnabled) {
        this.$modal.msgError("请输入验证码")
      } else {
        this.logging = true
        this.$modal.loading("登录中，请耐心等待...")
        this.pwdLogin()
      }
    },
    async pwdLogin() {
      this.$store.dispatch('Login', this.loginForm).then(() => {
        this.$modal.closeLoading()
        this.logging = false
        this.loginSuccess()
      }).catch(() => {
        // 失败必须关转圈，否则 showLoading 永久卡住（转圈bug根因）
        // add by CYQ 2026-09-09 修复登录失败转圈不消失
        this.$modal.closeLoading()
        this.logging = false
        if (this.captchaEnabled) {
          this.getCode()
        }
      })
    },
    loginSuccess(result) {
      this.saveCredentials()
      this.$store.dispatch('GetInfo').then(res => {
        this.$tab.reLaunch('/pages/index')
      }).catch(() => {
        this.$modal.msgError('获取用户信息失败，请重试')
      })
    },
    // 微信一键登录（仅小程序）：wx.login 拿 code，后端按 openid 查绑定用户
    // 已绑定直接登录，未绑定跳绑定页，绑定后可一键登录
    // add by CYQ 2026-09-06 微信小程序一键登录
    // #ifdef MP-WEIXIN
    handleWxLogin() {
      const that = this
      uni.login({
        success(loginRes) {
          if (!loginRes.code) {
            that.$modal.msgError('获取微信登录凭证失败')
            return
          }
          that.$modal.loading('微信登录中，请耐心等待...')
          that.$store.dispatch('WxLogin', loginRes.code).then((res) => {
            that.$modal.closeLoading()
            if (res.needBind) {
              that.$tab.navigateTo('/pages/wxbind?openid=' + res.openid)
            } else {
              that.loginSuccess()
            }
          }).catch(() => {
            that.$modal.closeLoading()
          })
        },
        fail() {
          that.$modal.msgError('唤起微信登录失败')
        }
      })
    }
    // #endif
  }
}
</script>

<style lang="scss">
page {
  background-color: #ffffff;
}
.network-error {
  background-color: #fff3cd;
  color: #856404;
  padding: 16rpx 30rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  .error-icon { margin-right: 10rpx; }
  .error-text { flex: 1; }
}
.login-container {
  width: 100%;
  .logo-area {
    padding-top: 15%;
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    .app-name {
      margin-top: 10rpx;
      font-size: 20px;
    }
  }
  .form-area {
    margin: 1.25rem 1.25rem auto;
    text-align: center;
    .input-row {
      display: flex;
      align-items: center;
      margin: 1.25rem auto;
      height: 45px;
      border-radius: 9999px;
      background-color: #f5f6f7;
      .icon {
        font-size: 38rpx;
        margin-left: 10px;
        color: #999;
      }
      .input {
        width: 100%;
        font-size: 14px;
        line-height: 20px;
        text-align: left;
        padding-left: 15px;
      }
      .captcha-img {
        width: 200rpx;
        height: 38px;
      }
    }
    .captcha-row {
      width: 100%;
      margin: 1.25rem auto;
    }
    .captcha-fail {
      min-width: 200rpx;
      height: 38px;
      background: #eee;
      color: #999;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      font-size: 22rpx;
      border-radius: 4rpx;
      flex-shrink: 0;
      .retry-link { color: #3b82f6; font-size: 20rpx; }
    }
    .remember-row {
      display: flex;
      align-items: center;
      margin-top: 0.5rem;
      .checkbox {
        width: 18px;
        height: 18px;
        border: 2px solid #ccc;
        border-radius: 4px;
        display: flex;
        align-items: center;
        justify-content: center;
        &.checked {
          background-color: #3b82f6;
          border-color: #3b82f6;
        }
        .check-mark {
          color: #fff;
          font-size: 14px;
        }
      }
      .remember-text {
        margin-left: 0.5rem;
        font-size: 0.875rem;
        color: #666;
      }
    }
    .login-btn {
      height: 46px;
      width: 100%;
      background-color: #2563eb;
      color: #fff;
      margin-top: 1rem;
      border-radius: 9999px;
      font-size: 1.125rem;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .wx-login-btn {
      background-color: #07c160;
    }
    .register-row {
      margin-top: 1rem;
      text-align: center;
      .gray-text { color: #999; }
      .link-text { color: #3b82f6; margin-left: 5rpx; }
    }
  }
}
</style>
