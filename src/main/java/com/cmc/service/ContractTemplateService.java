package com.cmc.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cmc.entity.ContractTemplate;

/**
 * @author NAPH130
 */
public interface ContractTemplateService extends IService<ContractTemplate> {
    Page<ContractTemplate> pageTemplates(long page, long pageSize, String keyword, String category);
}
