<template>
  <view class="tree-node">
    <view class="node-content" :style="{ paddingLeft: level * 24 + 'px' }">
      <!-- 节点内容 -->
      <view class="node-item" @click="handlePreview">
        <!-- 展开/折叠图标 -->
        <view v-if="hasChildren" class="expand-icon" @click.stop="toggleExpand">
          <uni-icons :type="expanded ? 'bottom' : 'right'" size="14" color="#666"></uni-icons>
        </view>
        <view v-else class="expand-icon-placeholder"></view>

        <view class="node-info">
          <text class="node-name">{{ node.fileName }}</text>
          <text v-if="node.description" class="node-desc">{{ node.description }}</text>
        </view>

        <!-- 操作按钮组 -->
        <view class="node-actions">
          <view class="action-icon" @click.stop="handleAdd">
            <uni-icons type="plusempty" size="18" color="#007aff"></uni-icons>
          </view>
          <view class="action-icon" @click.stop="handleEdit">
            <uni-icons type="compose" size="18" color="#e6a23c"></uni-icons>
          </view>
          <view class="action-icon" @click.stop="handleDelete">
            <uni-icons type="trash" size="18" color="#f56c6c"></uni-icons>
          </view>
        </view>
      </view>
    </view>

    <!-- 子节点 -->
    <view v-if="hasChildren && expanded" class="children-container">
      <tree-node
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :level="level + 1"
        :is-expand-all="isExpandAll"
        @preview="$emit('preview', $event)"
        @edit="$emit('edit', $event)"
        @delete="$emit('delete', $event)"
        @add="$emit('add', $event)"
      />
    </view>
  </view>
</template>

<script>
export default {
  name: 'TreeNode',
  props: {
    node: {
      type: Object,
      required: true
    },
    level: {
      type: Number,
      default: 0
    },
    isExpandAll: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      expanded: true
    }
  },
  computed: {
    hasChildren() {
      return this.node.children && this.node.children.length > 0
    }
  },
  watch: {
    isExpandAll(val) {
      this.expanded = val
    }
  },
  methods: {
    toggleExpand() {
      this.expanded = !this.expanded
    },
    handlePreview() {
      if (!this.hasChildren) {
        this.$emit('preview', this.node)
      } else {
        this.toggleExpand()
      }
    },
    handleAdd() {
      this.$emit('add', this.node)
    },
    handleEdit() {
      this.$emit('edit', this.node)
    },
    handleDelete() {
      this.$emit('delete', this.node)
    }
  }
}
</script>

<style scoped>
.tree-node {
  margin-bottom: 2px;
}

.node-content {
  display: flex;
  align-items: flex-start;
  padding: 4px 0;
}

.node-item {
  flex: 1;
  display: flex;
  flex-direction: row;
  align-items: center;
  background-color: #f9f9f9;
  border-radius: 8px;
  padding: 12px;
  border-left: 3px solid #007aff;
}

.expand-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 4px;
}

.expand-icon-placeholder {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  margin-right: 4px;
}

.node-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.node-name {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-desc {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-actions {
  display: flex;
  flex-direction: row;
  gap: 12px;
  align-items: center;
  flex-shrink: 0;
  margin-left: 8px;
}

.action-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.action-icon:active {
  background-color: #f0f0f0;
}

.children-container {
  margin-top: 2px;
}
</style>
