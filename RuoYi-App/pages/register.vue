<template>
  <view class="reg-container">
    <view class="logo-area">
      <image style="width: 100rpx; height: 100rpx;" :src="globalConfig.appInfo.logo" mode="widthFix"></image>
      <text class="app-title">Caelum 注册</text>
    </view>
    <view class="form-area">
      <view class="input-row">
        <view class="iconfont icon-user icon"></view>
        <input v-model="registerForm.username" class="input" type="text" placeholder="请输入账号" maxlength="30" />
      </view>
      <view class="input-row">
        <view class="iconfont icon-password icon"></view>
        <input v-model="registerForm.password" password class="input" placeholder="请输入密码" maxlength="20" />
      </view>
      <view class="input-row">
        <view class="iconfont icon-password icon"></view>
        <input v-model="registerForm.confirmPassword" password class="input" placeholder="请输入重复密码" maxlength="20" />
      </view>
      <view class="input-row captcha-row" v-if="captchaEnabled">
        <view class="iconfont icon-code icon"></view>
        <input v-model="registerForm.code" type="number" class="input" placeholder="请输入验证码" maxlength="4" />
        <image :src="codeUrl" @click="getCode" class="captcha-img"></image>
      </view>
      <button @click="handleRegister()" class="reg-btn">注册</button>
    </view>
    <view class="login-link" @click="handleUserLogin">使用已有账号登录</view>
  </view>
</template>

<script>
  import { getCodeImg, register } from '@/api/login'

  export default {
    data() {
      return {
        codeUrl: "",
        captchaEnabled: true,
        globalConfig: getApp().globalData.config,
        registerForm: {
          username: "",
          password: "",
          confirmPassword: "",
          code: "",
          uuid: ''
        }
      }
    },
    created() {
      this.getCode()
    },
    methods: {
      handleUserLogin() {
        this.$tab.navigateTo(`/pages/login`)
      },
      getCode() {
        getCodeImg().then(res => {
          this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
          if (this.captchaEnabled) {
            this.codeUrl = 'data:image/gif;base64,' + res.img
            this.registerForm.uuid = res.uuid
          }
        })
      },
      async handleRegister() {
        if (this.registerForm.username === "") {
          this.$modal.msgError("请输入您的账号")
        } else if (this.registerForm.password === "") {
          this.$modal.msgError("请输入您的密码")
        } else if (this.registerForm.confirmPassword === "") {
          this.$modal.msgError("请再次输入您的密码")
        } else if (this.registerForm.password !== this.registerForm.confirmPassword) {
          this.$modal.msgError("两次输入的密码不一致")
        } else if (this.registerForm.code === "" && this.captchaEnabled) {
          this.$modal.msgError("请输入验证码")
        } else {
          this.$modal.loading("注册中，请耐心等待...")
          this.doRegister()
        }
      },
      async doRegister() {
        register(this.registerForm).then(res => {
          this.$modal.closeLoading()
          uni.showModal({
            title: "系统提示",
            content: "恭喜你，您的账号 " + this.registerForm.username + " 注册成功！",
            success: function (res) {
              if (res.confirm) {
                uni.redirectTo({ url: `/pages/login` });
              }
            }
          })
        }).catch(() => {
          if (this.captchaEnabled) {
            this.getCode()
          }
        })
      },
      registerSuccess(result) {
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
.reg-container {
  width: 100%;
  .logo-area {
    padding-top: 15%;
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    .app-title {
      margin-top: 10rpx;
      font-size: 21px;
    }
  }
  .form-area {
    margin: 20rpx auto;
    padding-top: 15%;
    width: 80%;
    text-align: center;
    .input-row {
      display: flex;
      align-items: center;
      margin: 20rpx auto;
      background-color: #f5f6f7;
      height: 45px;
      border-radius: 20px;
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
        height: 38px;
        width: 200rpx;
      }
    }
    .captcha-row {
      width: 100%;
    }
    .reg-btn {
      margin-top: 40rpx;
      height: 45px;
      background-color: #2563eb;
      color: #fff;
      border-radius: 9999px;
      font-size: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }
  .login-link {
    text-align: center;
    margin-top: 20rpx;
    color: #3b82f6;
    font-size: 14px;
  }
}
</style>
