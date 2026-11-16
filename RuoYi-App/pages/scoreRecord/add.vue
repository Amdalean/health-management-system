<template>
  <view class="form-container">
    <uni-forms ref="formRef" :model="form" :rules="rules" label-width="100">
      <uni-forms-item label="课程" name="courseId" required>
        <uni-data-select
          v-model="form.courseId"
          :localdata="courseOptions"
          :clear="false"
          placeholder="请选择课程（已选课程）"
        />
        <view v-if="courseOptions.length === 0" class="course-tip">
          暂无可选课程，请先由老师在「课程管理 → 选课」中为你选课
        </view>
      </uni-forms-item>

      <uni-forms-item label="上传视频" name="filePath" required>
        <view class="upload-section">
          <button type="default" @click="chooseAndUploadVideo" class="upload-btn">
            <uni-icons type="videocamera-filled" size="18" color="#409eff"></uni-icons>
            <text>选择视频文件</text>
          </button>
          
          <view v-if="uploading" class="upload-progress">
            <view class="progress-info">
              <uni-icons type="spinner-cycle" size="16" color="#409eff"></uni-icons>
              <text class="upload-text">视频上传中...</text>
            </view>
            <view class="progress-bar">
              <view class="progress-fill" :style="{ width: uploadProgress + '%' }"></view>
            </view>
            <text class="progress-text">{{ uploadProgress }}%</text>
          </view>

          <view v-if="form.filePath" class="video-info">
            <view class="video-item">
              <uni-icons type="checkmarkempty" size="16" color="#67c23a"></uni-icons>
              <text class="video-name">{{ form.fileName || '视频文件' }}</text>
              <text v-if="form.fileSize" class="video-size">({{ formatFileSize(form.fileSize) }})</text>
              <view class="delete-btn" @click="removeVideo">
                <uni-icons type="closeempty" size="16" color="#f56c6c"></uni-icons>
              </view>
            </view>
          </view>

          <text class="upload-tip">支持 mp4, avi, mov, wmv, flv, mkv 格式，最多上传1个视频</text>
        </view>
      </uni-forms-item>

      <uni-forms-item label="分析类型" name="analysisType">
        <uni-data-select
          v-model="form.analysisType"
          :localdata="analysisTypeOptions"
          placeholder="请选择分析类型"
        />
      </uni-forms-item>

      <uni-forms-item label="AI 评分" name="score">
        <uni-easyinput 
          v-model="form.score" 
          type="number" 
          placeholder="AI自动评分(0-100)"
          :disabled="true"
        />
      </uni-forms-item>

      <uni-forms-item label="分析状态" name="status">
        <view class="status-display">
          <view class="status-badge" :class="getStatusClass(form.status)">
            {{ getStatusText(form.status) }}
          </view>
        </view>
      </uni-forms-item>

      <uni-forms-item label="AI 评语" name="feedback">
        <view class="readonly-field">
          <text class="readonly-text">{{ form.feedback || '暂无评语' }}</text>
        </view>
      </uni-forms-item>

      <uni-forms-item label="备注" name="remark">
        <uni-easyinput 
          v-model="form.remark" 
          type="textarea" 
          placeholder="请输入备注内容"
          :maxlength="500"
        />
      </uni-forms-item>
    </uni-forms>

    <view class="action-buttons">
      <button type="primary" @click="submit" :loading="submitting">提交</button>
      <button type="default" @click="goBack" style="margin-top: 12px;">取消</button>
    </view>
  </view>
</template>

<script>
import { getScoreRecord, addScoreRecord, updateScoreRecord, myCourses } from '@/api/main/scoreRecord'
import { getToken } from '@/utils/auth'
import config from '@/config'

