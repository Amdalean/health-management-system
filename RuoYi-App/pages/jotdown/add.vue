<template>
  <view class="form-container">
    <!-- AI 图片识别区域 -->
    <view class="ai-section">
      <view class="ai-card" @click="chooseAndRecognize">
        <view class="ai-card-icon" v-if="!aiLoading">
          <uni-icons type="camera-filled" size="28" color="#409EFF"></uni-icons>
        </view>
        <view class="ai-card-loader" v-else>
          <view class="ai-spinner"></view>
        </view>
        <text class="ai-card-title">{{ aiLoading ? 'AI正在识别...' : '拍照识别' }}</text>
        <text class="ai-card-desc">拍摄小票/账单，自动填单</text>
      </view>
    </view>

    <!-- 表单 -->
    <view class="form-section">
      <view class="form-group">
        <text class="form-label">费用说明<text class="required">*</text></text>
        <uni-easyinput v-model="form.description" placeholder="例如：午餐消费" :inputBorder="false" />
      </view>

      <view class="form-group">
        <text class="form-label">金额（元）<text class="required">*</text></text>
        <view class="amount-input-wrap">
          <text class="currency-sign">¥</text>
          <input
            class="amount-input"
            v-model="form.amount"
            type="digit"
            placeholder="0.00"
            :adjust-position="true"
          />
        </view>
      </view>

      <view class="form-group">
        <text class="form-label">日期<text class="required">*</text></text>
        <picker mode="date" :value="form.date" @change="onDateChange">
          <view class="picker-input" :class="{ placeholder: !form.date }">
            <uni-icons type="calendar" size="16" color="#909399" style="margin-right: 6px;"></uni-icons>
            <text>{{ form.date || '请选择日期' }}</text>
          </view>
        </picker>
      </view>

      <view class="form-group">
        <text class="form-label">费用类型<text class="required">*</text></text>
        <view class="type-selector">
          <view
            v-for="opt in expenseTypeOptions"
            :key="opt.value"
            class="type-item"
            :class="{ active: form.expenseType === opt.value }"
            :style="form.expenseType === opt.value ? { backgroundColor: opt.color, borderColor: opt.color } : {}"
            @click="form.expenseType = opt.value"
          >
            <text class="type-text" :class="{ active: form.expenseType === opt.value }">{{ opt.label }}</text>
          </view>
        </view>
      </view>

      <view class="form-group">
        <text class="form-label">备注</text>
        <uni-easyinput v-model="form.remarks" type="textarea" placeholder="选填" :maxlength="200" :inputBorder="false" />
      </view>
    </view>

    <!-- 操作按钮 -->
    <view class="action-bar">
      <button class="submit-btn" type="primary" @click="submit" :loading="submitting" :disabled="submitting">
        保存
      </button>
      <button v-if="form.id" class="delete-btn" @click="doDelete" :disabled="submitting">
        删除
      </button>
    </view>
  </view>
</template>

<script>
import { getJotdown, addJotdown, updateJotdown, delJotdown, aiRecognizeJotdown } from '@/api/main/jotdown'
import { ensurePrivacyAuthorized, isPrivacyScopeError, openPrivacyContract } from '@/utils/privacy'

const EXPENSE_TYPE_OPTIONS = [
  { value: '1', label: '餐饮', color: '#F56C6C' },
  { value: '2', label: '交通', color: '#409EFF' },
  { value: '3', label: '购物', color: '#E6A23C' },
  { value: '4', label: '娱乐', color: '#67C23A' },
  { value: '5', label: '医疗', color: '#909399' },
  { value: '6', label: '住房', color: '#8E71F3' },
  { value: '9', label: '其他', color: '#B0B0B0' }
]

