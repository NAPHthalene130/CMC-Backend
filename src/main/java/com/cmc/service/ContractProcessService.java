package com.cmc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cmc.dto.AssignDTO;
import com.cmc.dto.PendingTaskVO;
import com.cmc.dto.ProcessDTO;
import com.cmc.entity.ContractProcess;

import java.util.List;

public interface ContractProcessService extends IService<ContractProcess> {
    void assignContract(AssignDTO dto);
    List<PendingTaskVO> getPendingTasks(Long userId, Integer type);
    void countersign(Long userId, ProcessDTO dto);
    void approve(Long userId, ProcessDTO dto);
    void sign(Long userId, ProcessDTO dto);
    List<PendingTaskVO> getContractProcesses(Long contractId);
    void redraft(Long userId, Long contractId);
    Integer getCurrentState(Long contractId);
}
