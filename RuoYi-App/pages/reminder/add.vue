<template>
  <view class="form-container">
    <uni-forms ref="formRef" :model="form" :rules="rules" label-width="90">
      <uni-forms-item label="提醒标题" name="title">
        <uni-easyinput v-model="form.title" placeholder="请输入提醒标题" />
      </uni-forms-item>

      <uni-forms-item label="历法类型" name="calendarType">
        <uni-data-checkbox
          v-model="form.calendarType"
          :localdata="calendarTypes"
          @change="onCalendarTypeChange"
        />
      </uni-forms-item>

      <uni-forms-item label="提醒日期" name="remindDate">
        <!-- 公历日期选择器 -->
        <picker v-if="form.calendarType === '0'" mode="date" :value="solarDate" @change="onSolarDateChange">
          <view class="picker-input" :class="{ 'picker-placeholder': !solarDate }">
            {{ solarDate ? solarDate.substring(5) : '选择日期' }}
          </view>
        </picker>
        <!-- 农历月日选择 -->
        <view v-else-if="form.calendarType === '1'" class="lunar-picker-row">
          <picker mode="selector" :range="lunarMonthLabels" @change="onLunarMonthChange">
            <view class="picker-input picker-half" :class="{ 'picker-placeholder': !lunarMonth }">
              {{ lunarMonth || '选择月份' }}
            </view>
          </picker>
          <picker mode="selector" :range="lunarDayLabels" @change="onLunarDayChange">
            <view class="picker-input picker-half" :class="{ 'picker-placeholder': !lunarDay }">
              {{ lunarDay || '选择日期' }}
            </view>
          </picker>
        </view>
        <!-- 未选择历法类型 -->
        <uni-easyinput v-else v-model="form.remindDate" placeholder="请先选择历法类型" disabled />
      </uni-forms-item>

      <uni-forms-item label="提醒邮箱" name="remindEmail">
        <uni-easyinput v-model="form.remindEmail" placeholder="请输入提醒邮箱" type="email" />
      </uni-forms-item>

      <uni-forms-item label="AI提示词" name="aiPrompt">
        <uni-easyinput v-model="form.aiPrompt" placeholder="请输入AI提示词" type="textarea" />
      </uni-forms-item>

      <uni-forms-item label="状态" name="status">
        <uni-data-checkbox
          v-model="form.status"
          :localdata="statusTypes"
        />
      </uni-forms-item>

      <uni-forms-item label="备注" name="remark">
        <uni-easyinput v-model="form.remark" placeholder="请输入备注" type="textarea" />
      </uni-forms-item>
    </uni-forms>

    <button class="submit-btn" type="primary" @click="submit">提交</button>
  </view>
</template>

<script>
import { addReminder } from '@/api/main/reminder'

export default {
  data() {
    return {
      form: {
        title: '',
        remindDate: '',
        calendarType: '0',
        remindEmail: '',
        aiPrompt: '',
        status: '1',
        remark: ''
      },
      solarDate: '',
      lunarMonth: '',
      lunarDay: '',
      lunarMonthLabels: [
        '正月','二月','三月','四月','五月','六月',
        '七月','八月','九月','十月','冬月','腊月'
      ],
      lunarDayLabels: [
        '初一','初二','初三','初四','初五','初六','初七','初八','初九','初十',
        '十一','十二','十三','十四','十五','十六','十七','十八','十九','二十',
        '廿一','廿二','廿三','廿四','廿五','廿六','廿七','廿八','廿九','三十'
      ],
      calendarTypes: [
        { value: '0', text: '公历' },
        { value: '1', text: '农历' }
      ],
      statusTypes: [
        { value: '0', text: '停用' },
        { value: '1', text: '启用' }
      ],
      rules: {
        title: {
          rules: [{ required: true, errorMessage: '提醒标题不能为空' }]
        },
        remindDate: {
          rules: [{ required: true, errorMessage: '提醒日期不能为空' }]
        }
      }
    }
  },
  methods: {
    onCalendarTypeChange() {
      this.form.remindDate = ''
      this.solarDate = ''
      this.lunarMonth = ''
      this.lunarDay = ''
    },
    onSolarDateChange(e) {
      const date = e.detail.value
      this.solarDate = date
      this.form.remindDate = date.substring(5)
    },
    onLunarMonthChange(e) {
      this.lunarMonth = this.lunarMonthLabels[e.detail.value]
      this.form.remindDate = this.lunarMonth + this.lunarDay
    },
    onLunarDayChange(e) {
      this.lunarDay = this.lunarDayLabels[e.detail.value]
      this.form.remindDate = this.lunarMonth + this.lunarDay
    },
    async submit() {
      try {
        await this.$refs.formRef.validate()
        await addReminder(this.form)
        uni.showToast({ title: '新增成功', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
      } catch (e) {
        console.error('表单验证失败', e)
      }
    }
  }
}
</script>

<style scoped>
.form-container {
  padding: 16px;
  background-color: #fff;
  min-height: 100vh;
}

.submit-btn {
  margin-top: 24px;
}

.picker-input {
  padding: 8px 12px;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  font-size: 14px;
  color: #333;
  background-color: #fff;
  min-height: 22px;
  line-height: 22px;
}

.picker-placeholder {
  color: #c0c0c0;
}

.picker-half {
  width: 46%;
  display: inline-block;
}

.lunar-picker-row {
  display: flex;
  justify-content: space-between;
}
</style>
