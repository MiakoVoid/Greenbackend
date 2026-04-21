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
        return categoryService.buildIconLibrary(type);
    }
    
}
