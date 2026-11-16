-- ----------------------------
-- 时间管理局表
-- add by CYQ 2026年8月4日
-- ----------------------------
drop table if exists hms_time_diary;
create table hms_time_diary (
  diary_id      bigint(20)      not null auto_increment    comment '主键',
  user_id       bigint(20)      default null               comment '记录人用户ID',
  diary_date    date            not null                   comment '日期',
  start_time    varchar(10)     not null                   comment '开始时间(HH:mm)',
  end_time      varchar(10)     not null                   comment '结束时间(HH:mm)',
  category      varchar(50)     default '工作'             comment '分类(工作/学习/生活/其他)',
  title         varchar(200)    not null                   comment '事项标题',
  description   varchar(500)    default null               comment '详细描述',
  duration_min  int(11)         default 0                  comment '时长(分钟)',
  create_by     varchar(64)     default ''                 comment '创建者',
  create_time   datetime                                   comment '创建时间',
  update_by     varchar(64)     default ''                 comment '更新者',
  update_time   datetime                                   comment '更新时间',
  remark        varchar(500)    default null               comment '备注',
  primary key (diary_id)
) engine=innodb auto_increment=1 comment = '时间管理局表';
