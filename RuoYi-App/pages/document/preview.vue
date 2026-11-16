<template>
    <view class="preview-container">
      <view v-if="fileType === 'image'">
        <image :src="filePath" mode="widthFix" style="max-width:100%" />
      </view>
      <view v-else-if="fileType === 'pdf'">
        <view v-if="pdfLoading" class="pdf-loading">
          <uni-icons type="spinner-cycle" size="40" color="#007aff"></uni-icons>
          <text class="loading-text">正在打开文档...</text>
        </view>
        <view v-else class="pdf-result">
          <uni-icons :type="pdfSuccess ? 'checkmarkempty' : 'closeempty'" size="60" :color="pdfSuccess ? '#07c160' : '#f56c6c'"></uni-icons>
          <text class="result-title">{{ pdfSuccess ? '文档已打开' : '打开失败' }}</text>
          <text v-if="!pdfSuccess" class="result-desc">{{ pdfErrorMsg }}</text>
          <button v-if="!pdfSuccess" type="primary" size="mini" @click="openPdf">重新打开</button>
        </view>
      </view>
      <view v-else-if="fileType === 'text'">
        <scroll-view scroll-y class="text-preview">
          <text class="text-content">{{ textContent }}</text>
        </scroll-view>
      </view>
      <view v-else-if="!filePath">
        <view class="empty-preview">
          <uni-icons type="info" size="60" color="#ccc"></uni-icons>
          <text class="empty-title">暂无附件</text>
          <text class="empty-desc">该资料没有上传附件</text>
        </view>
      </view>
      <view v-else>
        <view class="empty-preview">
          <uni-icons type="info" size="60" color="#ccc"></uni-icons>
          <text class="empty-title">暂不支持预览</text>
          <text class="empty-desc">该文件类型暂不支持预览</text>
        </view>
      </view>
    </view>
  </template>

  <script>
  import { getDocument } from '@/api/main/document'
  import config from '@/config'
  const baseUrl = config.baseUrl

  export default {
    data() {
      return {
        filePath: '',
        fileType: '',
        textContent: '',
        fileName: '',
        pdfLoading: false,
        pdfSuccess: false,
        pdfErrorMsg: ''
      }
    },
    onLoad(options) {
      if (options.id) {
        getDocument(options.id).then(res => {
          const doc = res.data || {}
          this.filePath = this.getFullUrl(doc.filePath)
          this.fileName = doc.fileName
          // 简单判断类型
          if (doc.filePath && /\.(jpg|jpeg|png|gif)$/i.test(doc.filePath)) {
            this.fileType = 'image'
          } else if (doc.filePath && /\.pdf$/i.test(doc.filePath)) {
            this.fileType = 'pdf'
            this.openPdf()
          } else if (doc.filePath && /\.(txt|md|log|json|xml|html|css|js|java|py|c|cpp|h|sql|yml|yaml|csv|ini|conf|config)$/i.test(doc.filePath)) {
            this.fileType = 'text'
            this.loadTextContent(doc.filePath)
          } else if (!doc.filePath) {
            // 没有文件路径（可能是文件夹节点）
            this.fileType = ''
          } else {
            this.fileType = ''
          }
        })
      }
    },
    methods: {
      /** 将相对路径转为完整 HTTP URL */
      getFullUrl(path) {
        if (!path) return ''
        // 已经是完整 HTTP URL，直接返回
        if (path.startsWith('http://') || path.startsWith('https://')) {
          return path
        }
        // 确保路径以 / 开头
        if (!path.startsWith('/')) {
          path = '/' + path
        }
        return baseUrl + path
      },
      /** 打开 PDF 文件 */
      openPdf() {
        const url = this.filePath // 已经是完整 HTTP URL
        // #ifdef H5
        // H5 平台直接在新标签页打开
        window.open(url, '_blank')
        this.pdfSuccess = true
        this.pdfLoading = false
        // #endif
        // #ifdef APP-PLUS
        // 原生平台：先下载再用系统文档查看器打开
        this.pdfLoading = true
        this.pdfSuccess = false
        this.pdfErrorMsg = ''
        uni.downloadFile({
          url: url,
          success: (downloadResult) => {
            if (downloadResult.statusCode === 200) {
              uni.openDocument({
                filePath: downloadResult.tempFilePath,
                fileType: 'pdf',
                showMenu: true,
                success: () => {
                  this.pdfSuccess = true
                  this.pdfLoading = false
                },
                fail: (err) => {
                  console.error('openDocument 失败:', err)
                  this.pdfSuccess = false
                  this.pdfErrorMsg = '无法打开文档: ' + (err.errMsg || '未知错误')
                  this.pdfLoading = false
                }
              })
            } else {
              this.pdfSuccess = false
              this.pdfErrorMsg = '下载文件失败 (HTTP ' + downloadResult.statusCode + ')'
              this.pdfLoading = false
            }
          },
          fail: (err) => {
            console.error('downloadFile 失败:', err)
            this.pdfSuccess = false
            this.pdfErrorMsg = '下载文件失败: ' + (err.errMsg || '网络错误')
            this.pdfLoading = false
          }
        })
        // #endif
      },
      loadTextContent(url) {
        // 转换为完整 URL
        url = this.getFullUrl(url)

        console.log('加载文本文件，完整URL:', url)

        // 使用 fetch API 直接加载文本文件（更可靠）
        if (typeof fetch !== 'undefined') {
          fetch(url)
            .then(response => {
              if (response.ok) {
                return response.text()
              }
              throw new Error(`HTTP ${response.status}`)
            })
            .then(text => {
              this.textContent = text
            })
            .catch(err => {
              console.error('加载文本文件失败:', err)
              this.textContent = '文件加载失败: ' + err.message
            })
        } else {
          // 回退到 uni.request
          uni.request({
            url: url,
            method: 'GET',
            header: {
              'Content-Type': 'text/plain'
            },
            success: (res) => {
              if (res.statusCode === 200) {
                this.textContent = res.data
              } else {
                this.textContent = '文件加载失败 (HTTP ' + res.statusCode + ')'
              }
            },
            fail: (err) => {
              console.error('加载文本文件失败:', err)
              this.textContent = '文件加载失败: ' + JSON.stringify(err)
            }
          })
        }
      }
    }
  }
  </script>

  <style scoped>
  .preview-container {
    padding: 16px;
    height: calc(100vh - 44px);
  }
  .text-preview {
    height: 100%;
    background-color: #f8f9fa;
    border-radius: 8px;
    padding: 16px;
    box-sizing: border-box;
  }
  .text-content {
    font-family: 'Courier New', Courier, monospace;
    font-size: 14px;
    line-height: 1.6;
    color: #333;
    white-space: pre-wrap;
    word-break: break-all;
  }
  .empty-preview {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 80px 20px;
  }
  .empty-title {
    margin-top: 20px;
    font-size: 18px;
    font-weight: 500;
    color: #666;
  }
  .empty-desc {
    margin-top: 10px;
    font-size: 14px;
    color: #999;
  }
  .pdf-loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 80px 20px;
  }
  .loading-text {
    margin-top: 16px;
    font-size: 16px;
    color: #007aff;
  }
  .pdf-result {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 80px 20px;
  }
  .result-title {
    margin-top: 16px;
    font-size: 18px;
    font-weight: 500;
  }
  .result-desc {
    margin-top: 10px;
    font-size: 14px;
    color: #f56c6c;
  }
  </style>
