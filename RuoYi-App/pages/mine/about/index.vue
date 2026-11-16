<template>
  <view class="about-container">
    <view class="header-section text-center">
      <image style="width: 150rpx;height: 150rpx;" src="/static/logo200.png" mode="widthFix">
      </image>
      <uni-title type="h2" title="Caelum"></uni-title>
    </view>

    <view class="content-section">
      <view class="menu-list">
        <view class="list-cell list-cell-arrow" @click="checkUpdate">
          <view class="menu-item-box">
            <view>版本信息</view>
            <view class="text-right">v{{version}}</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow">
          <view class="menu-item-box">
            <view>官方邮箱</view>
            <view class="text-right">support@example.com</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow">
          <view class="menu-item-box">
            <view>服务热线</view>
            <view class="text-right">152-8689-1906</view>
          </view>
        </view>
      </view>
    </view>

    <view class="copyright">
      <view>Copyright &copy; 2022 hms All Rights Reserved.</view>
    </view>
  </view>
</template>

<script>
  import { checkUpdate } from '@/utils/updateManager'

  export default {
    data() {
      return {
        version: getApp().globalData.config.appInfo.version
      }
    },
    methods: {
      checkUpdate() {
        checkUpdate({ showNoUpdateTip: true })
      }
    }
  }
</script>

<style lang="scss">
  page {
    background-color: #f8f8f8;
  }

  .copyright {
    margin-top: 50rpx;
    text-align: center;
    line-height: 60rpx;
    color: #999;
  }

  .header-section {
    display: flex;
    padding: 30rpx 0 0;
    flex-direction: column;
    align-items: center;
  }
</style>
