// 应用全局配置
export default {
  // baseUrl: 'https://vue.ruoyi.vip/prod-api',
  // baseUrl: 'http://localhost:8080',
  // baseUrl: '/prod-api',
	// 生产地址请通过打包环境变量注入，勿提交真实域名
	baseUrl: '/prod-api',

  // 应用信息
  appInfo: {
    // 应用名称
    name: "hms-app",
    // 应用版本
    version: "1.3.2",
    // 应用logo
    logo: "/static/logo.png",
    // 官方网站
    site_url: "https://your-domain.com",
    // 政策协议
    agreements: [{
        title: "隐私政策",
        url: "https://ruoyi.vip/protocol.html"
      },
      {
        title: "用户服务协议",
        url: "https://ruoyi.vip/protocol.html"
      }
    ]
  }
}
