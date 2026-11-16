<template>
  <view class="help-container">
    <!-- 客服联系卡片 add by CYQ 2026-11-10 -->
    <view class="contact-card">
      <view class="contact-main">
        <view class="contact-title">还有问题？直接找我们</view>
        <view class="contact-desc">在线客服工作时间 9:00 - 21:00，未回复请稍候或直接来电</view>
      </view>
      <!-- #ifdef MP-WEIXIN -->
      <button class="contact-btn" open-type="contact" session-from="help-page">在线客服</button>
      <!-- #endif -->
      <!-- #ifndef MP-WEIXIN -->
      <button class="contact-btn" @click="handleCall">在线客服</button>
      <!-- #endif -->
      <view class="contact-phone" @click="handleCall">
        <text class="phone-text">☎ 紧急问题请联系 400-000-0000</text>
      </view>
    </view>

    <view v-for="(item, findex) in list" :key="findex" :title="item.title" class="list-title">
      <view class="text-title">
        <view :class="item.icon"></view>{{ item.title }}
      </view>
      <view class="childList">
        <view v-for="(child, zindex) in item.childList" :key="zindex" class="question" hover-class="hover"
          @click="handleText(child)">
          <view class="text-item">{{ child.title }}</view>
          <view class="line" v-if="zindex !== item.childList.length - 1"></view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
  export default {
    data() {
      return {
        list: [{
            icon: 'iconfont icon-github',
            title: 'Caelum 问题',
            childList: [{
              title: 'Caelum 官网地址多少？',
              content: 'https://your-domain.com'
            }]
          },
          {
            icon: 'iconfont icon-help',
            title: '其他问题',
            childList: [{
              title: '如何退出登录？',
              content: '请点击[我的] - [应用设置] - [退出登录]即可退出登录',
            }, {
              title: '如何修改用户头像？',
              content: '请点击[我的] - [选择头像] - [点击提交]即可更换用户头像',
            }, {
              title: '如何修改登录密码？',
              content: '请点击[我的] - [应用设置] - [修改密码]即可修改登录密码',
            }]
          }
        ]
      }
    },
    methods: {
      handleText(item) {
        this.$tab.navigateTo(`/pages/common/textview/index?title=${item.title}&content=${item.content}`)
      },
      // 非小程序端：在线客服降级为拨打紧急联系电话 add by CYQ 2026-11-10
      handleCall() {
        uni.makePhoneCall({
          phoneNumber: '400-000-0000',
          fail: () => {
            uni.setClipboardData({
              data: '400-000-0000',
              success: () => this.$modal.showToast('号码已复制')
            })
          }
        })
      }
    }
  }
</script>

<style lang="scss" scoped>
  page {
    background-color: #f8f8f8;
  }

  .help-container {
    margin-bottom: 100rpx;
    padding: 30rpx;
  }

  /* 客服联系卡片 add by CYQ 2026-11-10 */
  .contact-card {
    background: linear-gradient(135deg, #ffffff 0%, #f6f9ff 100%);
    border-radius: 16rpx;
    padding: 30rpx;
    margin-bottom: 30rpx;
    box-shadow: 0px 0px 10rpx rgba(193, 193, 193, 0.2);
  }

  .contact-title {
    font-size: 30rpx;
    font-weight: bold;
    color: #303133;
  }

  .contact-desc {
    font-size: 24rpx;
    color: #909399;
    margin-top: 10rpx;
    line-height: 1.6;
  }

  .contact-btn {
    margin-top: 24rpx;
    width: 100%;
    height: 76rpx;
    line-height: 76rpx;
    background: #007aff;
    color: #ffffff;
    font-size: 28rpx;
    border-radius: 38rpx;
    border: none;

    &::after {
      border: none;
    }
  }

  .contact-phone {
    margin-top: 20rpx;
    text-align: center;
  }

  .phone-text {
    font-size: 26rpx;
    color: #007aff;
    font-weight: bold;
  }

  .list-title {
    margin-bottom: 30rpx;
  }

  .childList {
    background: #ffffff;
    box-shadow: 0px 0px 10rpx rgba(193, 193, 193, 0.2);
    border-radius: 16rpx;
    margin-top: 10rpx;
  }

  .line {
    width: 100%;
    height: 1rpx;
    background-color: #F5F5F5;
  }

  .text-title {
    color: #303133;
    font-size: 32rpx;
    font-weight: bold;
    margin-left: 10rpx;

    .iconfont {
      font-size: 16px;
      margin-right: 10rpx;
    }
  }

  .text-item {
    font-size: 28rpx;
    padding: 24rpx;
  }

  .question {
    color: #606266;
    font-size: 28rpx;
  }
</style>
