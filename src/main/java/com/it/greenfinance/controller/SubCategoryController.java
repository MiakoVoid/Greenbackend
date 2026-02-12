// 文件路径: com/it/greenfinance/controller/SubCategoryController.java
package com.it.greenfinance.controller;

import com.it.greenfinance.pojo.bo.SubCategoryBo;
import com.it.greenfinance.pojo.vo.SubCategoryVo;
import com.it.greenfinance.service.SubCategoryService;
import com.it.utils.Result;
import com.it.utils.UserContextUtil;
import io.swagger.annotations.Api;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

import java.util.List;

/**
 * 子分类控制器
 */
@Api(tags = "子分类")
@RestController
@Validated
@RequestMapping("/sub-categories")
public class SubCategoryController {
    
    private final SubCategoryService subCategoryService;
    private final UserContextUtil userContextUtil;
    
    public SubCategoryController(SubCategoryService subCategoryService, UserContextUtil userContextUtil) {
        this.subCategoryService = subCategoryService;
        this.userContextUtil = userContextUtil;
    }
    
    /**
     * 获取指定分类下的所有子分类
     *
     * @param categoryId 分类ID
     * @return 子分类列表
     */
    @GetMapping
    public Result list(@RequestParam Long categoryId){
        Long userId = userContextUtil.getCurrentUserId();
        List<SubCategoryVo> subCategoryVos = subCategoryService.list(categoryId, userId);
        return Result.ok(subCategoryVos);
    }
        
    /**
     * 创建子分类
     *
     * @param subCategoryBo 子分类信息
     * @return 创建结果
     */
    @PostMapping
    public Result createSubCategory(@Valid @RequestBody SubCategoryBo subCategoryBo) {
        Long userId = userContextUtil.getCurrentUserId();
        SubCategoryVo subCategoryVo = subCategoryService.saveSubCategory(subCategoryBo, userId);
        return Result.ok("子分类创建成功", subCategoryVo);
    }
    
    /**
     * 更新子分类
     *
     * @param subCategoryBo 更新的子分类信息
     * @return 更新结果
     */
    @PutMapping("/update")
    public Result updateSubCategory(@Valid @RequestBody SubCategoryBo subCategoryBo) {
        Long userId = userContextUtil.getCurrentUserId();
        SubCategoryVo subCategoryVo = subCategoryService.updateSubCategory(subCategoryBo, userId);
        return Result.ok("子分类更新成功", subCategoryVo);
    }
    
    /**
     * 删除子分类（只有用户自己的子分类才能删除）
     *
     * @param id 子分类ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result deleteSubCategory(@PathVariable Long id) {
        Long userId = userContextUtil.getCurrentUserId();
        boolean deleted = subCategoryService.removeSubCategoryById(id, userId);
        if (deleted) {
            return Result.ok("子分类删除成功");
        } else {
            return Result.error(403, "无权限删除该子分类");
        }
    }
    
}
