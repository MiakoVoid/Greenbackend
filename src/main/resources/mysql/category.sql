-- 先清空表数据（若需要，首次执行可保留，后续更新注释掉）
-- TRUNCATE TABLE sub_category;
-- TRUNCATE TABLE category;

use `greenfinance`;
-- 插入支出类主分类（type=1）
INSERT INTO `category` (`user_id`, `name`, `icon_identifier`, `type`, `sort_order`) VALUES
                                                                                        (NULL, '餐饮', 'res:restaurant', 1, 1),
                                                                                        (NULL, '交通', 'res:commute', 1, 2),
                                                                                        (NULL, '住房', 'res:home', 1, 3),
                                                                                        (NULL, '购物', 'res:shopping_cart', 1, 4),
                                                                                        (NULL, '娱乐', 'res:movie', 1, 5),
                                                                                        (NULL, '医疗健康', 'res:medical_services', 1, 6),
                                                                                        (NULL, '教育', 'res:school', 1, 7),
                                                                                        (NULL, '人情往来', 'res:card_giftcard', 1, 8),
                                                                                        (NULL, '其他支出', 'res:money_off', 1, 9);

-- 插入收入类主分类（type=2）
INSERT INTO `category` (`user_id`, `name`, `icon_identifier`, `type`, `sort_order`) VALUES
                                                                                        (NULL, '工资', 'res:work', 2, 1),
                                                                                        (NULL, '理财收益', 'res:account_balance', 2, 2),
                                                                                        (NULL, '兼职收入', 'res:assignment', 2, 3),
                                                                                        (NULL, '礼金红包', 'res:monetization_on', 2, 4),
                                                                                        (NULL, '其他收入', 'res:attach_money', 2, 5);

-- 插入子分类（关联主分类ID，继承或自定义图标）
-- 餐饮子分类（主分类ID=1）
INSERT INTO `sub_category` (`category_id`, `user_id`, `name`, `icon_identifier`, `sort_order`) VALUES
                                                                                                   (1, NULL, '早餐', NULL, 1), -- 继承主分类图标
                                                                                                   (1, NULL, '午餐', NULL, 2),
                                                                                                   (1, NULL, '晚餐', NULL, 3),
                                                                                                   (1, NULL, '零食饮料', 'res:local_cafe', 4);

-- 交通子分类（主分类ID=2）
INSERT INTO `sub_category` (`category_id`, `user_id`, `name`, `icon_identifier`, `sort_order`) VALUES
                                                                                                   (2, NULL, '公交地铁', 'res:directions_bus', 1),
                                                                                                   (2, NULL, '打车', 'res:local_taxi', 2),
                                                                                                   (2, NULL, '加油', 'res:local_gas_station', 3),
                                                                                                   (2, NULL, '停车费', 'res:local_parking', 4);

-- 住房子分类（主分类ID=3）
INSERT INTO `sub_category` (`category_id`, `user_id`, `name`, `icon_identifier`, `sort_order`) VALUES
                                                                                                   (3, NULL, '房租', NULL, 1), -- 继承主分类图标
                                                                                                   (3, NULL, '水电煤', 'res:water_drop', 2), -- 水图标（电可用res:electric_bolt，这里统一用水图标）
                                                                                                   (3, NULL, '物业费', NULL, 3);

-- 购物子分类（主分类ID=4）
INSERT INTO `sub_category` (`category_id`, `user_id`, `name`, `icon_identifier`, `sort_order`) VALUES
                                                                                                   (4, NULL, '服饰鞋包', 'res:checkroom', 1),
                                                                                                   (4, NULL, '日用品', 'res:cleaning_services', 2),
                                                                                                   (4, NULL, '数码家电', 'res:devices', 3);

