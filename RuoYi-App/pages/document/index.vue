<template>
    <view class="document-container">
      <view class="search-bar">
        <uni-easyinput v-model="query.fileName" placeholder="请输入文件名称" />
        <button type="primary" size="mini" @click="getList">搜索</button>
        <button type="default" size="mini" @click="resetQuery">重置</button>
      </view>
      <view class="add-btn">
        <button type="primary" @click="goAdd">新增资料</button>
      </view>
      <uni-list>
        <uni-list-item v-for="item in list" :key="item.id" :title="item.fileName" :note="item.description" clickable @click="goPreview(item)" rightText="预览">
          <template v-slot:footer>
            <button size="mini" @click.stop="goEdit(item)">编辑</button>
            <button size="mini" type="warn" @click.stop="del(item)">删除</button>
          </template>
        </uni-list-item>
      </uni-list>
    </view>
  </template>
  
  <script>
  import { listDocument, delDocument } from '@/api/main/document'
  export default {
    data() {
      return {
        query: { fileName: '' },
        list: []
      }
    },
    onShow() {
      this.getList()
    },
    methods: {
      getList() {
        listDocument(this.query).then(res => {
          this.list = res.data || []
        })
      },
      resetQuery() {
        this.query.fileName = ''
        this.getList()
      },
      goAdd() {
        uni.navigateTo({ url: '/pages/document/add' })
      },
      goEdit(item) {
        uni.navigateTo({ url: '/pages/document/edit?id=' + item.id })
      },
      goPreview(item) {
        uni.navigateTo({ url: '/pages/document/preview?id=' + item.id })
      },
      del(item) {
        uni.showModal({
          title: '提示',
          content: '确定删除该资料吗？',
          success: (res) => {
            if (res.confirm) {
              delDocument(item.id).then(() => {
                uni.showToast({ title: '删除成功', icon: 'success' })
                this.getList()
              })
            }
          }
        })
      }
    }
  }
  </script>
  
  <style scoped>
  .document-container { padding: 16px; }
  .search-bar { display: flex; gap: 8px; margin-bottom: 12px; }
  .add-btn { margin-bottom: 12px; }
  </style>
  