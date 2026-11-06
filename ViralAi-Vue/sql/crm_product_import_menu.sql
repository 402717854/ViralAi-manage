-- ----------------------------
-- 模型产品（crm/product）批量导入按钮权限
-- 说明：仅新增一个 F 类型（按钮）菜单，不建表、不改数据。
-- 执行前请确认「模型产品」菜单已存在（perms = 'crm:product:list'）。
-- 数据库：MySQL 5.7+ / 8.0
-- ----------------------------

-- 1、定位父菜单（模型产品列表页）
set @parent_id = (select menu_id from sys_menu where perms = 'crm:product:list' limit 1);

-- 2、幂等插入：已存在 crm:product:import 时跳过
set @exists = (select count(1) from sys_menu where perms = 'crm:product:import');
set @menu_id = (select ifnull(max(menu_id), 0) + 1 from sys_menu);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select @menu_id, '模型产品导入', @parent_id, 10, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:product:import', '#', 'admin', sysdate(), '', null, '模型产品批量导入按钮'
from dual
where @exists = 0
  and @parent_id is not null;

-- 3、授权提示（按需执行，不要在脚本里自动放开给全部角色）：
--    菜单管理 → 角色管理 → 勾选「模型产品导入」按钮权限即可。
--    admin 超管默认拥有全部权限，无需额外配置。
--
-- 4、校验
-- select menu_id, menu_name, parent_id, menu_type, perms from sys_menu where perms like 'crm:product:%';
