package com.cmc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.entity.Function;
import com.cmc.mapper.FunctionMapper;
import com.cmc.service.FunctionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FunctionServiceImpl extends ServiceImpl<FunctionMapper, Function> implements FunctionService {

    @Override
    public Page<Function> pageFunctions(long page, long pageSize, String keyword) {
        LambdaQueryWrapper<Function> wrapper = new LambdaQueryWrapper<Function>()
                .like(StringUtils.hasText(keyword), Function::getName, keyword)
                .orderByAsc(Function::getNum);
        return page(new Page<>(page, pageSize), wrapper);
    }
}