export default {
  data() {
    return {
      form: {
        id: null,
        courseId: null,
        filePath: '',
        fileName: '',
        fileSize: null,
        analysisType: '八段锦',
        score: null,
        status: '0',
        feedback: null,
        analysisDetails: null,
        remark: null
      },
      rules: {
        courseId: {
          rules: [{
            required: true,
            errorMessage: '请选择课程'
          }]
        },
        filePath: {
          rules: [{
            required: true,
            errorMessage: '请上传视频文件'
          }]
        }
      },
      uploading: false,
      uploadProgress: 0,
      submitting: false,
      courseOptions: [],
      analysisTypeOptions: [
        { text: '八段锦', value: '八段锦' }
      ]
    }
  },
  onLoad(options) {
    this.loadCourses()
    if (options.id) {
      this.getDetail(options.id)
    }
  },
  methods: {
    // 加载当前登录用户可选课程（与课程建档 / 学生选课联动）
    loadCourses() {
      return myCourses().then(res => {
        const list = res.data || []
        this.courseOptions = list.map(c => ({
          text: c.courseName + (c.semester ? '（' + c.semester + '）' : ''),
          value: c.courseId
        }))
        // 仅有一门可选课程时自动选中，减少操作
        if (!this.form.courseId && this.courseOptions.length === 1) {
          this.form.courseId = this.courseOptions[0].value
        }
      }).catch(() => {
        this.courseOptions = []
      })
    },
    getDetail(id) {
      uni.showLoading({ title: '加载中...' })
      getScoreRecord(id).then(res => {
        this.form = {
          id: res.data.id,
          courseId: res.data.courseId,
          filePath: res.data.filePath,
          fileName: res.data.fileName,
          fileSize: res.data.fileSize,
          analysisType: res.data.analysisType || '八段锦',
          score: res.data.score,
          status: res.data.status,
          feedback: res.data.feedback,
          analysisDetails: res.data.analysisDetails,
          remark: res.data.remark
        }
        uni.hideLoading()
      }).catch(() => {
        uni.hideLoading()
        uni.showToast({ title: '获取详情失败', icon: 'error' })
      })
    },

    // 选择并上传视频
    async chooseAndUploadVideo() {
      if (this.form.filePath) {
        uni.showToast({ title: '请先删除已上传的视频', icon: 'none' })
        return
      }

      try {
        let res

        // #ifdef H5
        // H5端使用 chooseFile
        res = await uni.chooseFile({
          count: 1,
          type: 'file',
          extension: ['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv']
        })
        // #endif

        // #ifdef APP-PLUS
        // APP端使用 chooseVideo
        res = await new Promise((resolve, reject) => {
          uni.chooseVideo({
            sourceType: ['album', 'camera'],
            maxDuration: 600,
            success: (response) => {
              console.log('APP chooseVideo 原始返回:', JSON.stringify(response))
              // 转换返回格式
              resolve({
                tempFiles: [{
                  path: response.tempFilePath,
                  tempFilePath: response.tempFilePath,
                  name: response.name || `video_${Date.now()}.mp4`,
                  size: response.size || 0,
                  duration: response.duration || 0
                }]
              })
            },
            fail: (error) => {
              console.error('APP chooseVideo 失败:', error)
              reject(error)
            }
          })
        })
        // #endif

        // #ifdef MP-WEIXIN
        // 微信小程序端使用 chooseMedia
        res = await uni.chooseMedia({
          count: 1,
          mediaType: ['video'],
          sourceType: ['album', 'camera'],
          maxDuration: 600
        })
        // #endif

        if (res && res.tempFiles && res.tempFiles.length > 0) {
          const tempFile = res.tempFiles[0]
          console.log('选择的文件详情:', JSON.stringify(tempFile))

          // APP端和H5端的字段略有不同，需要兼容
          const fileData = {
            path: tempFile.tempFilePath || tempFile.path,
            name: tempFile.name || `video_${Date.now()}.mp4`,
            size: tempFile.size || 0
          }
          console.log('准备上传的文件数据:', JSON.stringify(fileData))

          await this.uploadVideo(fileData)
        } else {
          console.warn('选择文件返回空:', JSON.stringify(res))
          uni.showToast({ title: '未选择文件', icon: 'none' })
        }
      } catch (error) {
        console.error('选择文件失败:', error)
        if (error.errMsg && !error.errMsg.includes('cancel')) {
          uni.showToast({ title: '选择文件失败: ' + error.errMsg, icon: 'none', duration: 3000 })
        } else if (!error.errMsg.includes('cancel')) {
          uni.showToast({ title: '选择文件失败，请重试', icon: 'none' })
        }
      }
    },

    // 上传视频
    uploadVideo(tempFile) {
      this.uploading = true
      this.uploadProgress = 0

      uni.showLoading({ title: '视频上传中...' })

      const token = getToken()
      const uploadTask = uni.uploadFile({
        url: config.baseUrl + '/common/upload',
        filePath: tempFile.path,
        name: 'file',
        header: {
          'Authorization': 'Bearer ' + token
        },
        timeout: 300000, // 5分钟超时
        success: (uploadRes) => {
          console.log('上传响应:', uploadRes)
          try {
            const result = JSON.parse(uploadRes.data)
            console.log('上传成功，后端返回:', result)
            
            if (result.code === 200) {
              // 获取相对路径
              let url = result.url || result.fileName
              if (!url) {
                throw new Error('上传成功但未返回文件路径')
              }
              
              console.log('上传成功，后端返回完整URL:', result.url)
              if (url.startsWith('http')) {
                // 使用正则表达式提取相对路径
                const match = url.match(/https?:\/\/[^/]+(.*)/)
                url = match ? match[1] : url
                console.log('提取的相对路径:', url)
              }

              // 更新表单数据
              this.form.filePath = url
              this.form.fileName = tempFile.name || 'video_' + Date.now() + '.mp4'
              this.form.fileSize = tempFile.size

              uni.hideLoading()
              uni.showToast({ title: '上传成功', icon: 'success' })
            } else {
              throw new Error(result.msg || '上传失败')
            }
          } catch (error) {
            console.error('解析上传结果失败:', error)
            uni.hideLoading()
            uni.showToast({ title: error.message || '上传失败，请重试', icon: 'none' })
          }
        },
        fail: (error) => {
          console.error('上传失败:', error)
          uni.hideLoading()
          let message = '上传失败'
          if (error.errMsg) {
            if (error.errMsg.includes('timeout')) {
              message = '上传超时，请检查网络连接'
            } else if (error.errMsg.includes('fail')) {
              message = '上传失败，请重试'
            }
          }
          uni.showToast({ title: message, icon: 'none' })
        },
        complete: () => {
          this.uploading = false
          this.uploadProgress = 0
        }
      })

      // 监听上传进度
      uploadTask.onProgressUpdate((res) => {
        this.uploadProgress = res.progress
        console.log('上传进度:', res.progress)
      })
    },

    // 移除视频
    removeVideo() {
      uni.showModal({
        title: '提示',
        content: '确定删除已上传的视频吗？',
        success: (res) => {
          if (res.confirm) {
            this.form.filePath = ''
            this.form.fileName = ''
            this.form.fileSize = null
          }
        }
      })
    },

    // 提交表单
    submit() {
      this.$refs.formRef.validate().then(() => {
        if (!this.form.filePath) {
          uni.showToast({ title: '请上传视频文件', icon: 'none' })
          return
        }

        this.submitting = true

        const data = {
          id: this.form.id,
          courseId: this.form.courseId,
          filePath: this.form.filePath,
          fileName: this.form.fileName,
          fileSize: this.form.fileSize,
          analysisType: this.form.analysisType,
          score: this.form.score,
          status: this.form.status,
          feedback: this.form.feedback,
          analysisDetails: this.form.analysisDetails,
          remark: this.form.remark
        }

        const request = this.form.id ? updateScoreRecord(data) : addScoreRecord(data)

        request.then(() => {
          uni.showToast({ 
            title: this.form.id ? '修改成功' : '新增成功', 
            icon: 'success' 
          })
          setTimeout(() => {
            uni.navigateBack()
          }, 500)
        }).catch(() => {
          uni.showToast({ title: '操作失败', icon: 'none' })
        }).finally(() => {
          this.submitting = false
        })
      }).catch(() => {
        uni.showToast({ title: '请完善表单', icon: 'none' })
      })
    },

    // 返回
    goBack() {
      uni.navigateBack()
    },

    // 格式化文件大小
    formatFileSize(bytes) {
      if (!bytes) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
    },

    // 获取状态样式类
    getStatusClass(status) {
      switch (status) {
        case '0': return 'status-pending'
        case '1': return 'status-analyzing'
        case '2': return 'status-completed'
        case '3': return 'status-failed'
        default: return 'status-pending'
      }
    },

    // 获取状态文本
    getStatusText(status) {
      switch (status) {
        case '0': return '待分析'
        case '1': return '分析中'
        case '2': return '已完成'
        case '3': return '分析失败'
        default: return '未知状态'
      }
    }
  }
}
</script>

