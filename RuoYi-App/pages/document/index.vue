<template>
    <view class="document-container">
      <view class="search-bar">
        <uni-easyinput v-model="query.fileName" placeholder="请输入文件名称" />
        <button class="search-btn" type="primary" size="mini" @click="getList">搜索</button>
        <button class="reset-btn" type="default" size="mini" @click="resetQuery">重置</button>
      </view>
      <view class="action-bar">
        <button class="add-btn" type="primary" size="mini" @click="goAdd">
          <uni-icons type="plusempty" size="14" color="#fff"></uni-icons>
          <text>新增</text>
        </button>
        <button class="toggle-btn" type="default" size="mini" @click="toggleExpandAll">
          <uni-icons :type="isExpandAll ? 'up' : 'down'" size="14"></uni-icons>
          <text>{{ isExpandAll ? '收起' : '展开' }}</text>
        </button>
      </view>
      <view class="tree-container">
        <tree-node 
          v-for="item in treeList" 
          :key="item.id" 
          :node="item" 
          :is-expand-all="isExpandAll"
          @preview="goPreview"
          @edit="goEdit"
          @delete="del"
          @add="goAddWithParent"
        />
        <view v-if="treeList.length === 0" class="empty-tip">
          <uni-icons type="info" size="40" color="#ccc"></uni-icons>
          <text class="empty-text">暂无资料</text>
        </view>
      </view>
    </view>
  </template>

  <script>
  import { listDocument, delDocument } from '@/api/main/document'
  import TreeNode from './components/TreeNode.vue'
  
  export default {
    components: {
      TreeNode
    },
    data() {
      return {
        query: { fileName: '' },
        list: [],
        treeList: [],
        isExpandAll: true
      }
    },
    onShow() {
      this.getList()
    },
    methods: {
      getList() {
        listDocument(this.query).then(res => {
          this.list = res.data || []
          this.treeList = this.buildTree(this.list)
        })
      },
      /** 构建树形结构 */
      buildTree(data) {
        const map = {}
        const tree = []
        
        data.forEach(item => {
          map[item.id] = { ...item, children: [], expanded: true }
        })
        
        data.forEach(item => {
          const node = map[item.id]
          const parentId = item.pid || 0
          if (parentId === 0 || !map[parentId]) {
            tree.push(node)
          } else {
            map[parentId].children.push(node)
          }
        })
        
        return tree
      },
      /** 展开/收起全部 */
      toggleExpandAll() {
        this.isExpandAll = !this.isExpandAll
        this.setExpandState(this.treeList, this.isExpandAll)
      },
      /** 递归设置展开状态 */
      setExpandState(nodes, expanded) {
        nodes.forEach(node => {
          node.expanded = expanded
          if (node.children && node.children.length > 0) {
            this.setExpandState(node.children, expanded)
          }
        })
      },
      resetQuery() {
        this.query.fileName = ''
        this.getList()
      },
      goAdd() {
        uni.navigateTo({ url: '/pages/document/add' })
      },
      goAddWithParent(item) {
        uni.navigateTo({ url: '/pages/document/add?parentId=' + item.id })
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
  .document-container { 
    padding: 16px; 
    min-height: 100vh;
    background-color: #f5f5f5;
  }
  .search-bar { 
    display: flex; 
    gap: 8px; 
    margin-bottom: 12px;
    background-color: #fff;
    padding: 12px;
    border-radius: 8px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  }
  .search-btn {
    flex-shrink: 0;
  }
  .reset-btn {
    flex-shrink: 0;
  }
  .action-bar { 
    display: flex; 
    gap: 8px; 
    margin-bottom: 12px; 
  }
  .add-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    flex-shrink: 0;
  }
  .toggle-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    flex-shrink: 0;
  }
  .tree-container {
    background-color: #fff;
    border-radius: 8px;
    padding: 12px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  }
  .empty-tip {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 0;
  }
  .empty-text {
    margin-top: 12px;
    font-size: 14px;
    color: #999;
  }
  </style>
  