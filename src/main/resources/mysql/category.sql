-- ====================================================
-- 清理原有分类数据（请谨慎执行，确保已备份）
-- ====================================================
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE greenfinance.sub_category;
TRUNCATE TABLE greenfinance.category;
SET FOREIGN_KEY_CHECKS = 1;

-- ====================================================
-- 1. 插入主分类 (支出 1-11, 收入 20-27)
-- ====================================================
INSERT INTO greenfinance.category (id, user_id, name, category_Icon, type, sort_order, create_time, update_time) VALUES
-- 支出
(1, null, '餐饮', 'res:ic_category_food', 1, 1, NOW(), NOW()),
(2, null, '交通', 'res:ic_category_transport', 1, 2, NOW(), NOW()),
(3, null, '购物', 'res:ic_category_shopping', 1, 3, NOW(), NOW()),
(4, null, '娱乐', 'res:ic_category_entertainment', 1, 4, NOW(), NOW()),
(5, null, '医疗', 'res:ic_category_medical', 1, 5, NOW(), NOW()),
(6, null, '教育', 'res:ic_category_education', 1, 6, NOW(), NOW()),
(7, null, '居家', 'res:ic_category_housing', 1, 7, NOW(), NOW()),
(8, null, '通讯', 'res:ic_category_communication', 1, 8, NOW(), NOW()),
(9, null, '宠物', 'res:ic_category_pet', 1, 9, NOW(), NOW()),
(10, null, '人情', 'res:ic_category_gift', 1, 10, NOW(), NOW()),
(11, null, '其他', 'res:ic_category_other', 1, 11, NOW(), NOW()),

-- 收入
(20, null, '工资', 'res:ic_category_salary', 2, 20, NOW(), NOW()),
(21, null, '奖金', 'res:ic_category_bonus', 2, 21, NOW(), NOW()),
(22, null, '投资', 'res:ic_category_investment', 2, 22, NOW(), NOW()),
(23, null, '兼职', 'res:ic_category_parttime', 2, 23, NOW(), NOW()),
(24, null, '退款', 'res:ic_category_refund', 2, 24, NOW(), NOW()),
(25, null, '人情', 'res:ic_category_gift', 2, 25, NOW(), NOW()),
(26, null, '生活费', 'res:ic_category_living_allowance', 2, 26, NOW(), NOW()),
(27, null, '其他', 'res:ic_category_other', 2, 27, NOW(), NOW());

-- ====================================================
-- 2. 插入子分类
-- ====================================================

-- 餐饮 (1)
INSERT INTO greenfinance.sub_category (id, category_id, user_id, name, category_Icon, sort_order, create_time, update_time) VALUES
                                                                                                                                (101, 1, null, '早餐', 'res:ic_sub_category_breakfast', 1, NOW(), NOW()),
                                                                                                                                (102, 1, null, '午餐', 'res:ic_sub_category_lunch', 2, NOW(), NOW()),
                                                                                                                                (103, 1, null, '晚餐', 'res:ic_sub_category_dinner', 3, NOW(), NOW()),
                                                                                                                                (104, 1, null, '快餐', 'res:ic_sub_category_fastfood', 4, NOW(), NOW()),
                                                                                                                                (105, 1, null, '外卖', 'res:ic_sub_category_takeout', 5, NOW(), NOW()),
                                                                                                                                (106, 1, null, '零食', 'res:ic_sub_category_snack', 6, NOW(), NOW()),
                                                                                                                                (107, 1, null, '水果', 'res:ic_sub_category_fruit', 7, NOW(), NOW()),
                                                                                                                                (108, 1, null, '饮品', 'res:ic_sub_category_drink', 8, NOW(), NOW()),
                                                                                                                                (109, 1, null, '咖啡', 'res:ic_sub_category_coffee', 9, NOW(), NOW()),
                                                                                                                                (110, 1, null, '奶茶', 'res:ic_sub_category_milk_tea', 10, NOW(), NOW()),
                                                                                                                                (111, 1, null, '甜点', 'res:ic_sub_category_dessert', 11, NOW(), NOW());

-- 交通 (2)
INSERT INTO greenfinance.sub_category (id, category_id, user_id, name, category_Icon, sort_order, create_time, update_time) VALUES
                                                                                                                                (201, 2, null, '公共交通', 'res:ic_sub_category_public_transport', 1, NOW(), NOW()),
                                                                                                                                (202, 2, null, '打车', 'res:ic_sub_category_taxi', 2, NOW(), NOW()),
                                                                                                                                (203, 2, null, '停车', 'res:ic_sub_category_parking', 3, NOW(), NOW()),
                                                                                                                                (204, 2, null, '加油充电', 'res:ic_sub_category_maintenance', 4, NOW(), NOW()),
                                                                                                                                (205, 2, null, '保养维修', 'res:ic_sub_category_repair', 5, NOW(), NOW());

