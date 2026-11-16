<template>
  <view class="clockin-page">
    <!-- 当前时间 -->
    <view class="time-section">
      <view class="current-time">{{ currentTime }}</view>
      <view class="current-date">{{ currentDate }}</view>
    </view>

    <!-- 位置信息 -->
    <view class="location-section">
      <view class="location-header">
        <view class="iconfont icon-location location-icon"></view>
        <text class="location-label">当前位置</text>
        <text v-if="locating" class="location-status">定位中...</text>
      </view>
      <view v-if="address" class="location-address">{{ address }}</view>
      <view v-if="locationError" class="location-error">{{ locationError }}</view>
      <view v-if="!address && !locating && !locationError" class="location-placeholder">
        点击下方按钮获取位置
      </view>
    </view>

    <!-- 打卡按钮 -->
    <view class="clockin-action">
      <view
        class="clockin-btn"
        :class="{ 'clockin-btn-success': clockedIn, 'clockin-btn-loading': locating }"
        @click="handleClockIn"
      >
        <view v-if="!clockedIn && !locating" class="btn-content">
          <view class="iconfont icon-check btn-icon"></view>
          <text class="btn-text">打卡</text>
        </view>
        <view v-if="locating" class="btn-content">
          <text class="btn-text">定位中...</text>
        </view>
        <view v-if="clockedIn" class="btn-content">
          <view class="iconfont icon-roundcheckfill btn-icon"></view>
          <text class="btn-text">已打卡</text>
        </view>
      </view>
    </view>

    <!-- 服务端上报状态 -->
    <!-- 授权信息仅记录日志，不在界面显示 -->
    <view v-if="serverMsg" class="server-msg" :class="{ 'server-msg-error': serverMsg.includes('失败') }">
      {{ serverMsg }}
      <text v-if="recordId"> (ID: {{ recordId }})</text>
    </view>

    <!-- 打卡记录 -->
    <view v-if="clockedIn" class="result-section">
      <view class="result-card">
        <view class="result-header">
          <view class="iconfont icon-roundcheckfill result-success-icon"></view>
          <text class="result-title">打卡成功</text>
        </view>
        <view class="result-info">
          <view class="result-row">
            <text class="result-label">打卡时间</text>
            <text class="result-value">{{ clockInTime }}</text>
          </view>
          <view class="result-row">
            <text class="result-label">打卡地点</text>
            <text class="result-value">{{ address || '未知' }}</text>
          </view>
          <view v-if="clockinType" class="result-row">
            <text class="result-label">打卡类型</text>
            <text class="result-value" :class="clockinType === '内勤' ? 'type-office' : 'type-out'">{{ clockinType }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 重新打卡 -->
    <view v-if="clockedIn" class="reset-action">
      <view class="reset-btn" @click="handleReset">
        <text>重新打卡</text>
      </view>
    </view>
  </view>
</template>

<script>
// 高德Web服务API key（逆地理编码）
const AMAP_WEB_KEY = '9073e847b0ab5658da7bd99b6de16f34'
// 高德原生SDK key（定位，已在manifest.json配置）
const AMAP_SDK_KEY = 'REDACTED'

import { clockin as clockinApi, code2session, bindOpenid } from '@/api/system/clockin'
import { getOfficeLocations } from '@/api/system/clockin'
import { reportLocation } from '@/api/system/location'
// #ifdef APP-HARMONY
import '@/uni_modules/harmony-permissions'
// #endif

export default {
  data() {
    return {
      currentTime: '',
      currentDate: '',
      latitude: '',
      longitude: '',
      address: '',
      adcode: '',
      city: '',
      district: '',
      province: '',
      locating: false,
      locationError: '',
      clockedIn: false,
      clockInTime: '',
      recordId: null,
      clockinType: '',
      serverMsg: '',
      openidBound: false,
      wxOpenid: '',
      wxAuthMsg: '',
      subscribeAsked: false,
      timer: null
    }
  },
  onLoad() {
    this.subscribeAsked = uni.getStorageSync('wx_subscribe_asked') === '1'
    this.updateTime()
    this.requestWxAuth()
    this.timer = setInterval(() => this.updateTime(), 1000)
  },
  onUnload() {
    if (this.timer) {
      clearInterval(this.timer)
    }
  },
  methods: {
    updateTime() {
      const now = new Date()
      const hours = String(now.getHours()).padStart(2, '0')
      const minutes = String(now.getMinutes()).padStart(2, '0')
      const seconds = String(now.getSeconds()).padStart(2, '0')
      this.currentTime = `${hours}:${minutes}:${seconds}`
      const year = now.getFullYear()
      const month = String(now.getMonth() + 1).padStart(2, '0')
      const day = String(now.getDate()).padStart(2, '0')
      const weekDays = ['日', '一', '二', '三', '四', '五', '六']
      this.currentDate = `${year}年${month}月${day}日 星期${weekDays[now.getDay()]}`
    },
    async handleClockIn() {
      if (this.clockedIn || this.locating) return
      console.log('[打卡] 点击打卡按钮')
      // 微信小程序：首次进入时索要订阅授权（仅一次）
      if (!this.subscribeAsked) {
        this.requestSubscribe()
      }
      this.locating = true
      this.locationError = ''
      try {
        console.log('[打卡] 开始定位...')
        let res = null
        let retry = false
        try {
          res = await this.getLocationWithTimeout(15000)
        } catch (firstErr) {
          console.log('[打卡] 高精度定位失败，尝试普通定位:', firstErr)
          retry = true
          res = await this.getLocationWithTimeout(20000, false)
        }
        console.log('[打卡] 定位成功 (retry=' + retry + '):', JSON.stringify(res))
        this.latitude = res.latitude.toFixed(6)
        this.longitude = res.longitude.toFixed(6)
        await this.reverseGeocode(res.longitude, res.latitude)
        // 本地判定打卡类型（未启用内勤外勤区分时跳过）
        let clockinType = ''
        try {
          const locRes = await getOfficeLocations()
          console.log('[打卡] officeLocations 原始响应:', JSON.stringify(locRes))
          const enableOfficeType = locRes.data?.enableOfficeType !== false
          const locations = locRes.data?.locations || []
          console.log('[打卡] 启用内勤外勤区分:', enableOfficeType, '办公地点数量:', locations.length)
          if (enableOfficeType) {
            clockinType = '外勤'
            for (const loc of locations) {
              if (loc.latitude && loc.longitude && loc.radius) {
                const dist = this.haversineDistance(
                  Number(this.latitude), Number(this.longitude),
                  Number(loc.latitude), Number(loc.longitude)
                )
                console.log('[打卡] 办公地点:', loc.name,
                  '坐标:', loc.latitude + ',' + loc.longitude,
                  '半径:', loc.radius + 'm',
                  '距离:', dist.toFixed(1) + 'm',
                  dist <= loc.radius ? '✅命中' : '❌未命中')
                if (dist <= loc.radius) {
                  clockinType = '内勤'
                  break
                }
              } else {
                console.log('[打卡] 办公地点数据不完整:', JSON.stringify(loc))
              }
            }
          } else {
            console.log('[打卡] 未启用内勤外勤区分，跳过本地判定')
          }
        } catch (e) {
          console.log('[打卡] 获取办公地点失败，默认外勤:', JSON.stringify(e))
        }
        console.log('[打卡] 本地判定类型:', clockinType)
        // 外勤弹窗确认
        if (clockinType === '外勤') {
          const confirmed = await this.confirmOutOffice()
          if (!confirmed) {
            this.locating = false
            return
          }
        }
        const now = new Date()
        const h = String(now.getHours()).padStart(2, '0')
        const m = String(now.getMinutes()).padStart(2, '0')
        const s = String(now.getSeconds()).padStart(2, '0')
        this.clockInTime = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${h}:${m}:${s}`
        // 上报服务端
        try {
          console.log('[打卡] 上报服务端...')
          const serverRes = await clockinApi({
            latitude: Number(this.latitude),
            longitude: Number(this.longitude),
            address: this.address,
            openid: this.wxOpenid || undefined
          })
          this.recordId = serverRes.data?.id || serverRes.data || serverRes
          this.clockinType = serverRes.data?.clockinType || ''
          this.serverMsg = '上报成功'
          console.log('[打卡] 服务端上报成功, recordId:', this.recordId, 'clockinType:', this.clockinType)
          // 顺带上报当前城市，供天气推送取数（失败不影响打卡）add by Qwen 2026-10-26
          this.reportCurrentCity()
        } catch (serverErr) {
          this.serverMsg = '上报失败: ' + (serverErr?.message || serverErr?.errMsg || '网络错误')
          console.log('[打卡] 服务端上报失败:', serverErr)
        }
        console.log('[打卡] 打卡成功, 时间:', this.clockInTime, '地址:', this.address, '坐标:', this.latitude, this.longitude)
        this.clockedIn = true
      } catch (e) {
        const errMsg = e.errMsg || e.message || e + ''
        console.log('[打卡] 定位失败:', errMsg)
        if (errMsg.includes('timeout') || errMsg.includes('timed out')) {
          this.locationError = '定位超时，请确保GPS已开启并移至开阔区域后重试'
        } else if (errMsg.includes('auth') || errMsg.includes('permission')) {
          this.locationError = '请授权位置权限后再打卡'
        } else {
          this.locationError = '定位失败，请重试'
        }
      } finally {
        this.locating = false
      }
    },
    getLocationWithTimeout(timeout, highAccuracy = true) {
      return new Promise((resolve, reject) => {
        const timer = setTimeout(() => {
          reject(new Error('timeout'))
        }, timeout)
        // #ifdef H5
        // H5 端 AMap JS SDK 仅在挂载 <map> 时加载，打卡页面无地图组件
        // 因此 type:'gcj02' 不生效，浏览器返回 WGS84，需手动转 GCJ-02
        const coordType = 'wgs84'
        // #endif
        // #ifndef H5
        const coordType = 'gcj02'
        // #endif
        uni.getLocation({
          type: coordType,
          isHighAccuracy: highAccuracy,
          highAccuracyExpireTime: highAccuracy ? 5000 : 3000,
          success: (res) => {
            clearTimeout(timer)
            // #ifdef H5
            const gcj = this.wgs84ToGcj02(res.latitude, res.longitude)
            res.latitude = gcj.latitude
            res.longitude = gcj.longitude
            // #endif
            resolve(res)
          },
          fail: (err) => {
            clearTimeout(timer)
            reject(err)
          }
        })
      })
    },
    /**
     * WGS84 坐标系转 GCJ-02 坐标系
     * H5 端浏览器 navigator.geolocation 返回 WGS84，
     * 需手动转换为 GCJ-02 才能与高德地图/办公地点坐标匹配
     * add by CYQ 2026-06-06
     */
    wgs84ToGcj02(wgLat, wgLng) {
      const PI = Math.PI
      const a = 6378245.0
      const ee = 0.00669342162296594323

      function transformLat(x, y) {
        let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320.0 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
      }

      function transformLng(x, y) {
        let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
      }

      const dLat = transformLat(wgLng - 105.0, wgLat - 35.0)
      const dLng = transformLng(wgLng - 105.0, wgLat - 35.0)
      const radLat = wgLat / 180.0 * PI
      let magic = Math.sin(radLat)
      magic = 1 - ee * magic * magic
      const sqrtMagic = Math.sqrt(magic)
      const dLatFinal = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * PI)
      const dLngFinal = (dLng * 180.0) / (a / sqrtMagic * Math.cos(radLat) * PI)

      return {
        latitude: wgLat + dLatFinal,
        longitude: wgLng + dLngFinal
      }
    },
    haversineDistance(lat1, lon1, lat2, lon2) {
      const R = 6371000
      const dLat = (lat2 - lat1) * Math.PI / 180
      const dLon = (lon2 - lon1) * Math.PI / 180
      const a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
        + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180)
        * Math.sin(dLon / 2) * Math.sin(dLon / 2)
      return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    },
    confirmOutOffice() {
      return new Promise((resolve) => {
        uni.showModal({
          title: '外勤打卡确认',
          content: '当前定位不在办公地点范围内，确认为外勤打卡吗？',
          confirmText: '确认外勤',
          cancelText: '取消',
          success: (res) => {
            resolve(res.confirm)
          },
          fail: () => {
            resolve(false)
          }
        })
      })
    },
    async reverseGeocode(lng, lat) {
      try {
        const url = `https://restapi.amap.com/v3/geocode/regeo?key=${AMAP_WEB_KEY}&location=${lng},${lat}&extensions=base`
        console.log('[打卡] 逆地理编码请求:', url)
        // 手动包装 Promise，兼容 APP 原生环境
        const res = await new Promise((resolve, reject) => {
          uni.request({
            url,
            success: (resp) => resolve(resp),
            fail: (err) => reject(err)
          })
        })
        console.log('[打卡] 逆地理编码响应:', JSON.stringify(res))
        if (res.statusCode === 200 && res.data && res.data.status === '1') {
          const regeo = res.data.regeocode
          if (regeo) {
            // 高德返回的字段可能是空数组[]（坐标不在中国境内时），需判断是否为有效字符串
            const safeStr = (val) => (typeof val === 'string' && val.trim()) ? val : ''
            // 记录行政区划，供打卡成功后顺带上报当前城市 add by Qwen 2026-10-26
            this.province = safeStr(regeo.addressComponent?.province)
            this.city = safeStr(regeo.addressComponent?.city) || this.province
            this.district = safeStr(regeo.addressComponent?.district)
            this.adcode = safeStr(regeo.addressComponent?.adcode)
            const formattedAddr = safeStr(regeo.formatted_address)
            if (formattedAddr) {
              this.address = formattedAddr
            } else {
              const province = safeStr(regeo.addressComponent?.province)
              const city = safeStr(regeo.addressComponent?.city)
              const district = safeStr(regeo.addressComponent?.district)
              const township = safeStr(regeo.addressComponent?.township)
              const addr = province + city + district + township
              this.address = addr || `无法解析地址（坐标: ${lat}, ${lng}）`
            }
          } else {
            this.address = '未获取到地址信息'
          }
        } else {
          this.address = '地址解析失败: ' + (res.data?.info || res.data?.status || res.statusCode)
        }
      } catch (e) {
        console.log('[打卡] 逆地理编码异常:', JSON.stringify(e))
        this.address = '地址解析异常'
      }
    },
    // 打卡成功后顺带上报当前城市（天气推送取数用，失败不影响打卡）add by Qwen 2026-10-26
    async reportCurrentCity() {
      if (!this.latitude || !this.longitude) {
        console.log('[城市上报] 无坐标，跳过')
        return
      }
      try {
        // 只传坐标，城市由服务端逆地理解析，避免依赖客户端高德域名白名单
        const res = await reportLocation({
          city: this.city,
          district: this.district,
          adcode: this.adcode,
          latitude: Number(this.latitude),
          longitude: Number(this.longitude),
          source: 'clockin'
        })
        console.log('[城市上报] 打卡页上报成功:', res && res.msg)
      } catch (e) {
        console.log('[城市上报] 打卡页上报失败:', (e && (e.message || e.msg)) || e)
      }
    },
    // 微信小程序：静默获取 openid 并绑定
    async requestWxAuth() {
      // #ifdef MP-WEIXIN
      try {
        const stored = uni.getStorageSync('wx_openid_bound')
        if (stored) {
          this.openidBound = true
          return
        }
        console.log('[微信授权] 开始 wx.login...')
        const loginRes = await new Promise((resolve, reject) => {
          wx.login({ success: resolve, fail: reject })
        })
        console.log('[微信授权] wx.login 结果:', JSON.stringify(loginRes))
        if (!loginRes.code) {
          console.log('[微信授权]wx.login 失败: ' + JSON.stringify(loginRes))
          return
        }
        console.log('[微信授权] code2session 请求中, code=' + loginRes.code.substring(0, 10) + '...')
        const sessionRes = await code2session(loginRes.code)
        console.log('[微信授权] code2session 响应:', JSON.stringify(sessionRes))
        const openid = sessionRes.data && sessionRes.data.openid
        if (!openid) {
          console.log('[微信授权]换取openid失败: ' + JSON.stringify(sessionRes))
          return
        }
        this.wxOpenid = openid
        console.log('[微信授权] bindOpenid 请求中, openid=' + openid)
        await bindOpenid(openid)
        uni.setStorageSync('wx_openid_bound', true)
        this.openidBound = true
        console.log('[微信授权]openid已绑定')
        console.log('[微信授权] openid 绑定成功')
      } catch (e) {
        console.log('[微信授权]授权异常: ' + (e.message || JSON.stringify(e)))
        console.log('[微信授权] 异常:', JSON.stringify(e))
      }
      // #endif
    },
    // 微信小程序：索要订阅消息授权（必须由点击事件触发）
    requestSubscribe() {
      // #ifdef MP-WEIXIN
      console.log('[微信授权] 请求订阅消息授权...')
      const self = this
      wx.requestSubscribeMessage({
        tmplIds: ['REDACTED'],
        success(res) {
          console.log('[微信授权] 订阅消息授权结果:', JSON.stringify(res))
          const tplId = 'REDACTED'
          if (res[tplId] === 'accept') {
            self.subscribeAsked = true
            uni.setStorageSync('wx_subscribe_asked', '1')
            uni.showToast({ title: '授权成功', icon: 'success' })
          }
        },
        fail(err) {
          console.log('[微信授权] 订阅消息授权失败:', JSON.stringify(err))
        }
      })
      // #endif
    },

    handleReset() {
      this.clockedIn = false
      this.clockInTime = ''
      this.address = ''
      this.latitude = ''
      this.longitude = ''
      this.locationError = ''
      this.serverMsg = ''
      this.recordId = null
      this.clockinType = ''
    }
  }
}
</script>

<style lang="scss">
page {
  background-color: #f5f6f7;
}

.clockin-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx 30rpx;
}

