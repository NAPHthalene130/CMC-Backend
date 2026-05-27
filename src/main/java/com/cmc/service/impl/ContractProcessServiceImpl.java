package com.cmc.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.common.exception.BusinessException;
import com.cmc.dto.AssignDTO;
import com.cmc.dto.ProcessDTO;
import com.cmc.entity.ContractProcess;
import com.cmc.entity.ContractState;
import com.cmc.entity.User;
import com.cmc.entity.Contract;
import com.cmc.mapper.ContractMapper;
import com.cmc.mapper.ContractProcessMapper;
import com.cmc.mapper.ContractStateMapper;
import com.cmc.service.ContractProcessService;
import com.cmc.service.LogService;
import com.cmc.vo.ProcessTaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractProcessServiceImpl extends ServiceImpl<ContractProcessMapper, ContractProcess>
        implements ContractProcessService {

    private final ContractStateMapper contractStateMapper;
    private final ContractMapper contractMapper;
    private final LogService logService;

    @Override
    @Transactional
    public void assignContract(AssignDTO dto) {
        if (dto.getCountersignUserIds() == null || dto.getCountersignUserIds().isEmpty()
                || dto.getApproveUserIds() == null || dto.getApproveUserIds().isEmpty()
                || dto.getSignUserIds() == null || dto.getSignUserIds().isEmpty()) {
            throw new BusinessException("会签、审批、签订人员需全部指定");
        }
        if (lambdaQuery().eq(ContractProcess::getContractId, dto.getContractId()).count() > 0) {
            throw new BusinessException("该合同已分配流程人员");
        }
        List<ContractProcess> processes = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (dto.getCountersignUserIds() != null) {
            for (Long userId : dto.getCountersignUserIds()) {
                ContractProcess p = new ContractProcess();
                p.setContractId(dto.getContractId());
                p.setType(1);
                p.setState(0);
                p.setUserId(userId);
                p.setTime(now);
                processes.add(p);
            }
        }
        if (dto.getApproveUserIds() != null) {
            for (Long userId : dto.getApproveUserIds()) {
                ContractProcess p = new ContractProcess();
                p.setContractId(dto.getContractId());
                p.setType(2);
                p.setState(0);
                p.setUserId(userId);
                p.setTime(now);
                processes.add(p);
            }
        }
        if (dto.getSignUserIds() != null) {
            for (Long userId : dto.getSignUserIds()) {
                ContractProcess p = new ContractProcess();
                p.setContractId(dto.getContractId());
                p.setType(3);
                p.setState(0);
                p.setUserId(userId);
                p.setTime(now);
                processes.add(p);
            }
        }

        saveBatch(processes);

        User operator = (User) StpUtil.getSession().get("user");
        logService.saveLog(operator.getId(), operator.getUsername(),
                "分配合同：" + dto.getContractId());
    }

    @Override
    public List<ProcessTaskVO> getPendingTasks(Long userId, Integer type) {
        return lambdaQuery()
                .eq(ContractProcess::getUserId, userId)
                .eq(type != null, ContractProcess::getType, type)
                .eq(ContractProcess::getState, 0)
                .list()
                .stream()
                .map(this::toTaskVO)
                .toList();
    }

    @Override
    public List<ProcessTaskVO> getContractProcesses(Long contractId, Integer type) {
        return lambdaQuery()
                .eq(ContractProcess::getContractId, contractId)
                .eq(type != null, ContractProcess::getType, type)
                .orderByAsc(ContractProcess::getTime)
                .list()
                .stream()
                .map(this::toTaskVO)
                .toList();
    }

    private ProcessTaskVO toTaskVO(ContractProcess process) {
        ProcessTaskVO vo = new ProcessTaskVO();
        vo.setId(process.getId());
        vo.setContractId(process.getContractId());
        vo.setType(process.getType());
        vo.setState(process.getState());
        vo.setUserId(process.getUserId());
        vo.setContent(process.getContent());
        vo.setTime(process.getTime());
        Contract contract = contractMapper.selectById(process.getContractId());
        if (contract != null) {
            vo.setContractNum(contract.getNum());
            vo.setContractName(contract.getName());
        }
        return vo;
    }

    @Override
    @Transactional
    public void countersign(Long userId, ProcessDTO dto) {
        ContractProcess process = getTaskOrFail(userId, dto.getContractId(), 1);
        process.setState(1);
        process.setContent(dto.getContent());
        process.setTime(LocalDateTime.now());
        updateById(process);

        if (allCompleted(dto.getContractId(), 1)) {
            saveContractState(dto.getContractId(), 2);
        }

        logService.saveLog(userId, getUsername(userId), "会签合同：" + dto.getContractId());
    }

    @Override
    @Transactional
    public void approve(Long userId, ProcessDTO dto) {
        ContractProcess process = getTaskOrFail(userId, dto.getContractId(), 2);
        process.setState(dto.getApproved() != null && dto.getApproved() ? 1 : 2);
        process.setContent(dto.getContent());
        process.setTime(LocalDateTime.now());
        updateById(process);

        if (dto.getApproved() != null && dto.getApproved() && allApproved(dto.getContractId())) {
            saveContractState(dto.getContractId(), 4);
        }

        logService.saveLog(userId, getUsername(userId), "审批合同：" + dto.getContractId());
    }

    @Override
    @Transactional
    public void sign(Long userId, ProcessDTO dto) {
        ContractProcess process = getTaskOrFail(userId, dto.getContractId(), 3);
        process.setState(1);
        process.setContent(dto.getContent());
        process.setTime(LocalDateTime.now());
        updateById(process);

        if (allCompleted(dto.getContractId(), 3)) {
            saveContractState(dto.getContractId(), 5);
        }

        logService.saveLog(userId, getUsername(userId), "签订合同：" + dto.getContractId());
    }

    private ContractProcess getTaskOrFail(Long userId, Long contractId, Integer type) {
        ContractProcess process = lambdaQuery()
                .eq(ContractProcess::getUserId, userId)
                .eq(ContractProcess::getContractId, contractId)
                .eq(ContractProcess::getType, type)
                .eq(ContractProcess::getState, 0)
                .one();
        if (process == null) {
            throw new BusinessException("未找到待处理任务");
        }
        return process;
    }

    private boolean allCompleted(Long contractId, Integer type) {
        return lambdaQuery()
                .eq(ContractProcess::getContractId, contractId)
                .eq(ContractProcess::getType, type)
                .eq(ContractProcess::getState, 0)
                .count() == 0;
    }

    private boolean allApproved(Long contractId) {
        long pendingCount = lambdaQuery()
                .eq(ContractProcess::getContractId, contractId)
                .eq(ContractProcess::getType, 2)
                .eq(ContractProcess::getState, 0)
                .count();
        long rejectedCount = lambdaQuery()
                .eq(ContractProcess::getContractId, contractId)
                .eq(ContractProcess::getType, 2)
                .eq(ContractProcess::getState, 2)
                .count();
        return pendingCount == 0 && rejectedCount == 0;
    }

    private void saveContractState(Long contractId, Integer type) {
        ContractState state = new ContractState();
        state.setContractId(contractId);
        state.setType(type);
        state.setTime(LocalDateTime.now());
        contractStateMapper.insert(state);
    }

    private String getUsername(Long userId) {
        return (String) StpUtil.getSession().get("username");
    }
}
