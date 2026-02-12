package com.it.greenfinance.controller;

import com.it.greenfinance.pojo.bo.UserBo;
import com.it.greenfinance.pojo.vo.UserVo;
import com.it.greenfinance.service.UserService;
import com.it.utils.Result;
import com.it.utils.UserContextUtil;
import io.swagger.annotations.Api;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.validation.Valid;

import java.util.UUID;

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
     * 上传用户头像
     *
     * @param file 头像文件
     * @return 上传结果
     */
    @PostMapping("/avatar")
    public Result uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "请选择要上传的文件");
        }
        
        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            return Result.error(400, "只支持JPEG和PNG格式的图片");
        }
        
        // 检查文件大小（限制为2MB）
        if (file.getSize() > 2 * 1024 * 1024) {
            return Result.error(400, "文件大小不能超过2MB");
        }
        
        try {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString().replace("-", "") + fileExtension;
            
            // 构建文件存储路径（相对于安卓应用的文件目录）
            String avatarPath = "files/greenfinance/avatars/" + newFilename;
            
            // 调用服务层更新用户头像路径
            return userService.updateAvatar(avatarPath);
        } catch (Exception e) {
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
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
