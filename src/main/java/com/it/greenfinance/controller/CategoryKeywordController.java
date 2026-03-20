package com.it.greenfinance.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.it.greenfinance.pojo.bo.CategoryKeywordBo;
import com.it.greenfinance.pojo.vo.CategoryKeywordVo;
import com.it.greenfinance.service.CategoryKeywordService;
import com.it.utils.Result;
import com.it.utils.UserContextUtil;
import io.swagger.annotations.Api;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

/**
 * <p>
 * 分类关键词表 前端控制器
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Api("分类关键词")
@RestController
@Validated
@RequestMapping("/category-keywords")
public class CategoryKeywordController {

    private final CategoryKeywordService categoryKeywordService;
    private final UserContextUtil userContextUtil;

    public CategoryKeywordController(CategoryKeywordService categoryKeywordService, UserContextUtil userContextUtil) {
        this.categoryKeywordService = categoryKeywordService;
        this.userContextUtil = userContextUtil;
    }

    /**
     * 创建分类关键词
     *
     * @param categoryKeywordBo 分类关键词信息
     * @return 创建结果
     */
    @PostMapping
    public Result create(@Valid @RequestBody CategoryKeywordBo categoryKeywordBo) {
        CategoryKeywordVo vo = categoryKeywordService.create(categoryKeywordBo);
        return Result.ok("分类关键词创建成功", vo);
    }

    /**
     * 获取分类关键词列表
     *
     * @param page 页码
     * @param size 每页数量
     * @return 分类关键词列表
     */
    @GetMapping
    public Result list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            CategoryKeywordBo bo) {
        bo.setUserId(userContextUtil.getCurrentUserId());
        Page<CategoryKeywordVo> voPage = categoryKeywordService.list(page, size, bo);
        return Result.ok(voPage);
    }

    /**
     * 获取分类关键词详情
     *
     * @param id 分类关键词ID
     * @return 分类关键词详情
     */
    @GetMapping("/{id}")
    public Result getDetail(@PathVariable Long id) {
        CategoryKeywordBo bo = new CategoryKeywordBo();
        bo.setId(id);
        bo.setUserId(userContextUtil.getCurrentUserId());
        CategoryKeywordVo vo = categoryKeywordService.getDetail(bo);
        if (vo != null)
            return Result.ok(vo);
        return Result.error(404, "分类关键词不存在");
    }

    /**
     * 更新分类关键词
     *
     * @param categoryKeywordBo 更新的分类关键词信息
     * @return 更新结果
     */
    @PutMapping
    public Result update(@Valid @RequestBody CategoryKeywordBo categoryKeywordBo) {
        categoryKeywordBo.setUserId(userContextUtil.getCurrentUserId());
        CategoryKeywordVo categoryKeywordVo = categoryKeywordService.update(categoryKeywordBo);
        return Result.ok("分类关键词更新成功", categoryKeywordVo);
    }

    /**
     * 删除分类关键词
     *
     * @param id 分类关键词ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        boolean deleted = categoryKeywordService.delete(id);
        if (deleted) {
            return Result.ok("分类关键词删除成功");
        } else {
            return Result.error(403, "无权限删除该分类关键词");
        }
    }
}
