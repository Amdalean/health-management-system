# 学生建档 API 文档

**创建时间**: 2024-09-13
**更新时间**: 2026-10-18（add by CYQ：按论文表1对齐字段，新增「生成账号」接口）
**Controller**: `hms-main/src/main/java/com/hms/main/student/controller/HmsStudentController.java`
**数据表**: `hms_student`（student_id / student_no / name / gender / birth_date / major / grade / phone / enrollment_date / user_id）

## 接口列表

### 1. 查询学生建档列表

- **接口地址**: `GET /main/student/list`
- **权限**: `main:student:list`
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页数量，默认 10 |
| studentNo | String | 否 | 学号（模糊匹配） |
| name | String | 否 | 姓名（模糊匹配） |
| gender | String | 否 | 性别（0=男 1=女 2=未知） |
| major | String | 否 | 专业（模糊匹配） |
| grade | String | 否 | 年级（精确匹配） |
| phone | String | 否 | 联系电话（模糊匹配） |
| hasAccount | Integer | 否 | 账号状态筛选：0=未生成系统账号，1=已生成 |

- **返回**: `TableDataInfo`，`rows` 为 `HmsStudent` 集合（含 `userId` 字段，非空表示已生成账号）

---

### 2. 获取学生建档详情

- **接口地址**: `GET /main/student/{studentId}`
- **权限**: `main:student:query`

---

### 3. 新增学生建档

- **接口地址**: `POST /main/student`
- **权限**: `main:student:add`
- **请求体**: `HmsStudent`
- **业务校验**: 学号（`studentNo`）唯一，重复时返回失败提示

---

### 4. 修改学生建档

- **接口地址**: `PUT /main/student`
- **权限**: `main:student:edit`

---

### 5. 删除学生建档

- **接口地址**: `DELETE /main/student/{studentIds}`
- **权限**: `main:student:remove`
- **说明**: 支持批量，多个 ID 以英文逗号分隔

---

### 6. 导出学生建档

- **接口地址**: `POST /main/student/export`
- **权限**: `main:student:export`
- **说明**: 依查询条件导出 Excel

---

### 7. 为学生生成系统账号（单条）

- **接口地址**: `POST /main/student/generateUser/{studentId}`
- **权限**: `main:student:generateUser`
- **处理逻辑**:
  1. 校验学生是否存在、是否已生成账号（`user_id` 非空则拒绝，保证幂等）；
  2. 用户名 = **学号**，昵称 = 姓名，手机号 = 联系电话，性别沿用学生性别；
  3. 初始密码取系统配置 `sys.user.initPassword`（默认 `123456`），使用 BCrypt 加密存储；
  4. 角色自动分配为「学生」（`role_key = student`，不存在时自动创建）；
  5. 创建成功后回写 `hms_student.user_id`。
- **返回示例**:

```json
{
  "code": 200,
  "msg": "账号生成成功，系统用户ID：130"
}
```

---

### 8. 批量为学生生成系统账号

- **接口地址**: `POST /main/student/generateUser/batch`
- **权限**: `main:student:generateUser`
- **请求体**: 学生ID数组，例如 `[1, 2, 3]`
- **说明**: 逐条独立事务执行，允许部分成功；已生成过账号的记录返回失败明细
- **返回示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "total": 3,
    "success": 2,
    "failure": 1,
    "summary": "共处理 3 条，成功 2 条，失败 1 条",
    "details": [
      "【成功】学生【张三】账号 2026001 已生成",
      "【成功】学生【李四】账号 2026002 已生成",
      "【失败】学生ID 5：学生【王五】已生成系统账号，无需重复生成"
    ]
  }
}
```

---

## 相关文件

- Controller: `hms-main/src/main/java/com/hms/main/student/controller/HmsStudentController.java`
- Service: `hms-main/src/main/java/com/hms/main/student/service/impl/HmsStudentServiceImpl.java`
- Mapper: `hms-main/src/main/java/com/hms/main/student/mapper/HmsStudentMapper.java`
- XML: `hms-main/src/main/resources/mapper/main/HmsStudentMapper.xml`
- 账号生成服务: `hms-main/src/main/java/com/hms/main/account/service/impl/AccountGenerateServiceImpl.java`
- 建表脚本: `sql/table/hms_student_rebuild.sql`
- 菜单脚本: `sql/table/hms_teaching_menu.sql`
