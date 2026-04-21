package com.it.greenfinance.pojo.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 用户信息表
 * </p>
 *
 * @author system
 * @since 2025-10-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class UserBo implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名（唯一）
     */
    @Size(min = 3, max = 32, message = "用户名长度为3-32")
    private String username;

    /**
     * 密码（BCrypt加密存储）
     */
    @Size(min = 6, max = 64, message = "密码长度为6-64")
    private String password;

    /**
     * 邮箱（唯一）
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 手机号
     */
    @Size(max = 20, message = "手机号长度不合法")
    private String phone;

    /**
     * 头像本地存储路径（相对路径，如"avatars/10001.png"）
     */
    @Size(max = 255, message = "头像路径长度过长")
    private String avatarPath;

    /**
     * 状态：0-禁用，1-正常
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
    
    /**
     * 注销时间（申请注销的时间）
     */
    private Date deletionTime;

    /**
     * 注销状态：0-未申请注销，1-已申请注销（冷静期内），2-已注销
     */
    private Integer deletionStatus;

}
