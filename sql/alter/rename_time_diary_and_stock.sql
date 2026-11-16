-- ----------------------------
-- 菜单/字典改名：时间日记→时间管理局，量化交易→天才交易员系统
-- add by CYQ $(new Date().toISOString().slice(0,10))
-- ----------------------------

-- 时间日记 → 时间管理局（菜单名）
UPDATE sys_menu SET menu_name = '时间管理局'     WHERE menu_name = '时间日记'     AND perms = 'main:timeDiary:list';
UPDATE sys_menu SET menu_name = '时间管理局查询' WHERE menu_name = '时间日记查询' AND perms = 'main:timeDiary:query';
UPDATE sys_menu SET menu_name = '时间管理局新增' WHERE menu_name = '时间日记新增' AND perms = 'main:timeDiary:add';
UPDATE sys_menu SET menu_name = '时间管理局修改' WHERE menu_name = '时间日记修改' AND perms = 'main:timeDiary:edit';
UPDATE sys_menu SET menu_name = '时间管理局删除' WHERE menu_name = '时间日记删除' AND perms = 'main:timeDiary:remove';
UPDATE sys_menu SET menu_name = '时间管理局导出' WHERE menu_name = '时间日记导出' AND perms = 'main:timeDiary:export';

-- 量化交易 → 天才交易员系统（菜单名）
UPDATE sys_menu SET menu_name = '天才交易员系统' WHERE menu_name = '量化交易' AND perms = 'main:stock:list';
UPDATE sys_menu SET menu_name = '天才交易员系统查询' WHERE menu_name = '量化交易查询' AND perms = 'main:stock:query';
UPDATE sys_menu SET menu_name = '天才交易员系统新增' WHERE menu_name = '量化交易新增' AND perms = 'main:stock:add';
UPDATE sys_menu SET menu_name = '天才交易员系统修改' WHERE menu_name = '量化交易修改' AND perms = 'main:stock:edit';
UPDATE sys_menu SET menu_name = '天才交易员系统删除' WHERE menu_name = '量化交易删除' AND perms = 'main:stock:remove';
UPDATE sys_menu SET menu_name = '天才交易员系统导出' WHERE menu_name = '量化交易导出' AND perms = 'main:stock:export';

-- 时间日记分类 → 时间管理局分类（字典名）
UPDATE sys_dict_type SET dict_name = '时间管理局分类' WHERE dict_name = '时间日记分类' AND dict_type = 'time_diary_category';

-- 确认影响行数
SELECT '菜单更新完成' AS result;
