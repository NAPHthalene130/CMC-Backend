package com.cmc.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cmc.entity.Function;

import java.util.List;

public interface FunctionService extends IService<Function> {
    Page<Function> pageFunctions(long page, long pageSize, String keyword);

    List<Function> listTree();
}
