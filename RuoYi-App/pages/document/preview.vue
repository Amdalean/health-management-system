<template>
    <view class="preview-container">
      <view v-if="fileType === 'image'">
        <image :src="filePath" mode="widthFix" style="max-width:100%" />
      </view>
      <view v-else-if="fileType === 'pdf'">
        <web-view :src="filePath" />
      </view>
      <view v-else>
        <text>暂不支持该类型预览</text>
      </view>
    </view>
  </template>
  
  <script>
  import { getDocument } from '@/api/main/document'
  export default {
    data() {
      return {
        filePath: '',
        fileType: ''
      }
    },
    onLoad(options) {
      if (options.id) {
        getDocument(options.id).then(res => {
          const doc = res.data || {}
          this.filePath = doc.filePath
          // 简单判断类型
          if (doc.filePath && /\.(jpg|jpeg|png|gif)$/i.test(doc.filePath)) {
            this.fileType = 'image'
          } else if (doc.filePath && /\.pdf$/i.test(doc.filePath)) {
            this.fileType = 'pdf'
          } else {
            this.fileType = ''
          }
        })
      }
    }
  }
  </script>
  
  <style scoped>
  .preview-container { padding: 16px; }
  </style>
  