-- 娱乐子分类（主分类ID=5）
INSERT INTO `sub_category` (`category_id`, `user_id`, `name`, `icon_identifier`, `sort_order`) VALUES
                                                                                                   (5, NULL, '电影演出', 'res:movie', 1), -- 复用主分类图标
                                                                                                   (5, NULL, '游戏充值', 'res:games', 2),
                                                                                                   (5, NULL, '旅游', 'res:flight', 3);

-- 医疗健康子分类（主分类ID=6）
INSERT INTO `sub_category` (`category_id`, `user_id`, `name`, `icon_identifier`, `sort_order`) VALUES
                                                                                                   (6, NULL, '药店买药', NULL, 1), -- 继承主分类图标
                                                                                                   (6, NULL, '医院诊疗', 'res:local_hospital', 2),
                                                                                                   (6, NULL, '健身', 'res:fitness_center', 3);

-- 教育子分类（主分类ID=7）
INSERT INTO `sub_category` (`category_id`, `user_id`, `name`, `icon_identifier`, `sort_order`) VALUES
                                                                                                   (7, NULL, '书籍', 'res:menu_book', 1),
                                                                                                   (7, NULL, '培训课程', 'res:class', 2),
                                                                                                   (7, NULL, '学费', NULL, 3); -- 继承主分类图标

-- 人情往来子分类（主分类ID=8）
INSERT INTO `sub_category` (`category_id`, `user_id`, `name`, `icon_identifier`, `sort_order`) VALUES
                                                                                                   (8, NULL, '红包', 'res:card_giftcard', 1), -- 复用主分类图标
                                                                                                   (8, NULL, '礼物', NULL, 2), -- 继承主分类图标
                                                                                                   (8, NULL, '聚餐AA', NULL, 3);

-- 其他支出子分类（主分类ID=9）
INSERT INTO `sub_category` (`category_id`, `user_id`, `name`, `icon_identifier`, `sort_order`) VALUES
                                                                                                   (9, NULL, '杂项', NULL, 1), -- 继承主分类图标
                                                                                                   (9, NULL, '罚款', 'res:gavel', 2);

-- 工资子分类（主分类ID=10）
INSERT INTO `sub_category` (`category_id`, `user_id`, `name`, `icon_identifier`, `sort_order`) VALUES
                                                                                                   (10, NULL, '基本工资', NULL, 1), -- 继承主分类图标
                                                                                                   (10, NULL, '奖金', 'res:card_giftcard', 2),
                                                                                                   (10, NULL, '补贴', 'res:attach_money', 3);

-- 理财收益子分类（主分类ID=11）
INSERT INTO `sub_category` (`category_id`, `user_id`, `name`, `icon_identifier`, `sort_order`) VALUES
                                                                                                   (11, NULL, '存款利息', 'res:account_balance_wallet', 1),
                                                                                                   (11, NULL, '基金收益', NULL, 2), -- 继承主分类图标
                                                                                                   (11, NULL, '股票收益', 'res:trending_up', 3);

-- 兼职收入子分类（主分类ID=12）
INSERT INTO `sub_category` (`category_id`, `user_id`, `name`, `icon_identifier`, `sort_order`) VALUES
                                                                                                   (12, NULL, '副业', NULL, 1), -- 继承主分类图标
                                                                                                   (12, NULL, '劳务报酬', 'res:paid', 2);

-- 礼金红包子分类（主分类ID=13）
INSERT INTO `sub_category` (`category_id`, `user_id`, `name`, `icon_identifier`, `sort_order`) VALUES
                                                                                                   (13, NULL, '节日红包', NULL, 1), -- 继承主分类图标
                                                                                                   (13, NULL, '礼金', 'res:monetization_on', 2); -- 复用主分类图标

-- 其他收入子分类（主分类ID=14）
INSERT INTO `sub_category` (`category_id`, `user_id`, `name`, `icon_identifier`, `sort_order`) VALUES
                                                                                                   (14, NULL, '退款', 'res:arrow_back', 1),
                                                                                                   (14, NULL, '杂项', NULL, 2); -- 继承主分类图标