/**
 * 主题管理模块
 */
const state = {
  // 当前主题名，默认粉白
  theme: 'default'
}

const mutations = {
  SET_THEME: (state, theme) => {
    state.theme = theme
    uni.setStorageSync('app-theme', theme)
  },
  // 应用启动时从 Storage 恢复
  INIT_THEME: (state) => {
    const saved = uni.getStorageSync('app-theme')
    if (saved) {
      state.theme = saved
    }
  }
}

const actions = {
  switchTheme({ commit }, theme) {
    commit('SET_THEME', theme)
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
