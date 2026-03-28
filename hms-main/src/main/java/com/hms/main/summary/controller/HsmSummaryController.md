# HsmSummaryController 接口文档

## 1. 查询财务汇总主列表
- **URL**: `/main/summary/list`
- **Method**: `GET`
- **权限**: `main:summary:list`
- **描述**: 查询财务汇总主列表

## 2. 导出财务汇总主列表  
- **URL**: `/main/summary/export`
- **Method**: `POST`
- **权限**: `main:summary:export`
- **描述**: 导出财务汇总主列表

## 3. 获取财务汇总主详细信息
- **URL**: `/main/summary/{id}`
- **Method**: `GET`
- **权限**: `main:summary:query`
- **描述**: 根据ID获取财务汇总主详细信息

## 4. 获取财务汇总主初始化信息
- **URL**: `/main/summary/init`
- **Method**: `GET`
- **权限**: 无
- **描述**: 获取财务汇总主初始化信息

## 5. 新增财务汇总主
- **URL**: `/main/summary`
- **Method**: `POST`
- **权限**: `main:summary:add`
- **描述**: 新增财务汇总主

## 6. 修改财务汇总主
- **URL**: `/main/summary`
- **Method**: `PUT`
- **权限**: `main:summary:edit`
- **描述**: 修改财务汇总主

## 7. 删除财务汇总主
- **URL**: `/main/summary/{ids}`
- **Method**: `DELETE`
- **权限**: `main:summary:remove`
- **描述**: 删除财务汇总主

## 8. 检查支出
- **URL**: `/main/summary/checkExpense`
- **Method**: `GET`
- **权限**: 无
- **描述**: 获取全年支出数据

## 9. 获取存款计划执行情况
- **URL**: `/main/summary/depositPlan`
- **Method**: `GET`
- **权限**: 无
- **描述**: 获取存款计划执行数据

## 10. 获取存款预测数据
- **URL**: `/main/summary/depositPrediction`
- **Method**: `GET`
- **权限**: 无
- **描述**: 获取存款预测数据

## 11. 查询最新汇总明细按类型分组
- **URL**: `/main/summary/lastSummaryDetailByType`
- **Method**: `GET`
- **权限**: 无
- **描述**: 查询最新汇总明细按类型分组

## 12. AI财务分析接口
- **URL**: `/main/summary/aiFinancialAnalysis`
- **Method**: `GET`
- **权限**: `main:summary:ai-analysis`
- **描述**: 基于用户个人财务数据进行AI分析，结果会被缓存30分钟以提升性能
- **参数**:
  - `query` (String, 可选): 分析查询内容，默认为"分析我的财务状况"
- **返回**: AI分析结果


