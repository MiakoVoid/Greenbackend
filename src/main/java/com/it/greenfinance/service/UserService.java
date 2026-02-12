package com.it.greenfinance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.it.greenfinance.pojo.User;
import com.it.greenfinance.pojo.bo.UserBo;
import com.it.greenfinance.pojo.vo.UserVo;
import com.it.utils.Result;

/**
 * <p>
 * 用户信息表 服务类
 * </p>
 *
 * @author system
 * @since 2025-10-25
 */
public interface UserService extends IService<User> {
    
    /**
     * 用户登录
     *
     * @param userBo 用户登录信息
     * @return 登录结果
     */
    Result login(UserBo userBo);
    
    /**
     * 用户注册
     *
     * @param userBo 用户注册信息
     * @return 注册结果
     */
    Result register(UserBo userBo);
    
    /**
     * 用户登出
     *
     * @return 登出结果
     */
    Result logout();
    
    /**
     * 获取用户详情
     *
     * @param userBo 用户参数
     * @return 用户详情
     */
    UserVo getDetail(UserBo userBo);
    
    /**
     * 更新用户
     *
     * @param userBo 用户信息
     * @return 更新后的用户
     */
    UserVo update(UserBo userBo);
    
    /**
     * 更新用户头像
     *
     * @param avatarPath 头像路径
     * @return 更新结果
     */
    Result updateAvatar(String avatarPath);
    
    /**
     * 用户申请注销账户
     *
     * @param userId 用户ID
     * @return 注销申请结果
     */
    Result requestDeactivation(Long userId);
    
    /**
     * 取消用户注销申请
     *
     * @param userId 用户ID
     * @return 取消结果
     */
    Result cancelDeactivation(Long userId);
}