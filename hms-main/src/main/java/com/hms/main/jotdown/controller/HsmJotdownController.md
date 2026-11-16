# 随手记 (HsmJotdownController)

## 接口列表

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /main/jotdown/list | 分页查询列表 | main:jotdown:list |
| GET | /main/jotdown/stats | 统一统计（总金额/总条数/类型分布），不受分页影响 | main:jotdown:list |
| POST | /main/jotdown/export | 导出Excel | main:jotdown:export |
| GET | /main/jotdown/{id} | 获取详情 | main:jotdown:query |
| POST | /main/jotdown | 新增 | main:jotdown:add |
| PUT | /main/jotdown | 修改 | main:jotdown:edit |
| DELETE | /main/jotdown/{ids} | 删除 | main:jotdown:remove |
| POST | /main/jotdown/ai-recognize | AI图片识别录入 | main:jotdown:add |

## 统计接口

```
GET /main/jotdown/stats?description=关键词&date=2026-07-02

参数（可选，与列表查询条件一致）:
  description: 说明模糊搜索
  date: 日期筛选
  expenseType: 费用类型
  accountingPeriod: 会计期间

返回:
{
  "code": 200,
  "data": {
    "totalAmount": 12345.67,
    "totalCount": 42,
    "typeDistribution": [
      { "expenseType": "1", "total": 5000.00 },
      { "expenseType": "2", "total": 3000.00 }
    ]
  }
}
```

## AI识别接口

```
POST /main/jotdown/ai-recognize
Content-Type: multipart/form-data

参数:
  file: 图片文件 (MultipartFile)

返回:
{
  "code": 200,
  "data": {
    "description": "账单描述",
    "amount": "128.50",
    "date": "2026-06-01",
    "expenseType": "1",
    "remarks": "备注"
  }
}
```

识别策略: JotdownRecognitionStrategy，费用类型编码: 1餐饮/2交通/3购物/4娱乐/5医疗/6住房/9其他
