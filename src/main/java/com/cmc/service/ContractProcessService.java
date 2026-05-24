package com.cmc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cmc.dto.AssignDTO;
import com.cmc.dto.ProcessDTO;
import com.cmc.entity.ContractProcess;

import java.util.List;

public interface ContractProcessService extends IService<ContractProcess> {
    void assignContract(AssignDTO dto);
    List<ContractProcess> getPendingTasks(Long userId, Integer type);
    void countersign(Long userId, ProcessDTO dto);
    void approve(Long userId, ProcessDTO dto);
    void sign(Long userId, ProcessDTO dto);
}
