DROP TABLE IF EXISTS contract_version;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS contract_template;
DROP TABLE IF EXISTS login_log;
DROP TABLE IF EXISTS log;
DROP TABLE IF EXISTS contract_attachment;
DROP TABLE IF EXISTS contract_state;
DROP TABLE IF EXISTS contract_process;
DROP TABLE IF EXISTS contract;
DROP TABLE IF EXISTS customer;
DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS function;
DROP TABLE IF EXISTS role;
DROP TABLE IF EXISTS user;

CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(40) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_id BIGINT,
    status TINYINT NOT NULL DEFAULT 1,
    avatar VARCHAR(255),
    email VARCHAR(100),
    phone VARCHAR(20),
    must_change_password TINYINT NOT NULL DEFAULT 0,
    deleted INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(40) NOT NULL,
    description VARCHAR(100),
    functions VARCHAR(500),
    deleted INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE function (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    num VARCHAR(10),
    name VARCHAR(40) NOT NULL,
    url VARCHAR(100),
    description VARCHAR(100),
    parent_id BIGINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    deleted INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL
);

CREATE TABLE customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    num VARCHAR(20),
    name VARCHAR(40) NOT NULL,
    address VARCHAR(200),
    tel VARCHAR(20),
    fax VARCHAR(20),
    code VARCHAR(10),
    bank VARCHAR(50),
    account VARCHAR(50),
    remark VARCHAR(500),
    deleted INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE contract (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    num VARCHAR(20) NOT NULL,
    name VARCHAR(40) NOT NULL,
    customer_id BIGINT,
    begin_time DATE,
    end_time DATE,
    content TEXT,
    user_id BIGINT NOT NULL,
    template_id BIGINT,
    state INT NOT NULL DEFAULT 1,
    deleted INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE contract_process (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    type INT NOT NULL,
    state INT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL,
    content TEXT,
    time DATETIME
);

CREATE TABLE contract_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    type INT NOT NULL,
    time DATETIME NOT NULL
);

CREATE TABLE contract_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT,
    file_name VARCHAR(100) NOT NULL,
    path VARCHAR(255) NOT NULL,
    type VARCHAR(20),
    file_size BIGINT,
    upload_time DATETIME NOT NULL,
    chunk_index INT,
    chunk_total INT,
    file_md5 VARCHAR(64),
    status TINYINT DEFAULT 1
);

CREATE TABLE log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(40),
    content TEXT,
    time DATETIME NOT NULL,
    ip VARCHAR(45),
    type VARCHAR(20) DEFAULT 'OPERATION'
);

CREATE TABLE login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(40),
    ip VARCHAR(45),
    user_agent VARCHAR(500),
    status TINYINT NOT NULL DEFAULT 1,
    msg VARCHAR(200),
    time DATETIME NOT NULL
);

CREATE TABLE contract_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    content TEXT,
    category VARCHAR(50),
    deleted INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(500),
    type VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    is_read TINYINT NOT NULL DEFAULT 0,
    related_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE contract_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    version_num INT NOT NULL DEFAULT 1,
    name VARCHAR(40),
    content TEXT,
    change_desc VARCHAR(500),
    create_user_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO role (id, name, description, functions) VALUES
(1, 'ADMIN', '系统管理员，拥有全部权限', '1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30'),
(2, 'OPERATOR', '合同操作员，负责合同流程操作', '1,2,3,5,6,7,11,27,29');

INSERT INTO function (id, num, name, url, description, parent_id, sort_order) VALUES
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

INSERT INTO user (id, username, password, role_id, status) VALUES
(1, 'admin', '$2a$10$jXd1hyfGVnq24UE5/KDxcejdSQ5Ie7YzEQxrvbPFAL31qoDdpLnoy', 1, 1),
(2, 'operator', '$2a$10$jXd1hyfGVnq24UE5/KDxcejdSQ5Ie7YzEQxrvbPFAL31qoDdpLnoy', 2, 1),
(3, 'reviewer', '$2a$10$jXd1hyfGVnq24UE5/KDxcejdSQ5Ie7YzEQxrvbPFAL31qoDdpLnoy', 2, 1),
(4, 'approver', '$2a$10$jXd1hyfGVnq24UE5/KDxcejdSQ5Ie7YzEQxrvbPFAL31qoDdpLnoy', 2, 1),
(5, 'signer', '$2a$10$jXd1hyfGVnq24UE5/KDxcejdSQ5Ie7YzEQxrvbPFAL31qoDdpLnoy', 2, 1);

INSERT INTO customer (id, num, name, address, tel, fax, code, bank, account) VALUES
(1, 'KH-001', '青山科技有限公司', '深圳市南山区科技园', '0755-88889999', '0755-88889998', '518000', '招商银行深圳分行', '755900000000001'),
(2, 'KH-002', '远海贸易有限公司', '广州市天河区珠江新城', '020-66668888', '020-66668887', '510000', '中国银行广州分行', '440100000000002');

INSERT INTO contract_template (id, name, description, content, category) VALUES
(1, '标准采购合同', '适用于普通采购业务', '甲乙双方就采购事项达成如下协议：\n一、标的物...\n二、交付与验收...\n三、付款方式...', '采购'),
(2, '服务合作合同', '适用于服务外包与合作', '甲乙双方就服务合作事项达成如下协议：\n一、服务内容...\n二、服务期限...\n三、费用结算...', '服务');

INSERT INTO contract (id, num, name, customer_id, begin_time, end_time, content, user_id, state, create_time, update_time) VALUES
(1, 'HT-DEMO-001', '办公设备采购合同', 1, CURRENT_DATE, DATEADD('DAY', 90, CURRENT_DATE), '采购电脑、打印机及配套设备。', 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'HT-DEMO-002', '运维服务合作合同', 2, CURRENT_DATE, DATEADD('DAY', 180, CURRENT_DATE), '提供系统运维、巡检和应急响应服务。', 2, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO contract_process (contract_id, type, state, user_id, time) VALUES
(1, 1, 0, 3, CURRENT_TIMESTAMP),
(1, 2, 0, 4, CURRENT_TIMESTAMP),
(1, 3, 0, 5, CURRENT_TIMESTAMP),
(2, 1, 1, 3, CURRENT_TIMESTAMP),
(2, 2, 0, 4, CURRENT_TIMESTAMP),
(2, 3, 0, 5, CURRENT_TIMESTAMP);

INSERT INTO contract_state (contract_id, type, time) VALUES
(1, 1, CURRENT_TIMESTAMP),
(2, 1, CURRENT_TIMESTAMP),
(2, 2, CURRENT_TIMESTAMP);
