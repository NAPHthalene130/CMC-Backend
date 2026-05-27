CREATE DATABASE IF NOT EXISTS cmc DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cmc;

CREATE TABLE IF NOT EXISTS `role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(40) NOT NULL,
  `description` VARCHAR(100) NULL,
  `functions` VARCHAR(1000) NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `function` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `num` VARCHAR(10) NOT NULL,
  `name` VARCHAR(40) NOT NULL,
  `url` VARCHAR(100) NULL,
  `description` VARCHAR(100) NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_function_num` (`num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(40) NOT NULL,
  `password` VARCHAR(100) NOT NULL,
  `role_id` BIGINT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `customer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `num` VARCHAR(20) NOT NULL,
  `name` VARCHAR(40) NOT NULL,
  `address` VARCHAR(200) NOT NULL,
  `tel` VARCHAR(20) NOT NULL,
  `fax` VARCHAR(20) NULL,
  `code` VARCHAR(10) NULL,
  `bank` VARCHAR(50) NULL,
  `account` VARCHAR(50) NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_num` (`num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `contract` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `num` VARCHAR(20) NOT NULL,
  `name` VARCHAR(40) NOT NULL,
  `customer_id` BIGINT NULL,
  `begin_time` DATE NOT NULL,
  `end_time` DATE NOT NULL,
  `content` TEXT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_num` (`num`),
  KEY `idx_contract_user_id` (`user_id`),
  KEY `idx_contract_customer_id` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `contract_state` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `contract_id` BIGINT NOT NULL,
  `type` INT NOT NULL COMMENT '1起草,2会签完成,3定稿完成,4审批完成,5签订完成',
  `time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_contract_state_contract_id` (`contract_id`),
  KEY `idx_contract_state_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `contract_process` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `contract_id` BIGINT NOT NULL,
  `type` INT NOT NULL COMMENT '1会签,2审批,3签订',
  `state` INT NOT NULL COMMENT '0未完成,1已完成,2已否决',
  `user_id` BIGINT NOT NULL,
  `content` TEXT NULL,
  `time` DATETIME NULL,
  PRIMARY KEY (`id`),
  KEY `idx_process_contract_id` (`contract_id`),
  KEY `idx_process_user_type_state` (`user_id`, `type`, `state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `contract_attachment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `contract_id` BIGINT NOT NULL,
  `file_name` VARCHAR(100) NOT NULL,
  `path` VARCHAR(200) NOT NULL,
  `type` VARCHAR(20) NOT NULL,
  `upload_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_attachment_contract_id` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `username` VARCHAR(40) NULL,
  `content` TEXT NOT NULL,
  `time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_log_time` (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
