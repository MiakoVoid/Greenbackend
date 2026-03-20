package com.it.greenfinance.controller;

import com.it.greenfinance.pojo.bo.CategoryBo;
import com.it.greenfinance.pojo.vo.CategoryVo;
import com.it.greenfinance.service.CategoryService;
import com.it.utils.Result;
import com.it.utils.UserContextUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

import java.util.List;

/**
 * 分类控制器
 */
@Api(tags = "分类管理")
@RestController
@Validated
@RequestMapping("/categories")
public class CategoryController {
    
    private final CategoryService categoryService;
    private final UserContextUtil userContextUtil;
    
    public CategoryController(CategoryService categoryService, UserContextUtil userContextUtil) {
        this.categoryService = categoryService;
        this.userContextUtil = userContextUtil;
    }
    
    /**
     * 获取所有分类（支持按类型筛选）
     *
     * @param type 分类类型（可选，1=支出，2=收入）
     * @return 分类列表
     */
    @GetMapping
    public Result listCategories(@RequestParam(required = false) Integer type) {
        Long userId = userContextUtil.getCurrentUserId();
        List<CategoryVo> categoryVos = categoryService.getUserCategoriesWithSubCategories(userId, type);
        
        return Result.ok(categoryVos);
    }
    
    /**
     * 创建主分类
     *
     * @param categoryBo 分类信息
     * @return 创建结果
     */
    @PostMapping
    public Result createCategoryVo(@Valid @RequestBody CategoryBo categoryBo) {
        Long userId = userContextUtil.getCurrentUserId();
        CategoryVo vo = categoryService.saveCategory(categoryBo, userId);
        return Result.ok("主分类创建成功",vo);
    }
    
    /**
     * 更新主分类
     *
     * @param categoryBo 更新的分类信息
     * @return 更新结果
     */
    @PutMapping("/update")
    public Result updateCategory(@Valid @RequestBody CategoryBo categoryBo) {
        Long userId = userContextUtil.getCurrentUserId();
        CategoryVo categoryVo = categoryService.updateCategory(categoryBo, userId);
        return Result.ok("分类更新成功", categoryVo);
    }
    
    /**
     * 删除主分类（只有用户自己的分类才能删除）
     *
     * @param id 分类 ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result deleteCategory(@PathVariable Long id) {
        Long userId = userContextUtil.getCurrentUserId();
        boolean deleted = categoryService.removeCategoryById(id, userId);
        if (deleted) {
            return Result.ok("分类删除成功");
        } else {
            return Result.error(403, "无权限删除该分类");
        }
    }
    
    /**
     * 获取图标库列表
     *
     * @param type 图标类型（可选，1=支出分类图标，2=收入分类图标，不传返回全部）
     * @return 图标标识符字符串数组
     */
    @GetMapping("/icon-library")
    @ApiOperation("获取图标库列表")
    public Result getIconLibrary(
            @ApiParam(value = "图标类型（1-支出，2-收入，不传返回全部）", example = "1")
            @RequestParam(required = false) Integer type) {
        
        List<String> icons = buildIconLibrary(type);
        return Result.ok(icons);
    }
    
    /**
     * 构建图标库数据
     * @param type 图标类型（1=支出，2=收入，null=全部）
     * @return 图标标识符列表
     */
    private List<String> buildIconLibrary(Integer type) {
        List<String> icons = new java.util.ArrayList<>();
        
        // ==================== 支出分类图标 ====================
        
        // 餐饮类
        icons.add("res:ic_category_food");
        icons.add("res:ic_sub_category_drink");
        icons.add("res:ic_sub_category_fastfood");
        icons.add("res:ic_sub_category_milk_tea");
        icons.add("res:ic_sub_category_snack");
        icons.add("res:ic_sub_category_takeout");
        
        // 交通类
        icons.add("res:ic_category_transport");
        icons.add("res:ic_sub_category_public_transport");
        icons.add("res:ic_sub_category_taxi");
        icons.add("res:ic_sub_category_parking");
        
        // 购物类
        icons.add("res:ic_category_shopping");
        icons.add("res:ic_sub_category_clothing");
        icons.add("res:ic_sub_category_electronics");
        icons.add("res:ic_sub_category_daily_necessities");
        
        // 娱乐类
        icons.add("res:ic_category_entertainment");
        icons.add("res:ic_sub_category_movie");
        icons.add("res:ic_sub_category_party");
        icons.add("res:ic_sub_category_travel");
        icons.add("res:ic_sub_category_books");
        
        // 医疗类
        icons.add("res:ic_category_medical");
        icons.add("res:ic_sub_category_medicine");
        icons.add("res:ic_sub_category_treatment");
        
        // 教育类
        icons.add("res:ic_category_education");
        icons.add("res:ic_sub_category_tuition");
        icons.add("res:ic_sub_category_training");
        
        // 生活类
        icons.add("res:ic_category_life");
        icons.add("res:ic_sub_category_utilities");
        icons.add("res:ic_sub_category_rent");
        icons.add("res:ic_sub_category_mortgage");
        icons.add("res:ic_sub_category_property");
        icons.add("res:ic_sub_category_maintenance");
        icons.add("res:ic_sub_category_renovation");
        icons.add("res:ic_sub_category_telecom");
        icons.add("res:ic_sub_category_treat");
        
        // 其他支出
        icons.add("res:ic_category_other");
        
        // ==================== 收入分类图标 ====================
        
        // 工资类
        icons.add("res:ic_category_salary");
        icons.add("res:ic_sub_category_monthly_salary");
        icons.add("res:ic_sub_category_overtime_pay");
        icons.add("res:ic_sub_category_performance_bonus");
        icons.add("res:ic_sub_category_year_end_bonus");
        
        // 奖金类
        icons.add("res:ic_category_bonus");
        
        // 兼职类
        icons.add("res:ic_category_parttime");
        icons.add("res:ic_sub_category_consulting_freelance");
        icons.add("res:ic_sub_category_design_freelance");
        icons.add("res:ic_sub_category_translation_freelance");
        
        // 投资类
        icons.add("res:ic_category_investment");
        icons.add("res:ic_sub_category_financial_income");
        icons.add("res:ic_sub_category_stock_income");
        icons.add("res:ic_sub_category_lottery_winnings");
        
        // 礼金类
        icons.add("res:ic_category_gift");
        icons.add("res:ic_sub_category_gift_money");
        
        // 退款类
        icons.add("res:ic_category_refund");
        
        // 其他收入
        icons.add("res:ic_category_other_income");
        
        // 如果指定了类型，进行筛选
        if (type != null) {
            if (type == 1) {
                // 返回支出图标（前 38 个）
                return icons.subList(0, Math.min(38, icons.size()));
            } else if (type == 2) {
                // 返回收入图标（后 22 个）
                int startIndex = icons.size() - 22;
                return icons.subList(Math.max(0, startIndex), icons.size());
            }
        }
        
        return icons;
    }
    
}
