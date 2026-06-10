-- ============================================
-- 合同管理系统 (CMS) 数据库初始化脚本
-- Database: cmc
-- Version: 1.0
-- ============================================

CREATE DATABASE IF NOT EXISTS cms_fastDev2 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cms_fastDev2;

-- ----------------------------
-- 1. 用户表
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(40) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `role_id` BIGINT DEFAULT NULL COMMENT '角色ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像路径',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `must_change_password` TINYINT NOT NULL DEFAULT 0 COMMENT '是否需强制修改密码',
    `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除 0未删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 2. 角色表
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `name` VARCHAR(40) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(100) DEFAULT NULL COMMENT '角色描述',
    `functions` VARCHAR(500) DEFAULT NULL COMMENT '功能ID列表，逗号分隔',
    `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ----------------------------
-- 3. 功能表
-- ----------------------------
DROP TABLE IF EXISTS `function`;
CREATE TABLE `function` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '功能ID',
    `num` VARCHAR(10) DEFAULT NULL COMMENT '功能编号',
    `name` VARCHAR(40) NOT NULL COMMENT '功能名称',
    `url` VARCHAR(100) DEFAULT NULL COMMENT '功能URL',
    `description` VARCHAR(100) DEFAULT NULL COMMENT '功能描述',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父级ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='功能表';

