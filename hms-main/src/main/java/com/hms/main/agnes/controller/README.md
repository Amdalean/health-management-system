# Agnes Image 2.5 Flash 图像生成 API 文档

**创建时间**: 2026-07-20
**作者**: Qwen
**更新时间**: 2026-11-09（模型升级至 agnes-image-2.5-flash）

## 接口列表

### 1. 文生图

**接口地址**: `POST /agnes/image/generate`

**请求参数**:

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| prompt | String | 是 | - | 图像生成提示词 |
| size | String | 否 | 1K | 输出尺寸档位：1K / 2K / 3K / 4K |
| ratio | String | 否 | 1:1 | 宽高比：1:1 / 3:4 / 4:3 / 16:9 / 9:16 / 2:3 / 3:2 / 21:9 |

**请求示例**:

```json
{
  "prompt": "日出时分薄雾峡谷上方的发光浮空城市，电影级写实风格，广角构图",
  "size": "2K",
  "ratio": "16:9"
}
```

**响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": "https://platform-outputs.agnes-ai.space/images/t2i/xxx.png"
}
```

---

### 2. 文生图 - Base64 输出

**接口地址**: `POST /agnes/image/generateBase64`

请求参数同上，返回图片的 Base64 编码字符串。

---

### 3. 图生图

**接口地址**: `POST /agnes/image/edit`

> 说明：2.5 Flash 的底层 API 支持在 `extra_body.image` 中传多张参考图做多图合成，当前接口只暴露单图 `imageUrl`；需要多图合成时，把 `AgnesImageService.editImage` 的 `imageUrl` 参数改为 `List<String>` 传入多张即可（参考 `hms-agnes-image` skill）。

**请求参数**:

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| prompt | String | 是 | - | 编辑/风格化提示词 |
| imageUrl | String | 是 | - | 输入图片的公开可访问 URL |
| size | String | 否 | 1K | 输出尺寸档位 |
| ratio | String | 否 | 1:1 | 宽高比 |

**请求示例**:

```json
{
  "prompt": "将白天街道改为电影级赛博朋克夜景，添加霓虹招牌",
  "imageUrl": "https://example.com/input.png",
  "size": "2K",
  "ratio": "16:9"
}
```

---

## 尺寸参考

| Ratio | 1K | 2K | 4K |
|-------|-----|-----|-----|
| 1:1 | 1024×1024 | 2048×2048 | 4096×4096 |
| 16:9 | 1312×736 | 2624×1472 | 5248×2944 |
| 9:16 | 736×1312 | 1472×2624 | 2944×5248 |

## 配置

```yaml
ai:
  agnes:
    image:
      api-key: sk-xxx
      base-url: https://apihub.agnes-ai.com/v1/images/generations
      model: agnes-image-2.5-flash
```
