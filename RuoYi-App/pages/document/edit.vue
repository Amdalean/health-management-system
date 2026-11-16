<template>
  <view class="form-container">
    <uni-forms ref="form" :model="form" label-width="80">
      <uni-forms-item label="资料名称" name="fileName">
        <uni-easyinput v-model="form.fileName" placeholder="请输入资料名称" />
      </uni-forms-item>
      <uni-forms-item label="描述" name="description">
        <uni-easyinput v-model="form.description" placeholder="请输入描述" />
      </uni-forms-item>
      <uni-forms-item label="附件" name="filePath">
        <view class="upload-section">
          <button type="default" @click="chooseAndUploadFile" class="upload-btn">
            <uni-icons type="upload" size="18" color="#409eff"></uni-icons>
            <text>上传文件</text>
          </button>
          <view v-if="fileList.length > 0" class="file-list">
            <view v-for="(file, index) in fileList" :key="index" class="file-item">
              <text class="file-name" :title="file.name">{{ file.name }}</text>
              <text v-if="file.status === 'success'" class="file-status success">✓ 已上传</text>
              <text v-else-if="file.status === 'uploading'" class="file-status uploading">上传中...</text>
              <text v-else-if="file.status === 'error'" class="file-status error">✗ 失败</text>
              <view class="delete-btn" @click="removeFile(index)">
                <uni-icons type="closeempty" size="16" color="#f56c6c"></uni-icons>
              </view>
            </view>
          </view>
          <text class="upload-tip">最多上传5个文件</text>
        </view>
      </uni-forms-item>
    </uni-forms>
    <button type="primary" @click="submit">保存</button>
  </view>
</template>

<script>
import { getDocument, updateDocument } from '@/api/main/document'
import upload from '@/utils/upload'
import config from '@/config'
const baseUrl = config.baseUrl

