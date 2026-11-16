-- ============================================================
-- 因子研究：菜单 + 定时任务注册（幂等）
-- 执行库：ry
-- add by CYQ 2026-11-13
--
-- 说明：
--   1. 菜单挂在「量化交易 / 天才交易员系统」（parent_id=20788）下；
--   2. 定时任务 invokeTarget = StockAnalysisTask.factorEval()，默认每日 02:00；
--   3. 页面只读，触发入口是「系统监控 → 定时任务 → 执行一次」，不提供 HTTP 直接触发。
-- ============================================================

-- 1) 菜单：因子研究（component=main/stock/factor，父菜单 20788）
INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, remark)
SELECT '因子研究', 20788, 4, 'factor', 'main/stock/factor', NULL, '',
       1, 0, 'C', '0', '0', 'main:stock:list', '#',
       'admin', NOW(), 'A股因子检验报告（只读）'
  FROM DUAL
 WHERE NOT EXISTS (
   SELECT 1 FROM sys_menu WHERE parent_id = 20788 AND path = 'factor'
 );

-- 2) 角色授权：已拥有父菜单(20788)的角色自动获得因子研究菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, m.menu_id
  FROM sys_role_menu rm
  JOIN sys_menu m ON m.parent_id = 20788 AND m.path = 'factor'
 WHERE rm.menu_id = 20788
   AND NOT EXISTS (
     SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = m.menu_id
   );

-- 3) 定时任务：A股因子评估（每日 02:00；status='0' 正常，'1' 暂停）
INSERT INTO sys_job
  (job_name, job_group, invoke_target, cron_expression, misfire_policy,
   concurrent, status, create_by, create_time, remark)
SELECT 'A股因子评估', 'SYSTEM', 'StockAnalysisTask.factorEval()', '0 0 2 * * ?', '3',
       '1', '0', 'admin', NOW(),
       '数据自动增量 + 因子检验；结果写 stock_factor_*，不触发交易'
  FROM DUAL
 WHERE NOT EXISTS (
   SELECT 1 FROM sys_job WHERE invoke_target = 'StockAnalysisTask.factorEval()'
 );

-- 4) 复核
-- SELECT menu_id, menu_name, parent_id, path, component FROM sys_menu WHERE path='factor';
-- SELECT job_id, job_name, invoke_target, cron_expression, status FROM sys_job WHERE job_name='A股因子评估';
