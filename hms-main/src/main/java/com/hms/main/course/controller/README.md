# 课程管理 API 文档

**创建时间**: 2026-10-18
**作者**: CYQ
**Controller**: `hms-main/src/main/java/com/hms/main/course/controller/HmsCourseController.java`
**数据表**: `hms_course`（论文表3）、`hms_student_course`（论文表5）

> add by CYQ 2026-10-18 说明: 按论文表3「课程建档表 course」与论文表5「学生课程分配表」新建课程管理模块，
> 提供课程增删改查、导出，以及学生选课/退课管理。
> 说明：论文表4「教师课程分配表」与表3 的 `teacher_id` 语义重复，
> 本实现用 `hms_course.teacher_id` 直接表达"一门课程一位主讲教师"的关系，未单独建表。

## 接口列表

### 1. 查询课程建档列表

- **接口地址**: `GET /main/course/list`
- **权限**: `main:course:list`
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页数量，默认 10 |
| courseName | String | 否 | 课程名称（模糊匹配） |
| teacherId | Long | 否 | 授课教师ID |
| semester | String | 否 | 学期（精确匹配） |

- **返回**: `TableDataInfo`，`rows` 为 `HmsCourse` 集合，含 `teacherName`（授课教师）与 `studentCount`（选课人数）

---

### 2. 课程下拉选项

- **接口地址**: `GET /main/course/options`
- **权限**: `main:course:list`
- **说明**: 返回全部课程，供成绩分析等模块做课程选择

---

### 3. 获取课程建档详情

- **接口地址**: `GET /main/course/{courseId}`
- **权限**: `main:course:query`

---

### 4. 新增课程建档

- **接口地址**: `POST /main/course`
- **权限**: `main:course:add`
- **业务校验**: 同一学期内课程名称唯一（唯一索引 `uk_course_name_semester`），重复时返回失败提示

**请求示例**:

```json
{
  "courseName": "八段锦与体育健康",
  "teacherId": 1,
  "credit": 2,
  "hours": 32,
  "semester": "2026-2027-1",
  "remark": "体育实践课"
}
```

---

### 5. 修改课程建档

- **接口地址**: `PUT /main/course`
- **权限**: `main:course:edit`
- **业务校验**: 同上，需排除自身记录

---

### 6. 删除课程建档

- **接口地址**: `DELETE /main/course/{courseIds}`
- **权限**: `main:course:remove`
- **说明**: 支持批量；删除时会级联清理 `hms_student_course` 中该课程的选课记录（同一事务）

---

### 7. 导出课程建档

- **接口地址**: `POST /main/course/export`
- **权限**: `main:course:export`

---

### 8. 查询课程已选学生

- **接口地址**: `GET /main/course/{courseId}/students`
- **权限**: `main:course:assign`
- **返回**: `HmsStudentCourse` 集合（`assignmentId` / `studentId` / `courseId` / `assignTime` + `studentNo` / `studentName` / `grade` / `major`）

---

### 9. 查询选课候选学生

- **接口地址**: `GET /main/course/{courseId}/candidates`
- **权限**: `main:course:assign`
- **说明**: 返回尚未选修该课程的全部学生，供前端穿梭框（已选 / 未选）展示

---

### 10. 批量选课

- **接口地址**: `POST /main/course/{courseId}/students`
- **权限**: `main:course:assign`
- **请求体**: 学生ID数组，例如 `[1, 2, 3]`
- **说明**: 使用 `insert ignore` + 唯一索引 `(student_id, course_id)` 保证不重复选课；
  返回值为**新增**的选课记录条数（已选过的不会重复插入）

---

### 11. 批量退课

- **接口地址**: `DELETE /main/course/{courseId}/students/{studentIds}`
- **权限**: `main:course:assign`
- **说明**: 多个学生ID以英文逗号分隔

---

## 相关文件

- Controller: `hms-main/src/main/java/com/hms/main/course/controller/HmsCourseController.java`
- Service: `hms-main/src/main/java/com/hms/main/course/service/impl/HmsCourseServiceImpl.java`
- Mapper: `hms-main/src/main/java/com/hms/main/course/mapper/HmsCourseMapper.java`
- XML: `hms-main/src/main/resources/mapper/main/HmsCourseMapper.xml`
- 建表脚本: `sql/table/hms_course.sql`
- 菜单脚本: `sql/table/hms_course_menu.sql`
- 前端页面: `RuoYi-Vue3/src/views/main/course/index.vue`
