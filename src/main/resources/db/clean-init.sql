SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE `notification`;
TRUNCATE TABLE `login_log`;
TRUNCATE TABLE `log`;
TRUNCATE TABLE `contract_process`;
TRUNCATE TABLE `contract_state`;
TRUNCATE TABLE `contract_attachment`;
TRUNCATE TABLE `contract_version`;
TRUNCATE TABLE `contract_template`;
TRUNCATE TABLE `contract`;
TRUNCATE TABLE `customer`;
TRUNCATE TABLE `user_role`;
TRUNCATE TABLE `user`;
TRUNCATE TABLE `role`;
TRUNCATE TABLE `function`;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO `role` (`id`, `name`, `description`, `functions`) VALUES
(1, 'ADMIN', '系统管理员，拥有全部权限', '1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26'),
(2, 'OPERATOR', '合同操作员，负责合同流程操作', '1,2,3,4,5,6,7,8');

INSERT INTO `function` (`id`, `num`, `name`, `url`, `description`, `parent_id`, `sort_order`) VALUES
(1, 'F001', '起草合同', '/contract/draft', '起草新合同', 0, 1),
(2, 'F002', '定稿合同', '/contract/finalize', '定稿合同', 0, 2),
(3, 'F003', '查询合同', '/query/contract', '查询合同信息', 0, 3),
(4, 'F004', '删除合同', '', '删除合同', 0, 4),
(5, 'F005', '会签合同', '/contract/countersign', '会签合同', 0, 5),
(6, 'F006', '审批合同', '/contract/approve', '审批合同', 0, 6),
(7, 'F007', '签订合同', '/contract/sign', '签订合同', 0, 7),
(8, 'F008', '分配会签', '', '分配会签人员', 0, 8),
(9, 'F009', '分配审批', '', '分配审批人员', 0, 9),
(10, 'F010', '分配签订', '', '分配签订人员', 0, 10),
(11, 'F011', '流程查询', '/query/process', '查询合同流程', 0, 11),
(12, 'F012', '用户管理', '/system/users', '管理用户', 0, 12),
(13, 'F013', '角色管理', '/system/roles', '管理角色', 0, 13),
(14, 'F014', '基础信息维护', '', '维护基础数据', 0, 14),
(15, 'F015', '新增用户', '', '新增用户', 0, 15),
(16, 'F016', '编辑用户', '', '编辑用户', 0, 16),
(17, 'F017', '查询用户', '', '查询用户', 0, 17),
(18, 'F018', '删除用户', '', '删除用户', 0, 18),
(19, 'F019', '新增角色', '', '新增角色', 0, 19),
(20, 'F020', '编辑角色', '', '编辑角色', 0, 20),
(21, 'F021', '查询角色', '', '查询角色', 0, 21),
(22, 'F022', '删除角色', '', '删除角色', 0, 22),
(23, 'F023', '新增功能', '', '新增功能', 0, 23),
(24, 'F024', '编辑功能', '', '编辑功能', 0, 24),
(25, 'F025', '查询功能', '', '查询功能', 0, 25),
(26, 'F026', '删除功能', '', '删除功能', 0, 26),
(27, 'F027', '新增客户', '', '新增客户', 0, 27),
(28, 'F028', '编辑客户', '', '编辑客户', 0, 28),
(29, 'F029', '查询客户', '', '查询客户', 0, 29),
(30, 'F030', '删除客户', '', '删除客户', 0, 30);

INSERT INTO `user_role` (`id`, `user_id`, `role_id`) VALUES
(1, 1, 1);

INSERT INTO `user` (`id`, `username`, `password`, `role_id`, `status`) VALUES
(1, 'admin', '$2a$10$lK6DzK40yyvGeZ6nNlWwY.hjIAJfPCkdqzaE5wPncYWB3fMk9/CdG', 1, 1);
