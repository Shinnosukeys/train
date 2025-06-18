
-- 创建用户金额表
drop table if exists `user_amount`;
create table `user_amount` (
    `id` bigint not null comment 'id',
    `member_id` bigint not null comment '会员id',
    `amount` decimal(10, 2) not null comment '用户金额',
    `create_time` datetime(3) comment '新增时间',
    `update_time` datetime(3) comment '修改时间',
    primary key (`id`),
    index `member_id_index` (`member_id`)
) engine=innodb default charset=utf8mb4 comment='用户金额';