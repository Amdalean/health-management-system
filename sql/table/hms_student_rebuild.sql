-- ----------------------------
-- 学生建档表 hms_student 重建（对齐论文表1「学生建档表 student」）
-- add by CYQ 2026-10-18 说明:
--   1) 按论文表1重建字段：student_id / student_no / name / gender / birth_date /
--      major / grade / phone / enrollment_date；
--   2) 新增 user_id，用于关联「生成账号」后创建的系统用户（sys_user），
--      同时补充 create_by / create_time / update_by / update_time / remark 标准审计字段；
--   3) 原表先改名为 hms_student_bak 备份，历史数据按语义映射迁移后保留备份表（确认无误后可手动 DROP）。
--   字段语义映射：id→student_id、student_number→student_no、birthdate→birth_date、
--                admission_date→enrollment_date、gender 保持 CHAR(2)
--   注意：hms_score_record.student_id 引用本表主键，迁移时保持 id 不变。
-- ----------------------------

-- 1. 备份原表（可重复执行）
DROP TABLE IF EXISTS `hms_student_bak`;
RENAME TABLE `hms_student` TO `hms_student_bak`;

-- 2. 重建 hms_student
CREATE TABLE `hms_student` (
  `student_id`      bigint       NOT NULL AUTO_INCREMENT COMMENT '学生ID（主键）',
  `student_no`      varchar(20)  NOT NULL                COMMENT '学号',
  `name`            varchar(50)  NOT NULL                COMMENT '姓名',
  `gender`          char(2)      DEFAULT NULL            COMMENT '性别（0=男 1=女 2=未知，字典 sys_user_sex）',
  `birth_date`      date         DEFAULT NULL            COMMENT '出生日期',
  `major`           varchar(100) DEFAULT NULL            COMMENT '专业',
  `grade`           varchar(20)  DEFAULT NULL            COMMENT '年级',
  `phone`           varchar(20)  DEFAULT NULL            COMMENT '联系电话',
  `enrollment_date` date         DEFAULT NULL            COMMENT '入学时间',
  `user_id`         bigint       DEFAULT NULL            COMMENT '关联系统用户ID（生成账号后回写 sys_user.user_id）',
  `create_by`       varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time`     datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`       varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time`     datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`          varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`student_id`) USING BTREE,
  UNIQUE KEY `uk_student_no` (`student_no`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学生建档表';

-- 3. 迁移历史数据（保持主键不变，避免 hms_score_record.student_id 失联）
INSERT INTO `hms_student`
  (`student_id`, `student_no`, `name`, `gender`, `birth_date`, `major`, `grade`, `phone`,
   `enrollment_date`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT `id`, `student_number`, `name`, CAST(`gender` AS CHAR), `birthdate`, `major`, `grade`, `phone`,
       `admission_date`, 'admin', IFNULL(`created_at`, sysdate()), '', `updated_at`
FROM `hms_student_bak`;

-- 4. 校验
SELECT `student_id`, `student_no`, `name`, `gender`, `birth_date`, `major`, `grade`, `phone`,
       `enrollment_date`, `user_id` FROM `hms_student`;
