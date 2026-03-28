<template>
  <view class="normal-login-container w-full">
    <view class="logo-content  pt-[15%] w-full items-center justify-center flex">
      <image class="w-[100rpx] h-[100rpx]" :src="globalConfig.appInfo.logo" mode="widthFix">
      </image>
      <text class="ml-2.5 text-xl">T1大健康登录</text>
    </view>
    <view class="login-form-content">
      <view class="input-item flex items-center">
        <view class="iconfont icon-user icon"></view>
        <input v-model="loginForm.username" class="input" type="text" placeholder="请输入账号" maxlength="30" />
      </view>
      <view class="input-item flex items-center">
        <view class="iconfont icon-password icon"></view>
        <input v-model="loginForm.password" type="password" class="input" placeholder="请输入密码" maxlength="20" />
      </view>
      <view class="input-item flex items-center" style="width: 60%;margin: 0px;" v-if="captchaEnabled">
        <view class="iconfont icon-code icon"></view>
        <input v-model="loginForm.code" type="number" class="input" placeholder="请输入验证码" maxlength="4" />
        <view class="h-[38px] float-right">
          <image :src="codeUrl" @click="getCode" class="w-[200rpx] ml-2.5 absolute h-[38px]"></image>
        </view>
      </view>
      <view class="action-btn">
        <button @click="handleLogin" class="bg-blue-600 text-white mt-4 rounded-full text-lg flex items-center justify-center h-12">登录</button>
      </view>
      <view class="mt-4 text-center" v-if="register">
        <text class="text-gray-400">没有账号？</text>
        <text @click="handleUserRegister" class="text-blue-400">立即注册</text>
      </view>
<!--      <view class="text-center mt-5">
        <text class="text-gray-400">登录即代表同意</text>
        <text @click="handleUserAgrement" class="text-blue-400">《用户协议》</text>
        <text @click="handlePrivacy" class="text-blue-400">《隐私协议》</text>
      </view> -->
    </view>

  </view>
</template>

<script>
import { getCodeImg } from '@/api/login'

export default {
  data() {
    return {
      codeUrl: "",
      captchaEnabled: true,
      // 用户注册开关
      register: true,
      globalConfig: getApp().globalData.config,
      loginForm: {
        username: "",
        password: "",
        code: "",
        uuid: ''
      }
    }
  },
  created() {
    this.getCode()
  },
  methods: {
    // 用户注册
    handleUserRegister() {
      this.$tab.redirectTo(`/pages/register`)
    },
    // 隐私协议
    handlePrivacy() {
      let site = this.globalConfig.appInfo.agreements[0]
      this.$tab.navigateTo(`/pages/common/webview/index?title=${site.title}&url=${site.url}`)
    },
    // 用户协议
    handleUserAgrement() {
      let site = this.globalConfig.appInfo.agreements[1]
      this.$tab.navigateTo(`/pages/common/webview/index?title=${site.title}&url=${site.url}`)
    },
    // 获取图形验证码
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          this.codeUrl = 'data:image/gif;base64,' + res.img
          this.loginForm.uuid = res.uuid
        }
      })
    },
    // 登录方法
    async handleLogin() {
      if (this.loginForm.username === "") {
        this.$modal.msgError("请输入您的账号")
      } else if (this.loginForm.password === "") {
        this.$modal.msgError("请输入您的密码")
      } else if (this.loginForm.code === "" && this.captchaEnabled) {
        this.$modal.msgError("请输入验证码")
      } else {
        this.$modal.loading("登录中，请耐心等待...")
        this.pwdLogin()
      }
    },
    // 密码登录
    async pwdLogin() {
      this.$store.dispatch('Login', this.loginForm).then(() => {
        this.$modal.closeLoading()
        this.loginSuccess()
      }).catch(() => {
        if (this.captchaEnabled) {
          this.getCode()
        }
      })
    },
    // 登录成功后，处理函数
    loginSuccess(result) {
      // 设置用户信息
      this.$store.dispatch('GetInfo').then(res => {
        this.$tab.reLaunch('/pages/index')
      })
    }
  }
}
</script>

<style lang="scss">
page {
  background-color: #ffffff;
}

.normal-login-container {
  .login-form-content {
    @apply my-5 mx-5 mb-auto mt-[15%] text-center;
   
    .input-item {
      @apply my-5 mx-auto h-[45px] rounded-full bg-[#f5f6f7];
      
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

    }

    .action-btn {
      button {
        height: 46px;
        width: 100%;
      }
    }

  }
}
</style>
