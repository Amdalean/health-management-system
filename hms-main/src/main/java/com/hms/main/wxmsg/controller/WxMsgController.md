# WxMsgController 接口文档

## 概述
WxMsgController 是微信消息管理相关的控制器，提供微信消息处理和展示功能。

## 接口列表

### 1. 返回完整HTML页面
- **URL**: `/main/wxmsg/renderHtml`
- **方法**: GET
- **描述**: 返回一个完整的HTML页面，用于在浏览器中渲染微信消息相关内容
- **返回类型**: String (HTML内容)
- **示例响应**:
```html
<!DOCTYPE html>
<html>
<head>
    <title>微信消息页面</title>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { color: #333; border-bottom: 1px solid #ccc; padding-bottom: 10px; }
        .content { margin-top: 20px; }
        .footer { margin-top: 30px; color: #999; font-size: 0.9em; }
    </style>
</head>
<body>
    <div class="header">
        <h1>微信消息管理系统</h1>
    </div>
    <div class="content">
        <p>欢迎使用微信消息管理系统！</p>
        <p>此系统用于处理和管理微信消息。</p>
    </div>
    <div class="footer">
        <p>&copy; 2025 健康管理系统. All rights reserved.</p>
    </div>
</body>
</html>
```

### 2. 返回HTML片段
- **URL**: `/main/wxmsg/htmlFragment`
- **方法**: GET
- **描述**: 返回一个HTML片段，可用于嵌入到现有页面中
- **返回类型**: String (HTML片段)
- **示例响应**:
```html
<div style='padding: 20px; background-color: #f0f8ff; border: 1px solid #ccc;'>
<h2>微信消息预览</h2>
<p>这里是微信消息的内容预览区域</p>
<button onclick='alert("点击了按钮!")'>点击测试</button>
</div>
```