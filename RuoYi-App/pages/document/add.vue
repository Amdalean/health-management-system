<template>
  <view class="form-container">
    <uni-forms ref="form" :model="form" label-width="80">
      <uni-forms-item label="资料名称" name="fileName">
        <uni-easyinput v-model="form.fileName" placeholder="请输入资料名称" />
      </uni-forms-item>
      <uni-forms-item label="描述" name="description">
        <uni-easyinput v-model="form.description" placeholder="请输入描述" />
      </uni-forms-item>
      <uni-forms-item label="上传文件" name="filePath">
        <uni-file-picker 
          v-model="fileList"
          :limit="5"
          @select="onFileSelect"
          @delete="onFileDelete"
        />
      </uni-forms-item>
    </uni-forms>
    <button type="primary" @click="submit">提交</button>
  </view>
</template>

<script>
import { addDocument } from '@/api/main/document'
import upload from '@/utils/upload'

export default {
  data() {
    return {
      form: { fileName: '', description: '', filePath: '' },
      fileList: [] // 用于file-picker显示
    }
  },
  methods: {
    // 文件选择时直接上传
    async onFileSelect(e) {
      const file = e.tempFiles[0]
      if (file && file.file) {
        try {
          uni.showLoading({ title: '上传中...' })
          const uploadRes = await upload({
            url: '/common/upload',
            filePath: file.file.path,
            name: 'file'
          })
          // 只保留相对路径部分
          const url = uploadRes.url.replace(/^https?:\/\/[^/]+/, '')
          this.fileList.push({ url })
          this.form.filePath = this.fileList.map(f => f.url).join(',')
          uni.hideLoading()
          uni.showToast({ title: '文件上传成功', icon: 'success' })
        } catch (error) {
          uni.hideLoading()
          uni.showToast({ title: '文件上传失败', icon: 'none' })
        }
      }
    },
    // 文件删除时
    onFileDelete(e) {
      this.fileList.splice(e.index, 1)
      this.form.filePath = this.fileList.map(f => f.url).join(',')
    },
    // 提交表单
    submit() {
      addDocument(this.form).then(() => {
        uni.showToast({ title: '新增成功', icon: 'success' })
        uni.navigateBack()
      })
    }
  }
}
</script>

<style scoped>
.form-container { padding: 16px; }
</style>