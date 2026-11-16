import config from '@/config'
import storage from '@/utils/storage'
import constant from '@/utils/constant'
import { login, logout, getInfo, wxLogin, wxBindLogin } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'
import defaultAvatar from '@/static/images/profile.jpg'

const baseUrl = config.baseUrl

const user = {
  state: {
    token: getToken(),
    name: storage.get(constant.name),
    avatar: storage.get(constant.avatar),
    roles: storage.get(constant.roles),
    permissions: storage.get(constant.permissions)
  },

  mutations: {
    SET_TOKEN: (state, token) => {
      state.token = token
    },
    SET_NAME: (state, name) => {
      state.name = name
      storage.set(constant.name, name)
    },
    SET_AVATAR: (state, avatar) => {
      state.avatar = avatar
      storage.set(constant.avatar, avatar)
    },
    SET_ROLES: (state, roles) => {
      state.roles = roles
      storage.set(constant.roles, roles)
    },
    SET_PERMISSIONS: (state, permissions) => {
      state.permissions = permissions
      storage.set(constant.permissions, permissions)
    }
  },

  actions: {
    // 登录
    Login({ commit }, userInfo) {
      const username = userInfo.username.trim()
      const password = userInfo.password
      const code = userInfo.code
      const uuid = userInfo.uuid
      return new Promise((resolve, reject) => {
        login(username, password, code, uuid).then(res => {
          setToken(res.token)
          commit('SET_TOKEN', res.token)
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 微信一键登录（小程序）：code 换 token，未绑定返回 needBind+openid
    // add by CYQ 2026-09-06 微信小程序一键登录
    WxLogin({ commit }, code) {
      return new Promise((resolve, reject) => {
        wxLogin(code).then(res => {
          if (res.token) {
            setToken(res.token)
            commit('SET_TOKEN', res.token)
          }
          resolve(res)
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 会话过期静默续登（小程序）：wx.login 的 code 换新 token
    // 与 WxLogin 的区别：needBind / 失败直接 reject，由调用方决定是否走人工登录，不跳绑定页
    // add by CYQ 2026-11-14 已绑定微信的会话过期自动续登
    SilentWxLogin({ commit }, code) {
      return new Promise((resolve, reject) => {
        wxLogin(code).then(res => {
          if (!res.token) {
            reject(new Error(res.needBind ? '该微信未绑定系统账号' : (res.errmsg || '微信登录失败')))
            return
          }
          setToken(res.token)
          commit('SET_TOKEN', res.token)
          resolve(res)
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 微信绑定系统账号并登录（首次使用微信登录）
    // add by CYQ 2026-09-06 微信小程序一键登录
    WxBindLogin({ commit }, bindInfo) {
      return new Promise((resolve, reject) => {
        wxBindLogin(bindInfo.openid, bindInfo.username, bindInfo.password).then(res => {
          setToken(res.token)
          commit('SET_TOKEN', res.token)
          resolve(res)
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 获取用户信息
    GetInfo({ commit, state }) {
      return new Promise((resolve, reject) => {
        getInfo().then(res => {
          const user = res.user
          const avatar = (user == null || user.avatar == "" || user.avatar == null) ? defaultAvatar : baseUrl + user.avatar
          const username = (user == null || user.userName == "" || user.userName == null) ? "" : user.userName
          if (res.roles && res.roles.length > 0) {
            commit('SET_ROLES', res.roles)
            commit('SET_PERMISSIONS', res.permissions)
          } else {
            commit('SET_ROLES', ['ROLE_DEFAULT'])
          }
          commit('SET_NAME', username)
          commit('SET_AVATAR', avatar)
          resolve(res)
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 退出系统
    LogOut({ commit, state }) {
      return new Promise((resolve, reject) => {
        logout(state.token).then(() => {
          commit('SET_TOKEN', '')
          commit('SET_ROLES', [])
          commit('SET_PERMISSIONS', [])
          removeToken()
          storage.clean()
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    }
  }
}

export default user