export default {
  data() {
    return {
      form: {
        id: null,
        description: '',
        amount: '',
        date: '',
        expenseType: '',
        remarks: ''
      },
      expenseTypeOptions: EXPENSE_TYPE_OPTIONS,
      aiLoading: false,
      submitting: false,
      fromFlashBall: false
    }
  },
  onLoad(options) {
    if (options.screenshotPath || options.aiData) {
      this.fromFlashBall = true
    }
    if (options.id) {
      this.loadDetail(options.id)
    }
    if (options.screenshotPath) {
      this.aiRecognizeFromPath(decodeURIComponent(options.screenshotPath))
    }
    if (options.aiData) {
      try {
        const raw = JSON.parse(decodeURIComponent(options.aiData))
        // 后端返回 {code, msg, data: {description, amount, date, expenseType, remarks}}
        const aiData = raw.data || raw
        console.log('[add.vue] AI数据:', JSON.stringify(aiData))
        if (aiData.description) this.form.description = aiData.description
        if (aiData.amount != null) this.form.amount = String(aiData.amount)
        if (aiData.date) this.form.date = aiData.date
        if (aiData.expenseType != null) this.form.expenseType = String(aiData.expenseType)
        if (aiData.remarks) this.form.remarks = aiData.remarks
        uni.showToast({ title: 'AI 识别完成，请确认', icon: 'none' })
      } catch (e) {
        console.error('[add.vue] 解析AI数据失败:', e)
      }
    }
  },
  methods: {
    loadDetail(id) {
      uni.showLoading({ title: '加载中...' })
      getJotdown(id).then(res => {
        this.form = {
          id: res.data.id,
          description: res.data.description || '',
          amount: res.data.amount !== null ? String(res.data.amount) : '',
          date: res.data.date || '',
          expenseType: res.data.expenseType !== null ? String(res.data.expenseType) : '',
          remarks: res.data.remarks || ''
        }
        uni.hideLoading()
      }).catch(() => {
        uni.hideLoading()
        uni.showToast({ title: '加载详情失败', icon: 'error' })
        setTimeout(() => { uni.navigateBack() }, 1500)
      })
    },

    async aiRecognizeFromPath(filePath) {
      uni.showLoading({ title: '识别中...', mask: true })
      try {
        const res = await aiRecognizeJotdown(filePath)
        console.log('[add.vue] 截屏识图结果:', JSON.stringify(res))
        const aiData = res.data || res
        if (aiData.description) this.form.description = aiData.description
        if (aiData.amount != null) this.form.amount = String(aiData.amount)
        if (aiData.date) this.form.date = aiData.date
        if (aiData.expenseType != null) this.form.expenseType = String(aiData.expenseType)
        if (aiData.remarks) this.form.remarks = aiData.remarks
        uni.hideLoading()
        uni.showToast({ title: 'AI 识别完成，请确认', icon: 'none' })
      } catch (e) {
        uni.hideLoading()
        console.error('[add.vue] 截屏识图失败:', e)
        uni.showToast({ title: '识别失败', icon: 'none' })
      }
    },

    async chooseAndRecognize() {
      // 微信小程序：chooseImage 属隐私接口，调用前需完成隐私授权
      const privacy = await ensurePrivacyAuthorized()
      if (!privacy.ok) {
        if (isPrivacyScopeError(privacy.err)) {
          this.showPrivacyScopeTip()
        } else {
          uni.showToast({ title: '需先同意隐私协议才能使用相册', icon: 'none' })
        }
        return
      }

      try {
        const chooseRes = await new Promise((resolve, reject) => {
          uni.chooseImage({
            count: 1,
            sizeType: ['compressed'],
            sourceType: ['album', 'camera'],
            success: resolve,
            fail: reject
          })
        })

        const tempFilePath = chooseRes.tempFilePaths[0]
        if (!tempFilePath) return

        this.aiLoading = true
        uni.showLoading({ title: 'AI识别中...', mask: true })

        const aiRes = await aiRecognizeJotdown(tempFilePath)
        uni.hideLoading()
        this.aiLoading = false

        const data = aiRes.data
        if (data) {
          if (data.description) this.form.description = data.description
          if (data.amount !== null && data.amount !== undefined) this.form.amount = String(data.amount)
          if (data.date) this.form.date = data.date
          if (data.expenseType !== null && data.expenseType !== undefined) this.form.expenseType = String(data.expenseType)
          if (data.remarks) this.form.remarks = data.remarks
          uni.showToast({ title: '已识别，请核对后保存', icon: 'success' })
        } else {
          uni.showToast({ title: 'AI识别无结果', icon: 'none' })
        }
      } catch (err) {
        uni.hideLoading()
        this.aiLoading = false
        if (err && err.errMsg && err.errMsg.includes('cancel')) {
          return
        }
        console.error('AI识别失败:', err)
        if (isPrivacyScopeError(err)) {
          this.showPrivacyScopeTip()
          return
        }
        uni.showToast({ title: '识别失败，请重试', icon: 'none' })
      }
    },

    /**
     * errno 112：小程序后台《用户隐私保护指引》未声明「收集你选中的照片或视频信息」，
     * 该问题必须在微信公众平台后台补充声明，代码无法绕过。
     */
    showPrivacyScopeTip() {
      uni.showModal({
        title: '相册功能未开放',
        content: '小程序后台《用户隐私保护指引》未声明「收集你选中的照片或视频信息」。请到「微信公众平台 → 设置 → 服务内容声明 → 用户隐私保护指引」勾选该项并提交审核，通过后即可拍照/选图。',
        confirmText: '查看协议',
        cancelText: '知道了',
        success: (res) => {
          if (res.confirm) openPrivacyContract()
        }
      })
    },

    onDateChange(e) {
      this.form.date = e.detail.value
    },

    submit() {
      if (!this.form.description) { uni.showToast({ title: '请输入费用说明', icon: 'none' }); return }
      if (!this.form.amount) { uni.showToast({ title: '请输入金额', icon: 'none' }); return }
      if (!this.form.date) { uni.showToast({ title: '请选择日期', icon: 'none' }); return }
      if (!this.form.expenseType) { uni.showToast({ title: '请选择费用类型', icon: 'none' }); return }

      this.submitting = true

      const data = {
        id: this.form.id,
        description: this.form.description,
        amount: parseFloat(this.form.amount),
        date: this.form.date,
        expenseType: parseInt(this.form.expenseType),
        remarks: this.form.remarks || null
      }

      const request = this.form.id ? updateJotdown(data) : addJotdown(data)

      request.then((res) => {
        console.log('[add.vue] 保存成功:', JSON.stringify(res))
        uni.showToast({ title: this.form.id ? '修改成功' : '新增成功', icon: 'success' })
        setTimeout(() => {
          if (this.fromFlashBall) {
            uni.redirectTo({ url: '/pages/jotdown/index' })
          } else {
            uni.navigateBack()
          }
        }, 800)
      }).catch((err) => {
        console.error('[add.vue] 保存失败:', err)
        console.error('[add.vue] errData:', err?.data)
        console.error('[add.vue] errMsg:', err?.msg || err?.message || err?.errMsg)
        console.error('[add.vue] statusCode:', err?.statusCode)
        uni.showToast({ title: '操作失败', icon: 'none' })
      }).finally(() => {
        this.submitting = false
      })
    },
    doDelete() {
      uni.showModal({
        title: '确认删除',
        content: '确定删除这条记录吗？',
        confirmColor: '#F56C6C',
        success: (res) => {
          if (res.confirm) {
            this.submitting = true
            delJotdown(this.form.id).then(() => {
              uni.showToast({ title: '已删除', icon: 'success' })
              setTimeout(() => { uni.navigateBack() }, 800)
            }).catch(() => {
              uni.showToast({ title: '删除失败', icon: 'none' })
            }).finally(() => {
              this.submitting = false
            })
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.form-container {
  min-height: 100vh;
  background-color: #F8F9FB;
  padding-bottom: 100px;
}

/* AI识别区 */
.ai-section {
  padding: 16px;
}

.ai-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px 16px;
  text-align: center;
  border: 1.5px dashed #D0D5DD;
  transition: border-color 0.2s, background 0.2s;
}

.ai-card:active {
  border-color: #409EFF;
  background: #F0F7FF;
}

.ai-card-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #ECF5FF;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 10px;
}

.ai-card-loader {
  width: 56px;
  height: 56px;
  margin: 0 auto 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-spinner {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 3px solid #E8E8E8;
  border-top-color: #409EFF;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.ai-card-title {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: #1D2129;
  margin-bottom: 4px;
}

.ai-card-desc {
  display: block;
  font-size: 12px;
  color: #909399;
}

/* 表单区 */
.form-section {
  padding: 0 16px;
}

.form-group {
  background: #fff;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 10px;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #86909C;
  margin-bottom: 8px;
}

.required {
  color: #F53F3F;
  margin-left: 2px;
}

/* 金额输入 */
.amount-input-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
}

.currency-sign {
  font-size: 22px;
  font-weight: 700;
  color: #1D2129;
}

.amount-input {
  flex: 1;
  font-size: 22px;
  font-weight: 700;
  color: #1D2129;
  border: none;
  outline: none;
  min-height: 32px;
}

.amount-input::placeholder {
  color: #C9CDD4;
  font-weight: 400;
}

/* 日期选择 */
.picker-input {
  display: flex;
  align-items: center;
  font-size: 15px;
  color: #1D2129;
  padding: 2px 0;
}

.picker-input.placeholder {
  color: #C9CDD4;
}

/* 费用类型选择器 */
.type-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.type-item {
  padding: 6px 14px;
  border-radius: 16px;
  border: 1px solid #E5E6EB;
  background: #F7F8FA;
  transition: all 0.2s;
}

.type-item:active {
  opacity: 0.8;
}

.type-item.active {
  border-color: transparent;
}

.type-text {
  font-size: 13px;
  color: #4E5969;
}

.type-text.active {
  color: #fff;
}

/* 底部按钮 */
.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 12px 16px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -1px 4px rgba(0, 0, 0, 0.04);
}

.submit-btn {
  flex: 1;
  height: 46px;
  border-radius: 23px;
  background: #409EFF;
  color: #fff;
  font-size: 16px;
  font-weight: 500;
  border: none;
}

.submit-btn[disabled] {
  opacity: 0.6;
}

.delete-btn {
  width: 80px;
  height: 46px;
  border-radius: 23px;
  background: #fff;
  color: #F56C6C;
  font-size: 16px;
  font-weight: 500;
  border: 1px solid #F56C6C;
  margin-left: 12px;
}

.delete-btn[disabled] {
  opacity: 0.6;
}
</style>
