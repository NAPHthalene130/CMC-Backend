package com.cmc.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cmc.dto.ContractDTO;
import com.cmc.entity.Contract;

public interface ContractService extends IService<Contract> {
    Contract draft(Long userId, ContractDTO dto);
    Contract finalize(Long id, ContractDTO dto);
    Page<Contract> pageContracts(long page, long pageSize, String keyword);
    Page<Contract> pageByState(long page, long pageSize, Integer stateType);
}