-- ----------------------------
-- 4. 用户角色关联表
-- ----------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ----------------------------
-- 5. 客户表
-- ----------------------------
DROP TABLE IF EXISTS `customer`;
CREATE TABLE `customer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '客户ID',
    `num` VARCHAR(20) DEFAULT NULL COMMENT '客户编号',
    `name` VARCHAR(40) NOT NULL COMMENT '客户名称',
    `address` VARCHAR(200) DEFAULT NULL COMMENT '地址',
    `tel` VARCHAR(20) DEFAULT NULL COMMENT '电话',
    `fax` VARCHAR(20) DEFAULT NULL COMMENT '传真',
    `code` VARCHAR(10) DEFAULT NULL COMMENT '邮编',
    `bank` VARCHAR(50) DEFAULT NULL COMMENT '银行名称',
    `account` VARCHAR(50) DEFAULT NULL COMMENT '银行账户',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户表';

-- ----------------------------
-- 6. 合同表
-- ----------------------------
DROP TABLE IF EXISTS `contract`;
CREATE TABLE `contract` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '合同ID',
    `num` VARCHAR(20) NOT NULL COMMENT '合同编号',
    `name` VARCHAR(40) NOT NULL COMMENT '合同名称',
    `customer_id` BIGINT DEFAULT NULL COMMENT '客户ID',
    `begin_time` DATE DEFAULT NULL COMMENT '开始时间',
    `end_time` DATE DEFAULT NULL COMMENT '结束时间',
    `content` TEXT COMMENT '合同内容',
    `user_id` BIGINT NOT NULL COMMENT '起草人ID',
    `template_id` BIGINT DEFAULT NULL COMMENT '模板ID',
    `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_user_id` (`user_id`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_num` (`num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同表';

-- ----------------------------
-- 7. 合同操作流程表
-- ----------------------------
DROP TABLE IF EXISTS `contract_process`;
CREATE TABLE `contract_process` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `contract_id` BIGINT NOT NULL COMMENT '合同ID',
    `type` INT NOT NULL COMMENT '操作类型：1会签 2审批 3签订',
    `state` INT NOT NULL DEFAULT 0 COMMENT '操作状态：0未完成 1已完成 2已否决',
    `user_id` BIGINT NOT NULL COMMENT '操作人ID',
    `content` TEXT COMMENT '操作内容/意见',
    `time` DATETIME DEFAULT NULL COMMENT '操作时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type_state` (`type`, `state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同操作流程表';

-- ----------------------------
-- 8. 合同状态表
-- ----------------------------
DROP TABLE IF EXISTS `contract_state`;
CREATE TABLE `contract_state` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `contract_id` BIGINT NOT NULL COMMENT '合同ID',
    `type` INT NOT NULL COMMENT '状态类型：1起草 2会签完成 3定稿完成 4审批完成 5签订完成',
    `time` DATETIME NOT NULL COMMENT '完成时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_contract_id` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同状态表';

-- ----------------------------
-- 9. 合同附件表
-- ----------------------------
DROP TABLE IF EXISTS `contract_attachment`;
CREATE TABLE `contract_attachment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `contract_id` BIGINT DEFAULT NULL COMMENT '合同ID',
    `file_name` VARCHAR(100) NOT NULL COMMENT '文件名',
    `path` VARCHAR(255) NOT NULL COMMENT '文件路径',
    `type` VARCHAR(20) DEFAULT NULL COMMENT '文件类型',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    `upload_time` DATETIME NOT NULL COMMENT '上传时间',
    `chunk_index` INT DEFAULT NULL COMMENT '分片索引',
    `chunk_total` INT DEFAULT NULL COMMENT '分片总数',
    `file_md5` VARCHAR(64) DEFAULT NULL COMMENT '文件MD5指纹',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1完整 0分片中',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_contract_id` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同附件表';

-- ----------------------------
-- 10. 日志表
-- ----------------------------
DROP TABLE IF EXISTS `log`;
CREATE TABLE `log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `username` VARCHAR(40) DEFAULT NULL COMMENT '操作人用户名',
    `content` TEXT COMMENT '操作内容',
    `time` DATETIME NOT NULL COMMENT '操作时间',
    `ip` VARCHAR(45) DEFAULT NULL COMMENT '操作IP',
    `type` VARCHAR(20) DEFAULT 'OPERATION' COMMENT '日志类型：OPERATION/LOGIN/SECURITY',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_user_id` (`user_id`),
    KEY `idx_time` (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日志表';

-- ----------------------------
-- 11. 登录日志表
-- ----------------------------
DROP TABLE IF EXISTS `login_log`;
CREATE TABLE `login_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `username` VARCHAR(40) DEFAULT NULL COMMENT '用户名',
    `ip` VARCHAR(45) DEFAULT NULL COMMENT '登录IP',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器信息',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1成功 0失败',
    `msg` VARCHAR(200) DEFAULT NULL COMMENT '消息',
    `time` DATETIME NOT NULL COMMENT '登录时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_user_id` (`user_id`),
    KEY `idx_time` (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- ----------------------------
-- 12. 合同模板表
-- ----------------------------
DROP TABLE IF EXISTS `contract_template`;
CREATE TABLE `contract_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '模板描述',
    `content` TEXT COMMENT '模板内容',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '模板分类',
    `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同模板表';

-- ----------------------------
-- 13. 通知消息表
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '接收人ID',
    `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content` VARCHAR(500) DEFAULT NULL COMMENT '通知内容',
    `type` VARCHAR(20) NOT NULL DEFAULT 'SYSTEM' COMMENT '类型：SYSTEM/CONTRACT/APPROVAL',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：1已读 0未读',
    `related_id` BIGINT DEFAULT NULL COMMENT '关联业务ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知消息表';

-- ----------------------------
-- 14. 合同版本表
-- ----------------------------
DROP TABLE IF EXISTS `contract_version`;
CREATE TABLE `contract_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `contract_id` BIGINT NOT NULL COMMENT '合同ID',
    `version_num` INT NOT NULL DEFAULT 1 COMMENT '版本号',
    `name` VARCHAR(40) DEFAULT NULL COMMENT '合同名称',
    `content` TEXT COMMENT '合同内容',
    `change_desc` VARCHAR(500) DEFAULT NULL COMMENT '变更描述',
    `create_user_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_contract_id` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同版本表';

-- ----------------------------
-- 种子数据：默认角色
-- ----------------------------
INSERT INTO `role` (`id`, `name`, `description`, `functions`) VALUES
(1, 'ADMIN', '系统管理员，拥有全部权限', '1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26'),
(2, 'OPERATOR', '合同操作员，负责合同流程操作', '1,2,3,4,5,6,7,8');

-- ----------------------------
-- 种子数据：默认功能列表
-- ----------------------------
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

-- ----------------------------
-- 种子数据：默认管理员
-- ----------------------------
INSERT INTO `user` (`id`, `username`, `password`, `role_id`, `status`) VALUES
(1, 'admin', '$2a$10$lK6DzK40yyvGeZ6nNlWwY.hjIAJfPCkdqzaE5wPncYWB3fMk9/CdG', 1, 1);
