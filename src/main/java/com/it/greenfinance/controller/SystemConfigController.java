package com.it.greenfinance.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.it.greenfinance.pojo.bo.SystemConfigBo;
import com.it.greenfinance.pojo.vo.SystemConfigVo;
import com.it.greenfinance.service.SystemConfigService;
import com.it.utils.Result;
import com.it.utils.UserContextUtil;
import io.swagger.annotations.Api;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

/**
 * <p>
 * 系统配置表 前端控制器
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Api(tags = "系统配置")
@RestController
@Validated
@RequestMapping("/system-configs")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;
    private final UserContextUtil userContextUtil;

    public SystemConfigController(SystemConfigService systemConfigService, UserContextUtil userContextUtil) {
        this.systemConfigService = systemConfigService;
        this.userContextUtil = userContextUtil;
    }

    /**
     * 创建系统配置
     *
     * @param systemConfigBo 系统配置信息
     * @return 创建结果
     */
    @PostMapping
    public Result create(@Valid @RequestBody SystemConfigBo systemConfigBo) {
        SystemConfigVo vo = systemConfigService.create(systemConfigBo);
        return Result.ok("系统配置创建成功", vo);
    }

    /**
     * 获取系统配置列表
     *
     * @param page 页码
     * @param size 每页数量
     * @return 系统配置列表
     */
    @GetMapping
    public Result list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            SystemConfigBo bo) {
        bo.setUserId(userContextUtil.getCurrentUserId());
        Page<SystemConfigVo> voPage = systemConfigService.list(page, size, bo);
        return Result.ok(voPage);
    }

    /**
     * 获取系统配置详情
     *
     * @param id 系统配置ID
     * @return 系统配置详情
     */
    @GetMapping("/{id}")
    public Result getDetail(@PathVariable Long id) {
        SystemConfigBo bo = new SystemConfigBo();
        bo.setId(id);
        bo.setUserId(userContextUtil.getCurrentUserId());
        SystemConfigVo vo = systemConfigService.getDetail(bo);
        if (vo != null)
            return Result.ok(vo);
        return Result.error(404, "系统配置不存在");
    }

    /**
     * 更新系统配置
     *
     * @param systemConfigBo 更新的系统配置信息
     * @return 更新结果
     */
    @PutMapping
    public Result update(@Valid @RequestBody SystemConfigBo systemConfigBo) {
        systemConfigBo.setUserId(userContextUtil.getCurrentUserId());
        SystemConfigVo systemConfigVo = systemConfigService.update(systemConfigBo);
        return Result.ok("系统配置更新成功", systemConfigVo);
    }

    /**
     * 删除系统配置
     *
     * @param id 系统配置ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        boolean deleted = systemConfigService.removeById(id);
        if (deleted) {
            return Result.ok("系统配置删除成功");
        } else {
            return Result.error(403, "无权限删除该系统配置");
        }
    }

    /**
     * 根据配置键获取配置值
     *
     * @param configKey 配置键
     * @return 配置值
     */
    @GetMapping("/value")
    public Result getConfigValue(@RequestParam String configKey) {
        String configValue = systemConfigService.getConfigValue(configKey);
        if (configValue != null) {
            return Result.ok(configValue);
        } else {
            return Result.error(404, "配置项不存在");
        }
    }
}
