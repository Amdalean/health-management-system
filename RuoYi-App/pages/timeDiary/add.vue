<template>
  <view class="form-container">
    <!-- 表单 -->
    <view class="form-section">
      <view class="form-group">
        <text class="form-label">日期<text class="required">*</text></text>
        <picker mode="date" :value="form.diaryDate" @change="onDateChange">
          <view class="picker-input" :class="{ placeholder: !form.diaryDate }">
            <uni-icons type="calendar" size="16" color="#909399" style="margin-right: 6px;"></uni-icons>
            <text>{{ form.diaryDate || '请选择日期' }}</text>
          </view>
        </picker>
      </view>

      <view class="form-group">
        <text class="form-label">时间段<text class="required">*</text></text>
        <view class="time-range">
          <picker mode="time" :value="form.startTime" @change="onStartTimeChange">
            <view class="time-picker" :class="{ placeholder: !form.startTime }">
              <uni-icons type="clock" size="16" color="#909399" style="margin-right: 6px;"></uni-icons>
              <text>{{ form.startTime || '开始' }}</text>
            </view>
          </picker>
          <text class="time-arrow">→</text>
          <picker mode="time" :value="form.endTime" @change="onEndTimeChange">
            <view class="time-picker" :class="{ placeholder: !form.endTime }">
              <uni-icons type="clock-filled" size="16" color="#909399" style="margin-right: 6px;"></uni-icons>
              <text>{{ form.endTime || '结束' }}</text>
            </view>
          </picker>
        </view>
        <view class="duration-hint" v-if="durationText">
          <text class="duration-hint-text">时长 {{ durationText }}</text>
          <text class="duration-hint-sub">（选择时间段后自动计算）</text>
        </view>
      </view>

      <view class="form-group">
        <text class="form-label">分类<text class="required">*</text></text>
        <view class="type-selector">
          <view
            v-for="opt in categoryOptions"
            :key="opt.value"
            class="type-item"
            :class="{ active: form.category === opt.value }"
            :style="form.category === opt.value ? { backgroundColor: opt.color, borderColor: opt.color } : {}"
            @click="form.category = opt.value"
          >
            <text class="type-emoji">{{ opt.emoji }}</text>
            <text class="type-text" :class="{ active: form.category === opt.value }">{{ opt.label }}</text>
          </view>
        </view>
      </view>

      <view class="form-group">
        <text class="form-label">事项标题<text class="required">*</text></text>
        <uni-easyinput v-model="form.title" placeholder="例如：阅读《深入理解Java虚拟机》" :inputBorder="false" :maxlength="200" />
      </view>

      <view class="form-group">
        <text class="form-label">详细描述</text>
        <uni-easyinput v-model="form.description" type="textarea" placeholder="选填" :maxlength="500" :inputBorder="false" />
      </view>
    </view>

    <!-- 操作按钮 -->
    <view class="action-bar">
      <button class="submit-btn" type="primary" @click="submit" :loading="submitting" :disabled="submitting">
        保存
      </button>
      <button v-if="form.diaryId" class="delete-btn" @click="doDelete" :disabled="submitting">
        删除
      </button>
    </view>
  </view>
</template>

<script>
import { getTimeDiary, addTimeDiary, updateTimeDiary, delTimeDiary, getTimeDiaryTimeline, getDictData } from '@/api/main/timeDiary'

// 分类样式映射（字典提供选项，颜色/emoji 按字典值匹配，新增分类走默认样式）
const CATEGORY_META = {
  '工作': { color: '#5B9BD5', emoji: '💼' },
  '学习': { color: '#70AD47', emoji: '📚' },
  '生活': { color: '#ED7D31', emoji: '🏠' },
  '其他': { color: '#A5A5A5', emoji: '🎮' }
}

// 字典加载失败时的兜底选项，保证表单可用
const FALLBACK_OPTIONS = ['工作', '学习', '生活', '其他'].map(value => ({
  value,
  label: value,
  ...CATEGORY_META[value]
}))

