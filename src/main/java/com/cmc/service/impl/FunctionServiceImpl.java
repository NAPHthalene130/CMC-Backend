package com.cmc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.entity.Function;
import com.cmc.mapper.FunctionMapper;
import com.cmc.service.FunctionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FunctionServiceImpl extends ServiceImpl<FunctionMapper, Function> implements FunctionService {

    @Override
    public Page<Function> pageFunctions(long page, long pageSize, String keyword) {
        LambdaQueryWrapper<Function> wrapper = new LambdaQueryWrapper<Function>()
                .like(StringUtils.hasText(keyword), Function::getName, keyword)
                .orderByAsc(Function::getNum);
        return page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public List<Function> listTree() {
        List<Function> all = list(new LambdaQueryWrapper<Function>()
                .orderByAsc(Function::getSortOrder)
                .orderByAsc(Function::getNum));

        Map<Long, List<Function>> parentIdMap = all.stream()
                .collect(Collectors.groupingBy(f -> f.getParentId() != null ? f.getParentId() : 0L));

        List<Function> roots = parentIdMap.getOrDefault(0L, new ArrayList<>());
        roots.sort(Comparator.comparing(Function::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Function::getNum, Comparator.nullsLast(Comparator.naturalOrder())));

        for (Function fn : all) {
            List<Function> children = parentIdMap.get(fn.getId());
            if (children != null && !children.isEmpty()) {
                children.sort(Comparator.comparing(Function::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Function::getNum, Comparator.nullsLast(Comparator.naturalOrder())));
                fn.setChildren(children);
            }
        }

        return roots;
    }
}
