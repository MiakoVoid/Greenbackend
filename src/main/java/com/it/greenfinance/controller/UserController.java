package com.it.greenfinance.controller;

import com.it.greenfinance.pojo.bo.UserBo;
import com.it.greenfinance.pojo.vo.UserVo;
import com.it.greenfinance.service.UserService;
import com.it.utils.Result;
import com.it.utils.UserContextUtil;
import io.swagger.annotations.Api;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 用户信息表 前端控制器
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@RestController
@RequestMapping("/user")
@Api(tags = "用户管理")
@Validated
public class UserController {
    
    private final UserService userService;
    private final UserContextUtil userContextUtil;
    
    public UserController(UserService userService, UserContextUtil userContextUtil) {
        this.userService = userService;
        this.userContextUtil = userContextUtil;
    }
    
    /**
     * 获取用户详情
     *
     * @return 用户详情
     */
    @GetMapping("/profile")
    public Result getDetail() {
        Long userId = userContextUtil.getCurrentUserId();
        UserBo bo = new UserBo();
        bo.setId(userId);
        UserVo vo = userService.getDetail(bo);
        if (vo != null)
            return Result.ok(vo);
        return Result.error(404, "用户不存在");
    }
    
    /**
     * 更新用户
     *
     * @param userBo 更新的用户信息
     * @return 更新结果
     */
    @PutMapping("/profile")
    public Result update(@Valid @RequestBody UserBo userBo) {
        UserVo userVo = userService.update(userBo);
        return Result.ok("用户更新成功", userVo);
    }
    /**
     * 取消用户注销申请
     *
     * @return 取消结果
     */
    @PostMapping("/cancel-deactivation")
    public Result cancelDeactivation() {
        Long userId = userContextUtil.getCurrentUserId();
        return userService.cancelDeactivation(userId);
    }
    
}
