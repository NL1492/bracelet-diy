package com.bracelet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bracelet.entity.BeadInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 珠子信息 Mapper —— 继承 MyBatis-Plus BaseMapper，自带 CRUD
 */
@Mapper
public interface BeadInfoMapper extends BaseMapper<BeadInfo> {
}
