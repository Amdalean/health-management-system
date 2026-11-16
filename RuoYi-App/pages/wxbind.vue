<template>
  <view class="bind-container">
    <view class="logo-area">
      <image style="width: 100rpx; height: 100rpx;" :src="globalConfig.appInfo.logo" mode="widthFix"></image>
      <text class="app-name">绑定系统账号</text>
      <text class="tip-text">该微信尚未绑定账号，绑定后下次可一键登录</text>
    </view>
    <view class="form-area">
      <view class="input-row">
        <view class="iconfont icon-user icon"></view>
        <input v-model="bindForm.username" class="input" type="text" placeholder="请输入账号" maxlength="30" />
      </view>
      <view class="input-row">
        <view class="iconfont icon-password icon"></view>
        <input v-model="bindForm.password" password class="input" placeholder="请输入密码" maxlength="20" />
      </view>
      <button @click="handleBind" class="login-btn">绑定并登录</button>
    </view>
  </view>
</template>

<script>
import { getToken } from '@/utils/auth'

export default {
  data() {
    return {
      globalConfig: getApp().globalData.config,
      bindForm: {
        openid: '',
        username: '',
        password: ''
      }
    }
  },
  onLoad(options) {
    // 已登录则直接回首页
    if (getToken()) {
      this.$tab.reLaunch('/pages/index')
      return
    }
    if (options && options.openid) {
      this.bindForm.openid = options.openid
    }
  },
  methods: {
    handleBind() {
      if (!this.bindForm.openid) {
        this.$modal.msgError('缺少微信标识，请返回重试')
      } else if (this.bindForm.username === '') {
        this.$modal.msgError('请输入您的账号')
      } else if (this.bindForm.password === '') {
        this.$modal.msgError('请输入您的密码')
      } else {
        this.$modal.loading('绑定中，请耐心等待...')
        this.$store.dispatch('WxBindLogin', this.bindForm).then(() => {
          this.$modal.closeLoading()
          this.$store.dispatch('GetInfo').then(() => {
            this.$tab.reLaunch('/pages/index')
          })
        }).catch(() => {
          this.$modal.closeLoading()
        })
      }
    }
  }
}
</script>

<style lang="scss">
page {
  background-color: #ffffff;
}
.bind-container {
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
    .tip-text {
      margin-top: 10rpx;
      font-size: 26rpx;
      color: #999;
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
    }
    .login-btn {
      height: 46px;
      width: 100%;
      background-color: #07c160;
      color: #fff;
      margin-top: 1rem;
      border-radius: 9999px;
      font-size: 1.125rem;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }
}
</style>
