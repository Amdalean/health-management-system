-- ----------------------------
-- 教师建档表 hms_teacher（对齐论文表2「教师建档表 teacher」）
-- add by CYQ 2026-10-18 说明:
--   按论文表2建立教师表：teacher_id / name / gender / title / phone / hire_date /
--   department / staff_no；新增 user_id 用于关联「生成账号」后创建的系统用户，
--   并补充 create_by / create_time / update_by / update_time / remark 标准审计字段。
-- ----------------------------
DROP TABLE IF EXISTS `hms_teacher`;
CREATE TABLE `hms_teacher` (
  `teacher_id`  bigint       NOT NULL AUTO_INCREMENT COMMENT '教师ID（主键）',
  `staff_no`    varchar(20)  NOT NULL                COMMENT '教师工号',
  `name`        varchar(50)  NOT NULL                COMMENT '姓名',
  `gender`      char(2)      DEFAULT NULL            COMMENT '性别（0=男 1=女 2=未知，字典 sys_user_sex）',
  `title`       varchar(50)  DEFAULT NULL            COMMENT '职称',
  `phone`       varchar(20)  DEFAULT NULL            COMMENT '联系电话',
  `hire_date`   date         DEFAULT NULL            COMMENT '入职时间',
  `department`  varchar(100) DEFAULT NULL            COMMENT '所属部门',
  `user_id`     bigint       DEFAULT NULL            COMMENT '关联系统用户ID（生成账号后回写 sys_user.user_id）',
  `create_by`   varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time` datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`   varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time` datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`      varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`teacher_id`) USING BTREE,
  UNIQUE KEY `uk_staff_no` (`staff_no`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师建档表';

-- 初始化数据（示例，可删除）
INSERT INTO `hms_teacher` (`staff_no`, `name`, `gender`, `title`, `phone`, `hire_date`, `department`, `create_by`, `create_time`, `remark`)
SELECT 'T0001', '示例教师', '0', '副教授', '13800000000', '2015-09-01', '计算机学院', 'admin', sysdate(), '示例数据'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `hms_teacher` WHERE `staff_no` = 'T0001');

-- 校验
SELECT `teacher_id`, `staff_no`, `name`, `title`, `department`, `user_id` FROM `hms_teacher`;
