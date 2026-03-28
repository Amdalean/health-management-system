<template>
    <view class="form-container">
      <uni-forms ref="form" :model="form" label-width="80">
        <uni-forms-item label="资料名称" name="fileName">
          <uni-easyinput v-model="form.fileName" placeholder="请输入资料名称" />
        </uni-forms-item>
        <uni-forms-item label="描述" name="description">
          <uni-easyinput v-model="form.description" placeholder="请输入描述" />
        </uni-forms-item>
      </uni-forms>
      <uni-file-picker
        ref="filePicker"
        v-model="fileList"
        :limit="5"
        :show-upload-btn="false"
        :show-file-list="false"
        @delete="onFileDelete"
        :file-url-formatter="fileUrlFormatter"
      />
      <button type="default" @click="openFileDialog" style="margin-bottom: 12px;">上传</button>
      <view v-if="fileList.length" class="file-list-card">
        <text class="file-list-title">附件列表</text>
        <view v-for="(file, idx) in fileList" :key="file.url || file.name" class="file-list-item">
          <view class="file-icon-wrap">
            <text class="file-icon">📎</text>
          </view>
          <view class="file-info">
            <text class="file-name">{{ getFileName(file.url || file.name) }}</text>
            <text class="file-size" v-if="file.file && file.file.size">({{ formatSize(file.file.size) }})</text>
          </view>
          <button class="file-delete-btn" type="default" size="mini" @click="onFileDelete({ index: idx })">删除</button>
        </view>
      </view>
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
        fileList: []
      }
    },
    onLoad(options) {
      if (options.id) {
        getDocument(options.id).then(res => {
          this.form = res.data || {}
          if (this.form.filePath) {
            this.fileList = this.form.filePath.split(',').map(url => ({ url }))
          } else {
            this.fileList = []
          }
        })
      }
    },
    methods: {
      // 打开文件选择窗口
      openFileDialog() {
        this.$refs.filePicker.choose()
      },
      // 批量上传所有未上传的文件
      async uploadFiles() {
        let changed = false
        for (let i = 0; i < this.fileList.length; i++) {
          const file = this.fileList[i]
          // 只上传本地新选的文件（无url，有file对象）
          if (!file.url && file.file) {
            try {
              uni.showLoading({ title: '上传中...' })
              const uploadRes = await upload({
                url: '/common/upload',
                filePath: file.file.path,
                name: 'file'
              })
              // 只保留相对路径部分
              const url = uploadRes.url.replace(/^https?:\/\/[^/]+/, '')
              this.$set(this.fileList, i, { url })
              changed = true
            } catch (error) {
              uni.showToast({ title: '文件上传失败', icon: 'none' })
            } finally {
              uni.hideLoading()
            }
          }
        }
        if (changed) {
          this.form.filePath = this.fileList.map(f => f.url).join(',')
          uni.showToast({ title: '上传成功', icon: 'success' })
        } else {
          uni.showToast({ title: '无新文件需要上传', icon: 'none' })
        }
      },
      // 文件删除时
      onFileDelete(e) {
        this.fileList.splice(e.index, 1)
        this.form.filePath = this.fileList.map(f => f.url).join(',')
      },
      // 附件url拼接baseUrl
      fileUrlFormatter(file) {
        if (!file.url) return ''
        if (file.url.startsWith('http')) return file.url
        return baseUrl + file.url
      },
      // 获取文件名
      getFileName(url) {
        if (!url) return ''
        return url.split('/').pop()
      },
      // 格式化文件大小
      formatSize(size) {
        if (!size) return ''
        if (size < 1024) return size + 'B'
        if (size < 1024 * 1024) return (size / 1024).toFixed(1) + 'KB'
        return (size / 1024 / 1024).toFixed(1) + 'MB'
      },
      submit() {
        this.form.filePath = this.fileList.map(f => f.url).join(',')
        updateDocument(this.form).then(() => {
          uni.showToast({ title: '保存成功', icon: 'success' })
          uni.navigateBack()
        })
      }
    }
  }
  </script>
  
  <style scoped>
  .form-container { padding: 16px; }
  .file-list-card {
    background: #fff;
    border-radius: 10px;
    padding: 14px 18px;
    margin: 16px 0;
    box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  }
  .file-list-title {
    font-weight: bold;
    color: #222;
    margin-bottom: 12px;
    display: block;
    font-size: 16px;
  }
  .file-list-item {
    display: flex;
    align-items: center;
    padding: 10px 0;
    border-bottom: 1px solid #f0f0f0;
  }
  .file-list-item:last-child {
    border-bottom: none;
  }
  .file-icon-wrap {
    width: 32px;
    height: 32px;
    background: #e6f0ff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 12px;
  }
  .file-icon {
    font-size: 18px;
    color: #409eff;
  }
  .file-info {
    flex: 1;
    display: flex;
    flex-direction: column;
  }
  .file-name {
    color: #333;
    font-size: 15px;
    word-break: break-all;
  }
  .file-size {
    color: #999;
    font-size: 12px;
    margin-top: 2px;
  }
  .file-delete-btn {
    margin-left: 10px;
    background: #f56c6c;
    color: #fff;
    border-radius: 6px;
    padding: 0 10px;
    font-size: 12px;
    height: 26px;
    line-height: 26px;
  }
  </style>
  