export default {
  data() {
    return {
      form: { id: '', fileName: '', description: '', filePath: '' },
      fileList: [] // {name, url, status}
    }
  },
  onLoad(options) {
    if (options.id) {
      this.loadDocument(options.id)
    }
  },
  methods: {
    // 加载文档信息
    loadDocument(id) {
      getDocument(id).then(res => {
        this.form = res.data || {}
        if (this.form.filePath) {
          // 解析已有文件
          const urls = this.form.filePath.split(',').filter(url => url)
          this.fileList = urls.map((url, index) => ({
            name: this.getFileName(url),
            url: url,
            status: 'success'
          }))
        }
      })
    },

    // 选择并上传文件
    async chooseAndUploadFile() {
      if (this.fileList.length >= 5) {
        uni.showToast({ title: '最多只能上传5个文件', icon: 'none' })
        return
      }

      try {
        let res
        
        // #ifdef H5
        // H5端使用 uni.chooseFile
        res = await uni.chooseFile({
          count: 5 - this.fileList.length,
          type: 'all'
        })
        // #endif
        
        // #ifdef APP-PLUS
        // APP端使用 uni.chooseImage
        res = await new Promise((resolve, reject) => {
          uni.chooseImage({
            count: 5 - this.fileList.length,
            sizeType: ['original', 'compressed'],
            sourceType: ['album'],
            success: (response) => {
              console.log('APP chooseImage 原始返回:', JSON.stringify(response))
              // 转换返回格式
              const tempFiles = response.tempFilePaths.map((path, index) => ({
                path: path,
                tempFilePath: path,
                name: response.tempFiles?.[index]?.name || `file_${Date.now()}_${index}`,
                size: response.tempFiles?.[index]?.size || 0
              }))
              console.log('转换后的tempFiles:', JSON.stringify(tempFiles))
              resolve({ tempFiles })
            },
            fail: (error) => {
              console.error('APP chooseImage 失败:', error)
              reject(error)
            }
          })
        })
        // #endif

        // #ifdef MP-WEIXIN
        // 微信小程序使用 uni.chooseMessageFile
        res = await uni.chooseMessageFile({
          count: 5 - this.fileList.length,
          type: 'all'
        })
        // #endif

        if (res && res.tempFiles && res.tempFiles.length > 0) {
          for (const tempFile of res.tempFiles) {
            await this.uploadSingleFile(tempFile)
          }
        }
      } catch (error) {
        console.error('选择文件失败:', error)
        // 用户取消操作时不显示错误提示
        const errorMsg = error.errMsg || error.message || ''
        if (errorMsg && !errorMsg.includes('cancel') && !errorMsg.includes('选择文件失败')) {
          uni.showToast({ title: '选择文件失败: ' + errorMsg, icon: 'none', duration: 3000 })
        } else if (!errorMsg.includes('cancel')) {
          uni.showToast({ title: '选择文件失败，请重试', icon: 'none', duration: 3000 })
        }
      }
    },

    // 上传单个文件
    async uploadSingleFile(tempFile) {
      const fileName = tempFile.name || tempFile.path || 'file_' + Date.now()
      this.fileList.push({
        name: fileName,
        url: '',
        status: 'uploading'
      })

      try {
        uni.showLoading({ title: '上传中...' })
        const uploadRes = await upload({
          url: '/common/upload',
          filePath: tempFile.path || tempFile.tempFilePath,
          name: 'file'
        })
        let url = uploadRes.url
        if (url.startsWith('http')) {
          // 使用正则表达式提取相对路径
          const match = url.match(/https?:\/\/[^/]+(.*)/)
          url = match ? match[1] : url
        }

        const lastIndex = this.fileList.length - 1
        this.$set(this.fileList, lastIndex, {
          name: fileName,
          url: url,
          status: 'success'
        })

        uni.hideLoading()
        uni.showToast({ title: '上传成功', icon: 'success' })
      } catch (error) {
        const lastIndex = this.fileList.length - 1
        this.$set(this.fileList, lastIndex, {
          name: fileName,
          url: '',
          status: 'error'
        })

        uni.hideLoading()
        console.error('上传失败:', error)
        uni.showToast({ title: '上传失败', icon: 'none' })
      }
    },

    // 移除文件
    removeFile(index) {
      this.fileList.splice(index, 1)
      this.updateFilePath()
    },

    // 更新文件路径
    updateFilePath() {
      this.form.filePath = this.fileList
        .filter(f => f.status === 'success')
        .map(f => f.url)
        .join(',')
    },

    // 获取文件名
    getFileName(url) {
      if (!url) return '未知文件'
      return url.split('/').pop() || url
    },

    // 提交表单
    submit() {
      const hasError = this.fileList.some(f => f.status === 'error')
      if (hasError) {
        uni.showModal({
          title: '提示',
          content: '存在上传失败的文件，是否继续提交？',
          success: (res) => {
            if (res.confirm) {
              this.doSubmit()
            }
          }
        })
      } else {
        this.doSubmit()
      }
    },

    doSubmit() {
      this.updateFilePath()

      if (!this.form.fileName) {
        uni.showToast({ title: '请输入资料名称', icon: 'none' })
        return
      }

      console.log('提交表单数据:', JSON.stringify(this.form))

      updateDocument(this.form).then(() => {
        console.log('保存成功')
        uni.showToast({ title: '保存成功', icon: 'success' })
        uni.navigateBack()
      }).catch((error) => {
        console.error('保存失败，详细错误:', error)
        // request.js 已经显示了错误 toast，这里不再重复显示
      })
    }
  }
}
</script>

<style scoped>
.form-container { 
  padding: 16px; 
}

.upload-section {
  width: 100%;
  max-width: 100%;
  overflow: hidden;
}

.upload-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-bottom: 12px;
  background-color: #f0f9ff;
  border: 1px dashed #409eff;
  color: #409eff;
}

.file-list {
  margin-top: 12px;
  width: 100%;
  overflow: hidden;
}

.file-item {
  display: flex;
  align-items: center;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 8px;
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

.file-name {
  flex: 1;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 60%;
}

.file-status {
  font-size: 12px;
  margin: 0 8px;
}

.file-status.success {
  color: #67c23a;
}

.file-status.uploading {
  color: #e6a23c;
}

.file-status.error {
  color: #f56c6c;
}

.delete-btn {
  padding: 4px;
}

.upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
  display: block;
}
</style>
