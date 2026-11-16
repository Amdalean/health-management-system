-- ----------------------------
-- 财务汇总收入构成表
-- add by CYQ 2026-09-21 说明: 维护每月收入构成（工资/项目回款/炒股收益/基金收益等），合计回写 hsm_summary.income
-- ----------------------------
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `hsm_summary_income`;
CREATE TABLE `hsm_summary_income` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `summary_id` bigint NOT NULL COMMENT '关联财务汇总主表ID',
  `item_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '收入项目',
  `amount` decimal(15,2) NULL DEFAULT 0.00 COMMENT '金额',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_summary_id`(`summary_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '财务汇总收入构成表';

SET FOREIGN_KEY_CHECKS = 1;
