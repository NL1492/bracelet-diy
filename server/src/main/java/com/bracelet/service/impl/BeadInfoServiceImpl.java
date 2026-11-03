package com.bracelet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bracelet.entity.BeadInfo;
import com.bracelet.mapper.BeadInfoMapper;
import com.bracelet.service.BeadInfoService;
import org.springframework.stereotype.Service;

/**
 * 珠子信息 Service 实现
 */
@Service
public class BeadInfoServiceImpl extends ServiceImpl<BeadInfoMapper, BeadInfo>
        implements BeadInfoService {
}
