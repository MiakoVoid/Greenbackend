package com.it.greenfinance.controller;

import com.it.greenfinance.pojo.bo.UserBo;
import com.it.greenfinance.service.UserService;
import com.it.utils.Result;
import io.swagger.annotations.Api;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

/**
 * 用户登录控制器
 *
 * @author ${author}
 * @since 2025-10-25
 */
@RestController
@Api(tags = "用户认证")
@Validated
@RequestMapping("/auth")
public class UserLoginController {
    private final UserService userService;
    
    public UserLoginController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * 登录
     *
     * @param userBo 用户登录信息
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result login(@Valid @RequestBody UserBo userBo) {
        return userService.login(userBo);
    }
    
    /**
     * 注册
     *
     * @param userBo 用户注册信息
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result register(@Valid @RequestBody UserBo userBo) {
        return userService.register(userBo);
    }
    
    /**
     * 登出
     *
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result logout() {
        return userService.logout();
    }
}
