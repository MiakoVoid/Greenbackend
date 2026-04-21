package com.it.greenfinance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.it.greenfinance.pojo.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户信息表 Mapper 接口
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
