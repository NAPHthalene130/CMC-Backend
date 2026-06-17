package com.cmc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.entity.ContractTemplate;
import com.cmc.mapper.ContractTemplateMapper;
import com.cmc.service.ContractTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author NAPH130
 */
@Service
public class ContractTemplateServiceImpl extends ServiceImpl<ContractTemplateMapper, ContractTemplate>
        implements ContractTemplateService {

    @Override
    public Page<ContractTemplate> pageTemplates(long page, long pageSize, String keyword, String category) {
        LambdaQueryWrapper<ContractTemplate> wrapper = new LambdaQueryWrapper<ContractTemplate>()
                .like(StringUtils.hasText(keyword), ContractTemplate::getName, keyword)
                .eq(StringUtils.hasText(category), ContractTemplate::getCategory, category)
                .orderByDesc(ContractTemplate::getCreateTime);
        return page(new Page<>(page, pageSize), wrapper);
    }
}