-- 购物 (3)
INSERT INTO greenfinance.sub_category (id, category_id, user_id, name, category_Icon, sort_order, create_time, update_time) VALUES
                                                                                                                                (301, 3, null, '服装鞋包', 'res:ic_sub_category_clothing', 1, NOW(), NOW()),
                                                                                                                                (302, 3, null, '日用品', 'res:ic_sub_category_daily_necessities', 2, NOW(), NOW()),
                                                                                                                                (303, 3, null, '电子产品', 'res:ic_sub_category_electronics', 3, NOW(), NOW()),
                                                                                                                                (304, 3, null, '化妆品', 'res:ic_sub_category_cosmetics', 4, NOW(), NOW()),
                                                                                                                                (305, 3, null, '家居装饰', 'res:ic_sub_category_home_decor', 5, NOW(), NOW());

-- 娱乐 (4)
INSERT INTO greenfinance.sub_category (id, category_id, user_id, name, category_Icon, sort_order, create_time, update_time) VALUES
                                                                                                                                (401, 4, null, '电影', 'res:ic_sub_category_movie', 1, NOW(), NOW()),
                                                                                                                                (402, 4, null, '游戏', 'res:ic_sub_category_game', 2, NOW(), NOW()),
                                                                                                                                (403, 4, null, '运动健身', 'res:ic_sub_category_sports', 3, NOW(), NOW()),
                                                                                                                                (404, 4, null, '旅行', 'res:ic_sub_category_travel', 4, NOW(), NOW()),
                                                                                                                                (405, 4, null, '聚会', 'res:ic_sub_category_party', 5, NOW(), NOW()),
                                                                                                                                (406, 4, null, '酒吧', 'res:ic_sub_category_bar', 6, NOW(), NOW());

-- 医疗 (5)
INSERT INTO greenfinance.sub_category (id, category_id, user_id, name, category_Icon, sort_order, create_time, update_time) VALUES
                                                                                                                                (501, 5, null, '药品', 'res:ic_sub_category_medicine', 1, NOW(), NOW()),
                                                                                                                                (502, 5, null, '诊疗', 'res:ic_sub_category_treatment', 2, NOW(), NOW()),
                                                                                                                                (503, 5, null, '体检', 'res:ic_sub_category_checkup', 3, NOW(), NOW());

-- 教育 (6)
INSERT INTO greenfinance.sub_category (id, category_id, user_id, name, category_Icon, sort_order, create_time, update_time) VALUES
                                                                                                                                (601, 6, null, '学费', 'res:ic_sub_category_tuition', 1, NOW(), NOW()),
                                                                                                                                (602, 6, null, '培训', 'res:ic_sub_category_training', 2, NOW(), NOW()),
                                                                                                                                (603, 6, null, '书籍', 'res:ic_sub_category_books', 3, NOW(), NOW());

-- 居家 (7)
INSERT INTO greenfinance.sub_category (id, category_id, user_id, name, category_Icon, sort_order, create_time, update_time) VALUES
                                                                                                                                (701, 7, null, '房租', 'res:ic_sub_category_rent', 1, NOW(), NOW()),
                                                                                                                                (702, 7, null, '房贷', 'res:ic_sub_category_mortgage', 2, NOW(), NOW()),
                                                                                                                                (703, 7, null, '水电燃气', 'res:ic_sub_category_utilities', 3, NOW(), NOW()),
                                                                                                                                (704, 7, null, '物业', 'res:ic_sub_category_property', 4, NOW(), NOW()),
                                                                                                                                (705, 7, null, '装修', 'res:ic_sub_category_renovation', 5, NOW(), NOW()),
                                                                                                                                (706, 7, null, '维修', 'res:ic_sub_category_appliance', 6, NOW(), NOW()),
                                                                                                                                (707, 7, null, '家政保洁', 'res:ic_sub_category_laundry', 7, NOW(), NOW());

-- 通讯 (8)
INSERT INTO greenfinance.sub_category (id, category_id, user_id, name, category_Icon, sort_order, create_time, update_time) VALUES
                                                                                                                                (801, 8, null, '话费', 'res:ic_sub_category_telecom', 1, NOW(), NOW()),
                                                                                                                                (802, 8, null, '宽带', 'res:ic_sub_category_telecom', 2, NOW(), NOW()),
                                                                                                                                (803, 8, null, '订阅服务', 'res:ic_sub_category_subscription', 3, NOW(), NOW());

