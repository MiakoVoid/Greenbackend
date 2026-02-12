package com.it.greenfinance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.greenfinance.mapper.UserMapper;
import com.it.greenfinance.pojo.User;
import com.it.greenfinance.pojo.bo.UserBo;
import com.it.greenfinance.pojo.vo.UserVo;
import com.it.greenfinance.service.UserService;
import com.it.utils.JwtUtil;
import com.it.utils.Result;
import com.it.utils.UserContextUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p>
 * 用户信息表 服务实现类
 * </p>
 *
 * @author system
 * @since 2025-10-25
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private UserContextUtil userContextUtil;
    
    private static final String ERROR_CREDENTIALS_REQUIRED = "账号密码不能为空";
    private static final String ERROR_USER_NOT_EXISTS = "用户不存在";
    private static final String ERROR_PASSWORD_WRONG = "密码错误";
    private static final String ERROR_PASSWORD_TOO_SHORT = "密码长度不能小于6";
    private static final String ERROR_PASSWORD_COMPLEXITY = "密码必须包含字母和数字";
    private static final String ERROR_USER_EXISTS = "用户已存在";
    private static final String ERROR_USERNAME_FORMAT = "用户名格式不正确，应为3-20位字母、数字或下划线";
    private static final String SUCCESS_LOGIN = "登录成功";
    private static final String SUCCESS_LOGIN_CANCEL_DEACTIVATION = "登录成功, 已取消注销";
    private static final String SUCCESS_REGISTER = "注册成功";
    private static final String SUCCESS_LOGOUT = "登出成功";
    private static final String SUCCESS_AVATAR_UPDATE = "头像更新成功";
    private static final String SUCCESS_DEACTIVATION_REQUEST = "账户注销申请已提交，7天内可重新登录取消注销";
    private static final String ERROR_DEACTIVATION_FAILED = "注销申请提交失败";
    private static final String ERROR_USER_NOT_FOUND = "用户不存在";
    private static final String SUCCESS_DEACTIVATION_CANCEL = "注销申请已取消";
    
    
    @Override
    public Result login(UserBo userBo) {
        //1.判断账号密码不能为空
        if (userBo.getUsername() == null || userBo.getPassword() == null) {
            return Result.error(400, ERROR_CREDENTIALS_REQUIRED);
        }
        
        //2.判断用户名是否存在
        User dbUser = getOne(new QueryWrapper<User>().eq("username", userBo.getUsername()));
        if (dbUser == null) {
            return Result.error(400, ERROR_USER_NOT_EXISTS);
        }
        
        //3.判断密码是否正确
        if (!passwordEncoder.matches(userBo.getPassword(), dbUser.getPassword())) {
            return Result.error(400, ERROR_PASSWORD_WRONG);
        }
        
        String message = SUCCESS_LOGIN;
        // 检查用户是否处于注销冷静期，如果是则取消注销
        if (isUserInDeletionPeriod(dbUser)) {
            // 取消注销
            cancelDeactivation(dbUser);
            message = SUCCESS_LOGIN_CANCEL_DEACTIVATION;
        }
        
        //4.生成token
        String token = jwtUtil.generateToken(dbUser.getUsername(), dbUser.getId());
        
        //5.构建返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("userId", dbUser.getId());
        data.put("username", dbUser.getUsername());
        
        // 设置默认头像路径（如果用户没有设置头像）
        String avatarPath = dbUser.getAvatarPath();
        if (avatarPath == null || avatarPath.isEmpty()) {
            avatarPath = "res/drawable/default_avatar.jpg";
        }
        data.put("avatarPath", avatarPath);
        
        data.put("token", token);
        data.put("expiresIn", jwtUtil.getRemainingTime(token)); // 添加令牌过期时间
        
        return Result.ok(message, data);
    }
    
    @Override
    public Result register(UserBo userBo) {
        //账号密码合法性判断
        if (userBo.getUsername() == null || userBo.getPassword() == null) {
            return Result.error(400, ERROR_CREDENTIALS_REQUIRED);
        }
        
        // 验证用户名格式
        if (!isValidUsername(userBo.getUsername())) {
            return Result.error(400, ERROR_USERNAME_FORMAT);
        }
        
        // 验证密码格式
        if (!isValidPassword(userBo.getPassword())) {
            if (userBo.getPassword().length() < 6) {
                return Result.error(400, ERROR_PASSWORD_TOO_SHORT);
            }
            return Result.error(400, ERROR_PASSWORD_COMPLEXITY);
        }
        
        // 检查用户是否已存在
        if (isUserExists(userBo.getUsername())) {
            return Result.error(400, ERROR_USER_EXISTS);
        }
        
        // 转换Bo到实体并加密密码并保存用户
        User user = new User();
        BeanUtils.copyProperties(userBo, user);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // 设置默认头像路径
        if (user.getAvatarPath() == null || user.getAvatarPath().isEmpty()) {
            user.setAvatarPath("res/drawable/default_avatar.jpg");
        }
        
        save(user);
        
        // 生成token
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        
        // 构建返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("avatarPath", user.getAvatarPath());
        data.put("token", token);
        data.put("expiresIn", jwtUtil.getRemainingTime(token)); // 添加令牌过期时间
        
        return Result.ok(SUCCESS_REGISTER, data);
    }
    
    @Override
    public Result logout() {
        // 在实际应用中，可能需要将token加入黑名单
        // 获取当前用户的token并将其加入黑名单
        String token = userContextUtil.getCurrentToken();
        if (token != null && !token.isEmpty()) {
            jwtUtil.invalidateToken(token);
        }
        return Result.ok(SUCCESS_LOGOUT, null);
    }
    
    @Override
    @Cacheable(value = "user", key = "#userBo.id", condition = "#userBo.id != null")
    public UserVo getDetail(UserBo userBo) {
        User user = getOne(buildQueryWrapper(userBo));
        
        if (user != null) {
            return convertToVo(user);
        }
        return null;
    }
    
    @Override
    @CacheEvict(value = "user", key = "@userContextUtil.getCurrentUserId()")
    public UserVo update(UserBo userBo) {
        Long userId = userContextUtil.getCurrentUserId();
        User user = getById(userId);
        
        if (user == null) {
            return null;
        }
        
        // 更新允许修改的字段
        if (userBo.getEmail() != null) {
            user.setEmail(userBo.getEmail());
        }
        
        if (userBo.getPhone() != null) {
            user.setPhone(userBo.getPhone());
        }
        
        if (userBo.getAvatarPath() != null) {
            user.setAvatarPath(userBo.getAvatarPath());
        }
        
        user.setUpdateTime(new Date());
        updateById(user);
        return convertToVo(user);
    }
    
    @Override
    @CacheEvict(value = "user", key = "@userContextUtil.getCurrentUserId()")
    public Result updateAvatar(String avatarPath) {
        Long userId = userContextUtil.getCurrentUserId();
        User user = getById(userId);
        
        if (user == null) {
            return Result.error(404, ERROR_USER_NOT_FOUND);
        }
        
        user.setAvatarPath(avatarPath);
        user.setUpdateTime(new Date());
        updateById(user);
        
        Map<String, String> data = new HashMap<>();
        data.put("avatarPath", avatarPath);
        
        return Result.ok(SUCCESS_AVATAR_UPDATE, data);
    }
    
    @Override
    public Result requestDeactivation(Long userId) {
        User user = getById(userId);
        if (user == null) {
            return Result.error(404, ERROR_USER_NOT_FOUND);
        }
        
        // 设置注销状态和时间
        boolean success = requestDeactivationInternal(user);
        if (success) {
            return Result.ok(SUCCESS_DEACTIVATION_REQUEST, null);
        } else {
            return Result.error(500, ERROR_DEACTIVATION_FAILED);
        }
    }
    
    @Override
    @CacheEvict(value = "user", key = "#userId")
    public Result cancelDeactivation(Long userId) {
        User user = getById(userId);
        if (user == null) {
            return Result.error(404, ERROR_USER_NOT_FOUND);
        }
        
        // 检查用户是否处于注销状态
        if (user.getDeletionStatus() == null || user.getDeletionStatus() != 1) {
            return Result.error(400, "用户当前没有注销申请");
        }
        
        // 取消注销
        user.setDeletionStatus(0);
        user.setDeletionTime(null);
        boolean success = updateById(user);
        
        if (success) {
            return Result.ok(SUCCESS_DEACTIVATION_CANCEL, null);
        } else {
            return Result.error(500, "取消注销申请失败");
        }
    }
    
    /**
     * 内部方法：处理用户注销申请
     * @param user 用户对象
     * @return 是否成功
     */
    private boolean requestDeactivationInternal(User user) {
        user.setDeletionStatus(1); // 已申请注销（冷静期内）
        user.setDeletionTime(new Date()); // 设置注销申请时间
        return updateById(user);
    }
    
    /**
     * 取消用户注销状态
     * @param user 用户对象
     */
    private void cancelDeactivation(User user) {
        user.setDeletionStatus(0);
        user.setDeletionTime(null);
        updateById(user);
    }
    
    /**
     * 检查用户是否在注销冷静期内
     * @param user 用户对象
     * @return 是否在注销冷静期内
     */
    private boolean isUserInDeletionPeriod(User user) {
        if (user.getDeletionStatus() == null || user.getDeletionStatus() != 1) {
            return false;
        }
        
        if (user.getDeletionTime() == null) {
            return false;
        }
        
        // 检查是否还在7天内
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7);
        return user.getDeletionTime().after(cal.getTime());
    }
    
    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 是否存在
     */
    private boolean isUserExists(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return count(queryWrapper) > 0;
    }
    
    /**
     * 验证用户名格式
     * @param username 用户名
     * @return 是否有效
     */
    private boolean isValidUsername(String username) {
        return Pattern.matches("^[a-zA-Z0-9_]{3,20}$", username);
    }
    
    /**
     * 验证密码格式
     * @param password 密码
     * @return 是否有效
     */
    private boolean isValidPassword(String password) {
        return password.length() >= 6 &&
                Pattern.matches("^(?=.*[A-Za-z])(?=.*\\d).{6,20}$", password);
    }
    
    /**
     * 将User实体转换为UserVo
     * @param user User实体
     * @return UserVo对象
     */
    private UserVo convertToVo(User user) {
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user, userVo);
        // 不返回密码
        userVo.setPassword(null);
        
        // 设置默认头像路径（如果用户没有设置头像）
        if (userVo.getAvatarPath() == null || userVo.getAvatarPath().isEmpty()) {
            userVo.setAvatarPath("res/drawable/default_avatar.jpg");
        }
        
        return userVo;
    }
    
    private QueryWrapper<User> buildQueryWrapper(UserBo bo) {
        // 创建查询条件构造器实例
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        
        // 如果ID不为空，则添加ID相等条件
        if (bo.getId() != null) {
            queryWrapper.eq("id", bo.getId());
        }
        
        // 如果用户名不为空，则添加用户名相等条件
        if (bo.getUsername() != null && !bo.getUsername().isEmpty()) {
            queryWrapper.eq("username", bo.getUsername());
        }
        
        // 如果邮箱不为空，则添加邮箱相等条件
        if (bo.getEmail() != null && !bo.getEmail().isEmpty()) {
            queryWrapper.eq("email", bo.getEmail());
        }
        
        // 如果手机号不为空，则添加手机号相等条件
        if (bo.getPhone() != null && !bo.getPhone().isEmpty()) {
            queryWrapper.eq("phone", bo.getPhone());
        }
        
        // 按创建时间倒序排列结果
        queryWrapper.orderByDesc("create_time");
        
        // 返回构建好的查询条件构造器
        return queryWrapper;
    }
}