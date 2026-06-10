package com.cmc.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.common.exception.BusinessException;
import com.cmc.dto.AssignDTO;
import com.cmc.dto.PendingTaskVO;
import com.cmc.dto.ProcessDTO;
import com.cmc.entity.Contract;
import com.cmc.entity.ContractProcess;
import com.cmc.entity.ContractState;
import com.cmc.entity.Customer;
import com.cmc.entity.User;
import com.cmc.mapper.ContractMapper;
import com.cmc.mapper.ContractProcessMapper;
import com.cmc.mapper.ContractStateMapper;
import com.cmc.mapper.CustomerMapper;
import com.cmc.service.ContractProcessService;
import com.cmc.service.LogService;
import com.cmc.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author NAPH130
 */
@Service
@RequiredArgsConstructor
public class ContractProcessServiceImpl extends ServiceImpl<ContractProcessMapper, ContractProcess>
        implements ContractProcessService {

    private final ContractStateMapper contractStateMapper;
    private final ContractMapper contractMapper;
    private final CustomerMapper customerMapper;
    private final LogService logService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void assignContract(AssignDTO dto) {
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

        Contract contract = contractMapper.selectById(dto.getContractId());
        String contractName = contract != null ? contract.getName() : "未知合同";

        for (ContractProcess p : processes) {
            String typeName = p.getType() == 1 ? "会签" : p.getType() == 2 ? "审批" : "签订";
            notificationService.sendNotification(p.getUserId(),
                    "新的" + typeName + "任务",
                    "合同「" + contractName + "」已分配给您，请尽快处理",
                    "CONTRACT", dto.getContractId());
        }

        User operator = (User) StpUtil.getSession().get("user");
        if (operator != null) {
            logService.saveLog(operator.getId(), operator.getUsername(),
                    "分配合同：" + dto.getContractId());
        }
    }

    @Override
    public List<PendingTaskVO> getPendingTasks(Long userId, Integer type) {
        List<ContractProcess> processes = lambdaQuery()
                .eq(ContractProcess::getUserId, userId)
                .eq(type != null, ContractProcess::getType, type)
                .eq(ContractProcess::getState, 0)
                .list();

        if (processes.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> contractIds = processes.stream()
                .map(ContractProcess::getContractId)
                .collect(Collectors.toSet());

        Map<Long, Contract> contractMap = contractMapper.selectBatchIds(contractIds).stream()
                .collect(Collectors.toMap(Contract::getId, c -> c));

        Set<Long> customerIds = contractMap.values().stream()
                .map(Contract::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> customerNameMap = customerIds.isEmpty() ? Collections.emptyMap()
                : customerMapper.selectBatchIds(customerIds).stream()
                        .collect(Collectors.toMap(Customer::getId, Customer::getName));

        return processes.stream().map(p -> {
            PendingTaskVO vo = new PendingTaskVO();
            vo.setId(p.getId());
            vo.setContractId(p.getContractId());
            vo.setType(p.getType());
            vo.setState(p.getState());
            vo.setUserId(p.getUserId());
            vo.setContent(p.getContent());
            vo.setTime(p.getTime());

            Contract contract = contractMap.get(p.getContractId());
            if (contract != null) {
                vo.setContractNum(contract.getNum());
                vo.setContractName(contract.getName());
                vo.setContractContent(contract.getContent());
                vo.setCreateTime(contract.getCreateTime());
                if (contract.getCustomerId() != null) {
                    vo.setCustomerName(customerNameMap.getOrDefault(contract.getCustomerId(), null));
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void countersign(Long userId, ProcessDTO dto) {
        ContractProcess process = getTaskOrFail(userId, dto.getContractId(), 1);
        process.setState(1);
        process.setContent(dto.getContent());
        process.setTime(LocalDateTime.now());
        updateById(process);

        Contract contract = contractMapper.selectById(dto.getContractId());
        String contractName = contract != null ? contract.getName() : "未知合同";

        if (allCompleted(dto.getContractId(), 1)) {
            saveContractState(dto.getContractId(), 2);
            if (contract != null) {
                notificationService.sendNotification(contract.getUserId(),
                        "会签全部完成",
                        "合同「" + contractName + "」的所有会签已完成，请进行定稿",
                        "CONTRACT", dto.getContractId());
            }
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

        Contract contract = contractMapper.selectById(dto.getContractId());
        String contractName = contract != null ? contract.getName() : "未知合同";

        if (dto.getApproved() != null && dto.getApproved() && allApproved(dto.getContractId())) {
            saveContractState(dto.getContractId(), 4);
            if (contract != null) {
                notificationService.sendNotification(contract.getUserId(),
                        "审批已通过",
                        "合同「" + contractName + "」已通过审批，请进行签订",
                        "CONTRACT", dto.getContractId());
            }
        } else if (dto.getApproved() != null && !dto.getApproved()) {
            if (contract != null) {
                notificationService.sendNotification(contract.getUserId(),
                        "审批被拒绝",
                        "合同「" + contractName + "」审批未通过，请查看审批意见并修改",
                        "CONTRACT", dto.getContractId());
            }
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

        Contract contract2 = contractMapper.selectById(dto.getContractId());
        String contractName2 = contract2 != null ? contract2.getName() : "未知合同";

        if (allCompleted(dto.getContractId(), 3)) {
            saveContractState(dto.getContractId(), 5);
            if (contract2 != null) {
                notificationService.sendNotification(contract2.getUserId(),
                        "签订全部完成",
                        "合同「" + contractName2 + "」的所有签订已完成，合同流程结束",
                        "CONTRACT", dto.getContractId());
            }
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
        long rejectedCount = lambdaQuery()
                .eq(ContractProcess::getContractId, contractId)
                .eq(ContractProcess::getType, 2)
                .eq(ContractProcess::getState, 2)
                .count();
        if (rejectedCount > 0) return false;
        return lambdaQuery()
                .eq(ContractProcess::getContractId, contractId)
                .eq(ContractProcess::getType, 2)
                .eq(ContractProcess::getState, 0)
                .count() == 0;
    }

    private void saveContractState(Long contractId, Integer type) {
        ContractState state = new ContractState();
        state.setContractId(contractId);
        state.setType(type);
        state.setTime(LocalDateTime.now());
        contractStateMapper.insert(state);
    }

    private String getUsername(Long userId) {
        User user = (User) StpUtil.getSession().get("user");
        return user != null ? user.getUsername() : "未知用户";
    }
}