-- 宠物 (9)
INSERT INTO greenfinance.sub_category (id, category_id, user_id, name, category_Icon, sort_order, create_time, update_time) VALUES
                                                                                                                                (901, 9, null, '宠物食品', 'res:ic_category_pet', 1, NOW(), NOW()),
                                                                                                                                (902, 9, null, '宠物医疗', 'res:ic_category_medical', 2, NOW(), NOW()),
                                                                                                                                (903, 9, null, '宠物用品', 'res:ic_category_pet', 3, NOW(), NOW());

-- 人情 (10)
INSERT INTO greenfinance.sub_category (id, category_id, user_id, name, category_Icon, sort_order, create_time, update_time) VALUES
                                                                                                                                (1001, 10, null, '礼金', 'res:ic_sub_category_gift_money', 1, NOW(), NOW()),
                                                                                                                                (1002, 10, null, '请客', 'res:ic_sub_category_treat', 2, NOW(), NOW());

-- 其他支出 (11)
INSERT INTO greenfinance.sub_category (id, category_id, user_id, name, category_Icon, sort_order, create_time, update_time) VALUES
                                                                                                                                (1101, 11, null, '快递', 'res:ic_sub_category_express', 1, NOW(), NOW()),
                                                                                                                                (1102, 11, null, '理发', 'res:ic_sub_category_haircut', 2, NOW(), NOW()),
                                                                                                                                (1103, 11, null, '母婴', 'res:ic_sub_category_baby', 3, NOW(), NOW()),
                                                                                                                                (1104, 11, null, '其他', 'res:ic_category_other', 99, NOW(), NOW());

-- 工资 (20)
INSERT INTO greenfinance.sub_category (id, category_id, user_id, name, category_Icon, sort_order, create_time, update_time) VALUES
                                                                                                                                (2001, 20, null, '月薪', 'res:ic_sub_category_monthly_salary', 1, NOW(), NOW()),
                                                                                                                                (2002, 20, null, '加班费', 'res:ic_sub_category_overtime_pay', 2, NOW(), NOW());

-- 奖金 (21)
INSERT INTO greenfinance.sub_category (id, category_id, user_id, name, category_Icon, sort_order, create_time, update_time) VALUES
                                                                                                                                (2101, 21, null, '年终奖', 'res:ic_sub_category_year_end_bonus', 1, NOW(), NOW()),
                                                                                                                                (2102, 21, null, '绩效奖', 'res:ic_sub_category_performance_bonus', 2, NOW(), NOW());

-- 投资 (22)
INSERT INTO greenfinance.sub_category (id, category_id, user_id, name, category_Icon, sort_order, create_time, update_time) VALUES
                                                                                                                                (2201, 22, null, '理财收益', 'res:ic_sub_category_financial_income', 1, NOW(), NOW()),
                                                                                                                                (2202, 22, null, '股票收益', 'res:ic_sub_category_stock_income', 2, NOW(), NOW());

-- 兼职 (23)
INSERT INTO greenfinance.sub_category (id, category_id, user_id, name, category_Icon, sort_order, create_time, update_time) VALUES
                                                                                                                                (2301, 23, null, '设计兼职', 'res:ic_sub_category_design_freelance', 1, NOW(), NOW()),
                                                                                                                                (2302, 23, null, '咨询兼职', 'res:ic_sub_category_consulting_freelance', 2, NOW(), NOW()),
                                                                                                                                (2303, 23, null, '翻译兼职', 'res:ic_sub_category_translation_freelance', 3, NOW(), NOW());

-- 退款 (24)
INSERT INTO greenfinance.sub_category (id, category_id, user_id, name, category_Icon, sort_order, create_time, update_time) VALUES
    (2401, 24, null, '购物退款', 'res:ic_category_refund', 1, NOW(), NOW());

-- 人情收入 (25)
INSERT INTO greenfinance.sub_category (id, category_id, user_id, name, category_Icon, sort_order, create_time, update_time) VALUES
    (2501, 25, null, '礼金收入', 'res:ic_sub_category_gift_money', 1, NOW(), NOW());

-- 生活费 (26)
INSERT INTO greenfinance.sub_category (id, category_id, user_id, name, category_Icon, sort_order, create_time, update_time) VALUES
    (2601, 26, null, '生活费', 'res:ic_category_living_allowance', 1, NOW(), NOW());

-- 其他收入 (27)
INSERT INTO greenfinance.sub_category (id, category_id, user_id, name, category_Icon, sort_order, create_time, update_time) VALUES
                                                                                                                                (2701, 27, null, '中奖', 'res:ic_sub_category_lottery_winnings', 1, NOW(), NOW()),
                                                                                                                                (2702, 27, null, '意外收入', 'res:ic_sub_category_other_income', 2, NOW(), NOW());