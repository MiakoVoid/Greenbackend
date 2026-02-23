-- 创建数据库并设置字符集
CREATE DATABASE IF NOT EXISTS greenfinance DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE greenfinance;

-- 1. 用户表（头像存储本地路径）
CREATE TABLE `user` (
                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                        `username` varchar(50) NOT NULL COMMENT '用户名（唯一）',
                        `password` varchar(100) NOT NULL COMMENT '密码（BCrypt加密存储）',
                        `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
                        `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
                        `avatar_path` varchar(255) DEFAULT NULL COMMENT '头像本地存储路径（相对路径，如"avatars/10001.png"）',
                        `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
                        `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        `deletion_time` datetime DEFAULT NULL COMMENT '注销时间（申请注销的时间）',
                       `deletion_status` tinyint NOT NULL DEFAULT 0 COMMENT '注销状态：0-未申请注销，1-已申请注销（冷静期内），2-已注销',
                        INDEX `idx_deletion_status` (`deletion_status`) COMMENT '按注销状态查询用户',
                        INDEX `idx_deletion_time` (`deletion_time`) COMMENT '按注销时间查询用户',
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_username` (`username`),
                        UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 2. 主分类表（图标支持本地路径或系统资源）
CREATE TABLE `category` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主分类ID',
                            `user_id` bigint DEFAULT NULL COMMENT '用户ID（null表示系统默认分类）',
                            `name` varchar(50) NOT NULL COMMENT '分类名称',
                            `category_Icon` varchar(100) NOT NULL COMMENT '图标标识：系统图标用"res:资源名"（如"res:ic_food"），本地图标用"file:相对路径"（如"file:icons/custom1.png"）',
                            `type` tinyint NOT NULL COMMENT '类型：1-支出，2-收入',
                            `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序序号（升序排列）',
                            `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`id`),
                            KEY `idx_user_id` (`user_id`) COMMENT '按用户查询分类'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主分类表';

-- 3. 子分类表（同主分类，复用图标标识规则）
CREATE TABLE `sub_category` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '子分类ID',
                                `category_id` bigint NOT NULL COMMENT '关联主分类ID',
                                `user_id` bigint DEFAULT NULL COMMENT '用户ID（null表示系统默认分类）',
                                `name` varchar(50) NOT NULL COMMENT '子分类名称',
                                `category_Icon` varchar(100) DEFAULT NULL COMMENT '图标标识（规则同主分类，为空时继承主分类图标）',
                                `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序序号（升序排列）',
                                `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                PRIMARY KEY (`id`),
                                KEY `idx_category_id` (`category_id`) COMMENT '按主分类查询子分类',
                                KEY `idx_user_id` (`user_id`) COMMENT '按用户查询子分类',
                                CONSTRAINT `fk_sub_category_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='子分类表';

-- 4. 分类关键词表（不变）
CREATE TABLE `category_keyword` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关键词ID（自增主键）',
                                    `user_id` bigint NOT NULL COMMENT '所属用户ID（0=系统默认关键词，非0=用户自定义关键词）',
                                    `keyword` varchar(100) NOT NULL COMMENT '关键词（如“肯德基”“外卖”“京东”，支持中文/英文/数字）',
                                    `match_value` varchar(100) NOT NULL COMMENT '匹配值（关联的核心值：主分类名/子分类名/商户名，如“餐饮”“快餐”“肯德基”）',
                                    `type` tinyint NOT NULL COMMENT '匹配类型：1=主分类关键词，2=子分类关键词，3=商户名关键词',
                                    `category_id` bigint DEFAULT NULL COMMENT '主分类ID（关联category表，type=1/2时必填）',
                                    `sub_category_id` bigint DEFAULT NULL COMMENT '子分类ID（关联sub_category表，type=2时必填，type=1/3时可为NULL）',
                                    `weight` int NOT NULL DEFAULT 50 COMMENT '匹配权重（1-100，越高优先级越高，解决关键词冲突）',
                                    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1=启用，0=禁用（软删除，避免误删恢复麻烦）',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（自动填充）',
                                    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（自动更新）',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_user_keyword_type` (`user_id`,`keyword`,`type`) COMMENT '唯一约束：同一用户下，相同关键词+类型不能重复',
                                    KEY `idx_user_type` (`user_id`,`type`) COMMENT '按“用户+类型”查询关键词（如查询用户自定义的商户名关键词）',
                                    KEY `idx_category` (`category_id`,`sub_category_id`) COMMENT '按“主分类+子分类”查询关键词（用于分类维度统计）',
                                    KEY `idx_keyword_status` (`keyword`,`status`) COMMENT '关键词匹配时快速查询（过滤禁用关键词）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类关键词表（存储主分类、子分类、商户名的匹配关键词，支持系统默认和用户自定义）';

-- 5. 账单表（不变，无图片存储）
CREATE TABLE `bill` (
                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '账单ID',
                        `user_id` bigint NOT NULL COMMENT '所属用户ID',
                        `amount` decimal(10,2) NOT NULL COMMENT '最终金额（自动计算：original_amount - refund_amount）',
                        `original_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '原始金额',
                        `refund_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
                        `type` tinyint NOT NULL COMMENT '类型：1-支出，2-收入',
                        `category_id` bigint NOT NULL COMMENT '主分类ID',
                        `sub_category_id` bigint DEFAULT NULL COMMENT '子分类ID',
                        `merchant` varchar(100) DEFAULT NULL COMMENT '商户名称（如"星巴克"）',
                        `remark` varchar(500) DEFAULT NULL COMMENT '备注信息',
                        `bill_time` datetime NOT NULL COMMENT '收支发生时间',
                        `payment_method` varchar(50) DEFAULT NULL COMMENT '支付方式（如"微信支付"）',
                        `order_number` varchar(32) DEFAULT NULL COMMENT '订单号',
                        `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        PRIMARY KEY (`id`),
                        KEY `idx_user_time` (`user_id`,`bill_time`) COMMENT '按用户+时间查询账单',
                        KEY `idx_user_category` (`user_id`,`category_id`,`sub_category_id`) COMMENT '按用户+分类查询账单',
                        CONSTRAINT `fk_bill_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`),
                        CONSTRAINT `fk_bill_sub_category` FOREIGN KEY (`sub_category_id`) REFERENCES `sub_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单记录表';

-- 6. 预算表（不变）
CREATE TABLE `budget` (
                          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '预算ID',
                          `user_id` bigint NOT NULL COMMENT '所属用户ID',
                          `amount` decimal(10,2) NOT NULL COMMENT '月度预算总金额',
                          `year` int NOT NULL COMMENT '年份（如2023）',
                          `month` int NOT NULL COMMENT '月份（1-12）',
                          `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `uk_user_year_month` (`user_id`,`year`,`month`) COMMENT '用户每月预算唯一',
                          KEY `idx_user` (`user_id`) COMMENT '按用户查询预算'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度预算表';

-- 7. 预计支出表（不变）
CREATE TABLE `expected_expense` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '预计支出ID',
                                    `user_id` bigint NOT NULL COMMENT '所属用户ID',
                                    `amount` decimal(10,2) NOT NULL COMMENT '预计金额',
                                    `category_id` bigint NOT NULL COMMENT '主分类ID',
                                    `sub_category_id` bigint DEFAULT NULL COMMENT '子分类ID',
                                    `remark` varchar(500) DEFAULT NULL COMMENT '备注（如"10月房租"）',
                                    `due_date` date NOT NULL COMMENT '预计支付日期',
                                    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-待支付，2-已支付，3-已取消',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_user_due_date` (`user_id`,`due_date`) COMMENT '按用户+日期查询预计支出',
                                    KEY `idx_user_status` (`user_id`,`status`) COMMENT '按用户+状态查询预计支出',
                                    CONSTRAINT `fk_expected_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`),
                                    CONSTRAINT `fk_expected_sub_category` FOREIGN KEY (`sub_category_id`) REFERENCES `sub_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预计支出表';

-- 8. 系统配置表
CREATE TABLE `system_config` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID（自增主键）',
                                 `config_key` varchar(50) NOT NULL COMMENT '配置键，需与user_id组合唯一',
                                 `config_value` varchar(2000) NOT NULL COMMENT '配置值（支持字符串/JSON格式，扩展长度适配复杂配置）',
                                 `remark` varchar(500) DEFAULT NULL COMMENT '配置说明',
                                 `user_id` bigint DEFAULT NULL COMMENT '用户ID（null表示系统默认配置，非null表示该用户的自定义配置）',
                                 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_system_config_user_id` (`user_id`) COMMENT '按用户ID查询配置，优化查询效率',
                                 UNIQUE KEY `uk_system_config_user_key` (`user_id`, `config_key`) COMMENT '联合唯一索引：同一用户下配置键唯一，系统默认配置（user_id=null）也唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表（存储系统全局配置或用户自定义配置）';
INSERT INTO system_config (user_id, config_key, config_value, remark) VALUES
                                                                          (NULL, 'bill_month_start_day', '1', '账单月起始日，默认为每月1日'),
                                                                          (NULL, 'week_start_day', '1', '日期周起始日，1=周日，2=周一，默认为周日')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), remark = VALUES(remark);
INSERT INTO system_config (config_key, config_value, remark) VALUES
    ('ai_recognition_enabled', 'true', '智能识别开关，控制是否启用AI智能识别功能')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), remark = VALUES(remark);