-- ----------------------------
-- 课程建档表 hms_course（对齐论文表3「课程建档表 course」）
-- 学生选课表 hms_student_course（对齐论文表5「学生课程分配表 Student_Course_Assignment」）
-- add by CYQ 2026-10-18 说明:
--   1) hms_course 字段：course_id / course_name / teacher_id / credit / hours / semester，
--      补充 create_by / create_time / update_by / update_time / remark 标准审计字段；
--      teacher_id 关联 hms_teacher.teacher_id（一门课程一位主讲教师）。
--   2) hms_student_course 字段：assignment_id / student_id / course_id / assign_time，
--      (student_id, course_id) 唯一，保证同一学生不重复选同一门课。
--   3) 说明：论文表4「教师课程分配表」与表3 的 teacher_id 语义重复，
--      本实现用 hms_course.teacher_id 直接表达教师—课程关系，未单独建表。
-- ----------------------------

DROP TABLE IF EXISTS `hms_course`;
CREATE TABLE `hms_course` (
  `course_id`   bigint       NOT NULL AUTO_INCREMENT COMMENT '课程ID（主键）',
  `course_name` varchar(100) NOT NULL                COMMENT '课程名称',
  `teacher_id`  bigint       DEFAULT NULL            COMMENT '授课教师ID（hms_teacher.teacher_id）',
  `credit`      int          DEFAULT NULL            COMMENT '课程学分',
  `hours`       int          DEFAULT NULL            COMMENT '学时',
  `semester`    varchar(20)  DEFAULT NULL            COMMENT '学期，如 2026-2027-1',
  `create_by`   varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time` datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`   varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time` datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`      varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`course_id`) USING BTREE,
  UNIQUE KEY `uk_course_name_semester` (`course_name`, `semester`) USING BTREE,
  KEY `idx_teacher_id` (`teacher_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程建档表';

DROP TABLE IF EXISTS `hms_student_course`;
CREATE TABLE `hms_student_course` (
  `assignment_id` bigint   NOT NULL AUTO_INCREMENT COMMENT '分配ID（主键）',
  `student_id`    bigint   NOT NULL                COMMENT '学生ID（hms_student.student_id）',
  `course_id`     bigint   NOT NULL                COMMENT '课程ID（hms_course.course_id）',
  `assign_time`   datetime DEFAULT NULL            COMMENT '选课（分配）时间',
  PRIMARY KEY (`assignment_id`) USING BTREE,
  UNIQUE KEY `uk_student_course` (`student_id`, `course_id`) USING BTREE,
  KEY `idx_course_id` (`course_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学生选课（学生课程分配）表';

-- 示例数据（幂等）
INSERT INTO `hms_course` (`course_name`, `teacher_id`, `credit`, `hours`, `semester`, `create_by`, `create_time`, `remark`)
SELECT '八段锦与体育健康', t.`teacher_id`, 2, 32, '2026-2027-1', 'admin', sysdate(), '示例数据'
FROM `hms_teacher` t
WHERE t.`staff_no` = 'T0001'
  AND NOT EXISTS (SELECT 1 FROM `hms_course` c WHERE c.`course_name` = '八段锦与体育健康' AND c.`semester` = '2026-2027-1');

INSERT INTO `hms_course` (`course_name`, `teacher_id`, `credit`, `hours`, `semester`, `create_by`, `create_time`, `remark`)
SELECT 'Java 程序设计', t.`teacher_id`, 3, 48, '2026-2027-1', 'admin', sysdate(), '示例数据'
FROM `hms_teacher` t
WHERE t.`staff_no` = 'T0002'
  AND NOT EXISTS (SELECT 1 FROM `hms_course` c WHERE c.`course_name` = 'Java 程序设计' AND c.`semester` = '2026-2027-1');

-- 校验
SELECT c.`course_id`, c.`course_name`, t.`name` AS `teacher_name`, c.`credit`, c.`hours`, c.`semester`
FROM `hms_course` c LEFT JOIN `hms_teacher` t ON t.`teacher_id` = c.`teacher_id`
ORDER BY c.`course_id`;
