package com.cmc.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cmc.entity.Function;

public interface FunctionService extends IService<Function> {
    Page<Function> pageFunctions(long page, long pageSize, String keyword);
}
