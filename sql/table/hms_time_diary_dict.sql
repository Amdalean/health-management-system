-- ----------------------------
-- 时间管理局分类字典
-- add by CYQ 2026年8月6日
-- ----------------------------

-- 字典类型
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time)
VALUES ('时间管理局分类', 'time_diary_category', '0', 'admin', NOW());

-- 字典数据
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time)
VALUES
(1, '工作', '工作', 'time_diary_category', 'primary', 'N', '0', 'admin', NOW()),
(2, '学习', '学习', 'time_diary_category', 'success', 'N', '0', 'admin', NOW()),
(3, '生活', '生活', 'time_diary_category', 'warning', 'N', '0', 'admin', NOW()),
(4, '其他', '其他', 'time_diary_category', 'info', 'N', '0', 'admin', NOW());
