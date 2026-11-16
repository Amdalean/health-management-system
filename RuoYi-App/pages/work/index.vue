<template>
  <view class="work-container" :class="themeClass">
    <!-- 轮播图 -->
    <uni-swiper-dot class="uni-swiper-dot-box" :info="data" :current="current" field="content">
      <swiper class="swiper-box" :current="swiperDotIndex" @change="changeSwiper">
        <swiper-item v-for="(item, index) in data" :key="index">
          <view class="swiper-item" @click="clickBannerItem(item)">
            <image style="width: 100%;" :src="item.image" mode="aspectFill" :draggable="false" />
          </view>
        </swiper-item>
      </swiper>
    </uni-swiper-dot>

    <!-- 宫格组件（根据权限动态显示） -->
    <uni-section title="功能入口" type="line"></uni-section>
    <view class="grid-body">
      <uni-grid :column="4" :showBorder="false" @change="changeGrid">
        <uni-grid-item v-for="item in visibleMenus" :key="item.id">
          <view class="grid-item-box" @click="handleMenuClick(item)">
            <uni-icons :type="item.icon" size="30"></uni-icons>
            <text class="text">{{ item.text }}</text>
          </view>
        </uni-grid-item>
      </uni-grid>
      <view v-if="visibleMenus.length === 0" class="empty-tip">
        暂无可用功能，请联系管理员分配权限
      </view>
    </view>
  </view>
</template>

<script>
import themeMixin from '@/mixins/theme'

export default {
  mixins: [themeMixin],
  data() {
    return {
      current: 0,
      swiperDotIndex: 0,
      data: [
      {
        image: '/static/images/banner/banner01.jpg'
      },
      {
        image: '/static/images/banner/banner02.jpg'
      },
      {
        image: '/static/images/banner/banner03.jpg'
      }
      ],
      // 工作台宫格菜单配置（含权限标识）
      // add by CYQ 2026-06-06 说明: 通过 perm 控制菜单可见性，权限在PC端角色管理中分配
      allMenus: [
        { id: 'document',    icon: 'wallet-filled',     text: '资料管理', perm: 'main:document:list',    url: '/pages/document/index' },
        { id: 'reminder',    icon: 'email-filled',      text: '提醒管理', perm: 'main:reminder:list',    url: '/pages/reminder/index' },
        // { id: 'scoreRecord', icon: 'paperplane-filled', text: '成绩分析', perm: 'main:scoreRecord:list', url: '/pages/scoreRecord/index' },
        { id: 'jotdown',     icon: 'compose',           text: '随手记',   perm: 'main:jotdown:list',     url: '/pages/jotdown/index' },
        { id: 'timeDiary',   icon: 'calendar',          text: '时间管理局', perm: 'main:timeDiary:list',    url: '/pages/timeDiary/index' },
        { id: 'summary',     icon: 'wallet-filled',     text: '财务汇总', perm: 'main:summary:list',     url: '/pages/summary/index' },
        { id: 'stock',          icon: 'wallet-filled',    text: '量化交易', perm: 'main:stock:list',        url: '/pages/stock/index' },
        { id: 'tbk',            icon: 'gift-filled',      text: '优惠查询', perm: 'main:tbk:list',          url: '/pages/tbk/index' },
        // add by CYQ 2026-11-04 优惠查询-AI 选品
        { id: 'tbkPick',        icon: 'star-filled',      text: 'AI 选品', perm: 'main:tbk:list',          url: '/pages/tbk/ai-pick' },
        { id: 'clockin',     icon: 'map-filled',        text: '打卡', perm: 'mobile:clockin:list',   url: '/pages/mine/clockin/index' }
      ]
    }
  },
  computed: {
    // 根据用户权限过滤可见菜单
    visibleMenus() {
      return this.allMenus.filter(item => {
        try {
          // 管理员拥有全部权限
          if (this.$auth.hasRole('admin')) return true
          return this.$auth.hasPermi(item.perm)
        } catch (e) {
          // 权限数据未就绪时隐藏菜单
          return false
        }
      })
    }
  },
  methods: {
    clickBannerItem(item) {
      console.info(item)
    },
    changeSwiper(e) {
      this.current = e.detail.current
    },
    changeGrid(e) {
      // this.$modal.showToast('模块建设中~')
    },
    handleMenuClick(item) {
      if (item.url) {
        uni.navigateTo({ url: item.url })
      }
    }
  }
}
</script>

<style lang="scss">
/* #ifndef APP-NVUE */
page {
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  background-color: var(--bg-start, #fff);
  min-height: 100%;
  height: auto;
}

view {
  font-size: 14px;
  line-height: inherit;
}

/* #endif */

.work-container {
  min-height: 100vh;
  background: var(--bg-gradient, var(--bg-start, #fff));
}

.text {
  text-align: center;
  font-size: 26rpx;
  margin-top: 10rpx;
  color: var(--color-text, #333);
}

.grid-body {
  background: var(--card-bg, #fff);
  margin: 16rpx;
  border-radius: var(--radius-card, 24rpx);
  padding: 16rpx 0;
}

.grid-item-box {
  flex: 1;
  /* #ifndef APP-NVUE */
  display: flex;
  /* #endif */
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 15px 0;
}

/* uni-icons 颜色跟随主题 */
.grid-item-box .uni-icons {
  color: var(--color-primary, #333) !important;
}

/* uni-section 标题颜色跟随主题 */
.work-container .uni-section .uni-section-title-text,
.work-container .uni-section .uni-section__title-text,
.work-container .uni-section span,
.work-container .uni-section text {
  color: var(--color-text, #333) !important;
}

.empty-tip {
  text-align: center;
  padding: 40rpx 0;
  font-size: 28rpx;
  color: var(--color-text-secondary, #999);
}

.uni-margin-wrap {
  width: 690rpx;
  width: 100%;

}

.swiper {
  height: 300rpx;
}

.swiper-box {
  height: 150px;
}

.swiper-item {
  /* #ifndef APP-NVUE */
  display: flex;
  /* #endif */
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #fff;
  height: 300rpx;
  line-height: 300rpx;
}

@media screen and (min-width: 500px) {
  .uni-swiper-dot-box {
    width: 400px;
    /* #ifndef APP-NVUE */
    margin: 0 auto;
    /* #endif */
    margin-top: 8px;
  }

  .image {
    width: 100%;
  }
}
</style>
