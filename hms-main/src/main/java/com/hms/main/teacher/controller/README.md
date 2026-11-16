# 教师建档 API 文档

**创建时间**: 2026-10-18
**作者**: CYQ
**Controller**: `hms-main/src/main/java/com/hms/main/teacher/controller/HmsTeacherController.java`
**数据表**: `hms_teacher`（teacher_id / staff_no / name / gender / title / phone / hire_date / department / user_id）

> add by CYQ 2026-10-18 说明: 按论文表2「教师建档表 teacher」新建教师建档模块，
> 提供增删改查、导出，以及依据教师工号生成系统账号的接口。

## 接口列表

### 1. 查询教师建档列表

- **接口地址**: `GET /main/teacher/list`
- **权限**: `main:teacher:list`
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页数量，默认 10 |
| staffNo | String | 否 | 教师工号（模糊匹配） |
| name | String | 否 | 姓名（模糊匹配） |
| gender | String | 否 | 性别（0=男 1=女 2=未知） |
| title | String | 否 | 职称（模糊匹配） |
| department | String | 否 | 所属部门（模糊匹配） |
| hasAccount | Integer | 否 | 账号状态筛选：0=未生成系统账号，1=已生成 |

- **返回**: `TableDataInfo`，`rows` 为 `HmsTeacher` 集合（含 `userId`，非空表示已生成账号）

---

### 2. 获取教师建档详情

- **接口地址**: `GET /main/teacher/{teacherId}`
- **权限**: `main:teacher:query`

---

### 3. 新增教师建档

- **接口地址**: `POST /main/teacher`
- **权限**: `main:teacher:add`
- **请求体**: `HmsTeacher`
- **业务校验**: 教师工号（`staffNo`）唯一，重复时返回失败提示

**请求示例**:

```json
{
  "staffNo": "T0002",
  "name": "张三",
  "gender": "0",
  "title": "讲师",
  "phone": "13800000001",
  "hireDate": "2020-09-01",
  "department": "计算机学院",
  "remark": "新入职教师"
}
```

---

### 4. 修改教师建档

- **接口地址**: `PUT /main/teacher`
- **权限**: `main:teacher:edit`

---

### 5. 删除教师建档

- **接口地址**: `DELETE /main/teacher/{teacherIds}`
- **权限**: `main:teacher:remove`
- **说明**: 支持批量，多个 ID 以英文逗号分隔

---

### 6. 导出教师建档

- **接口地址**: `POST /main/teacher/export`
- **权限**: `main:teacher:export`

---

### 7. 为教师生成系统账号（单条）

- **接口地址**: `POST /main/teacher/generateUser/{teacherId}`
- **权限**: `main:teacher:generateUser`
- **处理逻辑**:
  1. 校验教师是否存在、是否已生成账号（`user_id` 非空则拒绝，保证幂等）；
  2. 用户名 = **教师工号**，昵称 = 姓名，手机号 = 联系电话；
  3. 初始密码取系统配置 `sys.user.initPassword`（默认 `123456`），BCrypt 加密；
  4. 角色自动分配为「教师」（`role_key = teacher`，不存在时自动创建）；
  5. 创建成功后回写 `hms_teacher.user_id`。
- **返回示例**:

```json
{
  "code": 200,
  "msg": "账号生成成功，系统用户ID：131"
}
```

---

### 8. 批量为教师生成系统账号

- **接口地址**: `POST /main/teacher/generateUser/batch`
- **权限**: `main:teacher:generateUser`
- **请求体**: 教师ID数组，例如 `[1, 2]`
- **说明**: 逐条独立事务，允许部分成功，返回成功/失败统计与明细
- **返回结构**: 同学生批量生成接口（`total` / `success` / `failure` / `summary` / `details`）

---

## 相关文件

- Controller: `hms-main/src/main/java/com/hms/main/teacher/controller/HmsTeacherController.java`
- Service: `hms-main/src/main/java/com/hms/main/teacher/service/impl/HmsTeacherServiceImpl.java`
- Mapper: `hms-main/src/main/java/com/hms/main/teacher/mapper/HmsTeacherMapper.java`
- XML: `hms-main/src/main/resources/mapper/main/HmsTeacherMapper.xml`
- 账号生成服务: `hms-main/src/main/java/com/hms/main/account/service/impl/AccountGenerateServiceImpl.java`
- 建表脚本: `sql/table/hms_teacher.sql`
- 菜单脚本: `sql/table/hms_teaching_menu.sql`
