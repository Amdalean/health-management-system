const TokenKey = 'App-Token'

let cachedToken = uni.getStorageSync(TokenKey) || ''
console.log('[auth] 初始读取 token:', cachedToken ? '有值' : '为空')

export function getToken() {
  if (cachedToken) {
    console.log('[auth] getToken 缓存命中:', '有值')
    return cachedToken
  }
  cachedToken = uni.getStorageSync(TokenKey) || ''
  console.log('[auth] getToken 同步重读:', cachedToken ? '有值' : '为空')
  if (!cachedToken) {
    uni.getStorage({
      key: TokenKey,
      success: (res) => {
        cachedToken = res.data || ''
        console.log('[auth] getToken 异步兜底:', cachedToken ? '有值' : '为空')
      },
      fail: () => {
        // 未登录时无 token 属正常情况，无需打印，避免误导
      }
    })
  }
  return cachedToken
}

export function setToken(token) {
  cachedToken = token || ''
  return uni.setStorageSync(TokenKey, cachedToken)
}

export function removeToken() {
  cachedToken = ''
  return uni.removeStorageSync(TokenKey)
}
