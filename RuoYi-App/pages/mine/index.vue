<template>
  <view class="h-full w-full" :style="{ height: `${windowHeight}px` }">
    <!--顶部个人信息栏-->
    <view class="px-4 pt-4 pb-12 bg-[#3c96f3] text-white">
      <view class="flex padding justify-between">
        <view class="flex items-center">
          <view v-if="!avatar" class="border-2 border-solid border-[#eaeaea] rounded-full w-16 h-16 bg-white">
            <view class="iconfont icon-people text-gray text-[40px]"></view>
          </view>
          <image v-if="avatar" @click="handleToAvatar" :src="avatar"
            class="border-2 border-solid border-[#eaeaea] rounded-full w-16 h-16" mode="widthFix">
          </image>
          <view v-if="!name" @click="handleToLogin" class="text-lg ml-2.5">
            点击登录
          </view>
          <view v-if="name" @click="handleToInfo" class="ml-4">
            <view class="text-lg">
              用户名：{{ name }}
            </view>
          </view>
        </view>
        <view @click="handleToInfo" class="flex items-center">
          <text>个人信息</text>
          <view class="iconfont icon-right"></view>
        </view>
      </view>
    </view>

    <view class="content-section relative top-[-50px]">
      <view class="m-4 py-5 bg-white rounded-md grid grid-cols-4 text-center [&_.icon]:text-[28px]">
        <view @click="handleJiaoLiuQun">
          <view class="iconfont icon-friendfill text-pink-600 icon"></view>
          <view class="my-2 text-xs">交流群</view>
        </view>
        <view @click="handleBuilding">
          <view class="iconfont icon-service text-blue-600 icon"></view>
          <view class="my-2 text-xs">在线客服</view>
        </view>
        <view @click="handleBuilding">
          <view class="iconfont icon-community text-yellow-600 icon"></view>
          <view class="my-2 text-xs">反馈社区</view>
        </view>
        <view @click="handleBuilding">
          <view class="iconfont icon-dianzan text-green-600 icon"></view>
          <view class="my-2 text-xs">点赞我们</view>
        </view>
      </view>

      <view class="menu-list">
        <view class="list-cell list-cell-arrow" @click="handleToEditInfo">
          <view class="menu-item-box">
            <view class="iconfont icon-user menu-icon"></view>
            <view>编辑资料</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow" @click="handleHelp">
          <view class="menu-item-box">
            <view class="iconfont icon-help menu-icon"></view>
            <view>常见问题</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow" @click="handleAbout">
          <view class="menu-item-box">
            <view class="iconfont icon-aixin menu-icon"></view>
            <view>关于我们</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow" @click="handleToSetting">
          <view class="menu-item-box">
            <view class="iconfont icon-setting menu-icon"></view>
            <view>应用设置</view>
          </view>
        </view>
      </view>

    </view>
  </view>
</template>

<script>
import storage from '@/utils/storage'

export default {
  data() {
    return {
      name: this.$store.state.user.name,
      version: getApp().globalData.config.appInfo.version
    }
  },
  computed: {
    avatar() {
      return this.$store.state.user.avatar
    },
    windowHeight() {
      return uni.getSystemInfoSync().windowHeight - 50
    }
  },
  methods: {
    handleToInfo() {
      this.$tab.navigateTo('/pages/mine/info/index')
    },
    handleToEditInfo() {
      this.$tab.navigateTo('/pages/mine/info/edit')
    },
    handleToSetting() {
      this.$tab.navigateTo('/pages/mine/setting/index')
    },
    handleToLogin() {
      this.$tab.reLaunch('/pages/login')
    },
    handleToAvatar() {
      this.$tab.navigateTo('/pages/mine/avatar/index')
    },
    handleLogout() {
      this.$modal.confirm('确定注销并退出系统吗？').then(() => {
        this.$store.dispatch('LogOut').then(() => {
          this.$tab.reLaunch('/pages/index')
        })
      })
    },
    handleHelp() {
      this.$tab.navigateTo('/pages/mine/help/index')
    },
    handleAbout() {
      this.$tab.navigateTo('/pages/mine/about/index')
    },
    handleJiaoLiuQun() {
      this.$modal.showToast('QQ群：①133713780、②146013835')
    },
    handleBuilding() {
      this.$modal.showToast('模块建设中~')
    }
  }
}
</script>

<style lang="scss">
page {
  background-color: #f5f6f7;
}
</style>
