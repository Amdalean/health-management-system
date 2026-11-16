# 办公地点接口

add by CYQ 2026-06-06 说明: 办公地点CRUD管理，用于打卡地理围栏判定

## 接口列表

### 1. 查询办公地点列表（分页）
- **URL**: `GET /mobile/officeLocation/list`
- **权限**: `mobile:officeLocation:list`
- **请求参数**: 标准分页参数 + name(可选) + status(可选)
- **返回**: 分页表格数据

### 2. 导出办公地点
- **URL**: `POST /mobile/officeLocation/export`
- **权限**: `mobile:officeLocation:export`
- **返回**: Excel 文件流

### 3. 获取办公地点详情
- **URL**: `GET /mobile/officeLocation/{id}`
- **权限**: `mobile:officeLocation:query`

### 4. 新增办公地点
- **URL**: `POST /mobile/officeLocation`
- **权限**: `mobile:officeLocation:add`
- **请求参数**:
```json
{
  "name": "公司总部",
  "latitude": 39.904200,
  "longitude": 116.407396,
  "radius": 500,
  "status": "1"
}
```

### 5. 修改办公地点
- **URL**: `PUT /mobile/officeLocation`
- **权限**: `mobile:officeLocation:edit`

### 6. 删除办公地点
- **URL**: `DELETE /mobile/officeLocation/{ids}`
- **权限**: `mobile:officeLocation:remove`