export default {
  data() {
    return {
      form: {
        diaryId: null,
        diaryDate: '',
        startTime: '',
        endTime: '',
        category: '',
        title: '',
        description: ''
      },
      categoryOptions: [],
      submitting: false
    }
  },
  computed: {
    /** 根据开始/结束时间自动计算的时长文案 */
    durationText() {
      if (!this.form.startTime || !this.form.endTime) return ''
      const mins = this.calcDuration()
      if (mins <= 0) return ''
      const h = Math.floor(mins / 60)
      const rest = mins % 60
      if (h > 0 && rest > 0) return `${h}小时${rest}分钟`
      if (h > 0) return `${h}小时`
      return `${rest}分钟`
    }
  },
  onLoad(options) {
    this.loadCategoryDict()
    if (options.id) {
      this.loadDetail(options.id)
    } else {
      // 新增默认今天，并自动带出该日期最后一条记录的结束时间
      const now = new Date()
      this.form.diaryDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
      this.fetchLastEndTime(this.form.diaryDate)
    }
  },
  methods: {
    /** 加载分类字典（time_diary_category） */
    loadCategoryDict() {
      getDictData('time_diary_category').then(res => {
        const list = res.data || []
        if (list.length === 0) {
          this.categoryOptions = FALLBACK_OPTIONS
        } else {
          this.categoryOptions = list.map(item => {
            const meta = CATEGORY_META[item.dictValue] || { color: '#A5A5A5', emoji: '🎮' }
            return {
              value: item.dictValue,
              label: item.dictLabel,
              ...meta
            }
          })
        }
        // 当前分类为空时默认选中第一个选项
        if (!this.form.category && this.categoryOptions.length > 0) {
          this.form.category = this.categoryOptions[0].value
        }
      }).catch(() => {
        this.categoryOptions = FALLBACK_OPTIONS
        if (!this.form.category && this.categoryOptions.length > 0) {
          this.form.category = this.categoryOptions[0].value
        }
      })
    },
    /** 带出指定日期最后一条记录的结束时间作为开始时间 */
    fetchLastEndTime(date) {
      if (!date) return
      getTimeDiaryTimeline({ diaryDate: date }).then(res => {
        const rows = res.data || []
        if (rows.length > 0 && rows[0].endTime) {
          this.form.startTime = rows[0].endTime
        }
      }).catch(() => {
        // 查询失败不阻塞表单填写
      })
    },
    loadDetail(id) {
      uni.showLoading({ title: '加载中...' })
      getTimeDiary(id).then(res => {
        const d = res.data || {}
        this.form = {
          diaryId: d.diaryId,
          diaryDate: d.diaryDate ? String(d.diaryDate).substring(0, 10) : '',
          startTime: d.startTime || '',
          endTime: d.endTime || '',
          category: d.category || '',
          title: d.title || '',
          description: d.description || ''
        }
        uni.hideLoading()
      }).catch(() => {
        uni.hideLoading()
        uni.showToast({ title: '加载详情失败', icon: 'error' })
        setTimeout(() => { uni.navigateBack() }, 1500)
      })
    },
    onDateChange(e) {
      this.form.diaryDate = e.detail.value
      // 新增模式下切换日期时自动带出该日期的最后结束时间
      if (!this.form.diaryId) {
        this.fetchLastEndTime(this.form.diaryDate)
      }
    },
    onStartTimeChange(e) {
      this.form.startTime = e.detail.value
    },
    onEndTimeChange(e) {
      this.form.endTime = e.detail.value
    },
    /** 计算时长（分钟），跨天自动 +24h */
    calcDuration() {
      const [sh, sm] = this.form.startTime.split(':').map(Number)
      const [eh, em] = this.form.endTime.split(':').map(Number)
      let mins = (eh * 60 + em) - (sh * 60 + sm)
      if (mins < 0) {
        mins += 24 * 60 // 跨天
      }
      return mins
    },
    submit() {
      if (!this.form.diaryDate) { uni.showToast({ title: '请选择日期', icon: 'none' }); return }
      if (!this.form.startTime) { uni.showToast({ title: '请选择开始时间', icon: 'none' }); return }
      if (!this.form.endTime) { uni.showToast({ title: '请选择结束时间', icon: 'none' }); return }
      if (!this.form.category) { uni.showToast({ title: '请选择分类', icon: 'none' }); return }
      if (!this.form.title) { uni.showToast({ title: '请输入事项标题', icon: 'none' }); return }

      this.submitting = true

      const data = {
        diaryId: this.form.diaryId,
        diaryDate: this.form.diaryDate,
        startTime: this.form.startTime,
        endTime: this.form.endTime,
        category: this.form.category,
        title: this.form.title,
        description: this.form.description || null,
        durationMin: this.calcDuration()
      }

      const request = this.form.diaryId ? updateTimeDiary(data) : addTimeDiary(data)

      request.then((res) => {
        uni.showToast({ title: this.form.diaryId ? '修改成功' : '新增成功', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 800)
      }).catch((err) => {
        console.error('[timeDiary add] 保存失败:', JSON.stringify(err))
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
            delTimeDiary(this.form.diaryId).then(() => {
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

/* 表单区 */
.form-section {
  padding: 16px;
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

/* 日期/时间选择 */
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

.time-range {
  display: flex;
  align-items: center;
  gap: 10px;
}

.time-picker {
  flex: 1;
  display: flex;
  align-items: center;
  font-size: 15px;
  color: #1D2129;
  background: #F7F8FA;
  border-radius: 8px;
  padding: 10px 12px;
}

.time-picker.placeholder {
  color: #C9CDD4;
}

.time-arrow {
  color: #C9CDD4;
  font-size: 16px;
}

.duration-hint {
  margin-top: 10px;
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.duration-hint-text {
  font-size: 14px;
  font-weight: 700;
  color: #409EFF;
}

.duration-hint-sub {
  font-size: 12px;
  color: #C0C4CC;
}

/* 分类选择器 */
.type-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.type-item {
  display: flex;
  align-items: center;
  gap: 4px;
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

.type-emoji {
  font-size: 13px;
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
