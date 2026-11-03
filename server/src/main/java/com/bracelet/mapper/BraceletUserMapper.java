package com.bracelet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bracelet.entity.BraceletUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface BraceletUserMapper extends BaseMapper<BraceletUser> {
}