<style scoped>
.form-container {
  padding: 16px;
  padding-bottom: 80px;
}

.course-tip {
  width: 100%;
  margin-top: 6px;
  font-size: 12px;
  line-height: 18px;
  color: #e6a23c;
}

.upload-section {
  width: 100%;
}

.upload-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
  background-color: #f0f9ff;
  border: 1px dashed #409eff;
  color: #409eff;
  padding: 12px;
}

.upload-progress {
  margin-bottom: 12px;
}

.progress-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
}

.upload-text {
  font-size: 14px;
  color: #409eff;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background-color: #e5e5e5;
  border-radius: 4px;
  overflow: hidden;
  margin: 8px 0;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(to right, #409eff, #66b1ff);
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: #999;
  text-align: center;
  display: block;
}

.video-info {
  margin-top: 12px;
}

.video-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 6px;
}

.video-name {
  flex: 1;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.video-size {
  font-size: 12px;
  color: #999;
}

.delete-btn {
  padding: 4px;
}

.upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
  display: block;
  line-height: 1.5;
}

.status-display {
  padding: 8px 0;
}

.status-badge {
  display: inline-block;
  font-size: 14px;
  padding: 6px 16px;
  border-radius: 20px;
}

.status-pending {
  background-color: #f4f4f5;
  color: #909399;
}

.status-analyzing {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.status-completed {
  background-color: #e1f3d8;
  color: #67c23a;
}

.status-failed {
  background-color: #fef0f0;
  color: #f56c6c;
}

.readonly-field {
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 6px;
  min-height: 60px;
}

.readonly-text {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.action-buttons {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px;
  background-color: #fff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
}
</style>