.time-section {
  text-align: center;
  margin-bottom: 40rpx;
}

.current-time {
  font-size: 60rpx;
  font-weight: bold;
  color: #333;
}

.current-date {
  font-size: 28rpx;
  color: #888;
  margin-top: 10rpx;
}

.location-section {
  width: 100%;
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 40rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);
}

.location-header {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.location-icon {
  font-size: 36rpx;
  color: #3c96f3;
  margin-right: 10rpx;
}

.location-label {
  font-size: 30rpx;
  color: #333;
  font-weight: bold;
}

.location-status {
  font-size: 26rpx;
  color: #3c96f3;
  margin-left: 16rpx;
}

.location-address {
  font-size: 28rpx;
  color: #333;
  line-height: 1.5;
  margin-bottom: 8rpx;
}

.location-coords {
  font-size: 24rpx;
  color: #999;
}

.location-error {
  font-size: 26rpx;
  color: #e74c3c;
}

.location-placeholder {
  font-size: 26rpx;
  color: #bbb;
}

.clockin-action {
  margin-bottom: 40rpx;
}

.clockin-btn {
  width: 200rpx;
  height: 200rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #3c96f3, #2980b9);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(60, 150, 243, 0.35);
  transition: all 0.3s;
}

.clockin-btn-success {
  background: linear-gradient(135deg, #27ae60, #219a52);
  box-shadow: 0 8rpx 24rpx rgba(39, 174, 96, 0.35);
}

.clockin-btn-loading {
  background: linear-gradient(135deg, #f39c12, #e67e22);
  box-shadow: 0 8rpx 24rpx rgba(243, 156, 18, 0.35);
}

.btn-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.btn-icon {
  font-size: 48rpx;
  color: #fff;
  margin-bottom: 8rpx;
}

.btn-text {
  font-size: 28rpx;
  color: #fff;
  font-weight: bold;
}

.result-section {
  width: 100%;
  margin-bottom: 30rpx;
}

.result-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);
}

.result-header {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
}

.result-success-icon {
  font-size: 40rpx;
  color: #27ae60;
  margin-right: 12rpx;
}

.result-title {
  font-size: 32rpx;
  color: #27ae60;
  font-weight: bold;
}

.result-info {
  padding-left: 8rpx;
}

.result-row {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.result-row:last-child {
  border-bottom: none;
}

.result-label {
  font-size: 26rpx;
  color: #888;
}

.result-value {
  font-size: 26rpx;
  color: #333;
}

.reset-action {
  width: 100%;
}

.reset-btn {
  text-align: center;
  padding: 20rpx;
  background: #fff;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #3c96f3;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);
}

.server-msg {
  width: 100%;
  text-align: center;
  padding: 16rpx 0;
  font-size: 26rpx;
  color: #27ae60;
}

.server-msg-error {
  color: #e74c3c;
}

.type-office {
  color: #27ae60;
  font-weight: bold;
}

.type-out {
  color: #e67e22;
  font-weight: bold;
}
</style>
