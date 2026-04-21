package com.it.greenfinance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.it.greenfinance.pojo.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 主分类表 Mapper 接口
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

}
