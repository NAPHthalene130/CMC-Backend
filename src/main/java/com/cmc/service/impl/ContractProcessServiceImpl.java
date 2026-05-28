package com.cmc.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.common.exception.BusinessException;
import com.cmc.dto.AssignDTO;
import com.cmc.dto.PendingTaskDTO;
import com.cmc.dto.ProcessDTO;
import com.cmc.entity.Contract;
import com.cmc.entity.ContractProcess;
import com.cmc.entity.ContractState;
import com.cmc.entity.User;
import com.cmc.mapper.ContractMapper;
import com.cmc.mapper.ContractProcessMapper;
import com.cmc.mapper.ContractStateMapper;
import com.cmc.service.ContractProcessService;
import com.cmc.service.LogService;
import com.cmc.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractProcessServiceImpl extends ServiceImpl<ContractProcessMapper, ContractProcess>
        implements ContractProcessService {

    private final ContractStateMapper contractStateMapper;
    private final ContractMapper contractMapper;
    private final LogService logService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void assignContract(AssignDTO dto) {
        Contract contract = contractMapper.selectById(dto.getContractId());
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }
        if (!Objects.equals(contract.getState(), 1)) {
            throw new BusinessException("只有起草完成的合同可以分配");
        }
        if (CollectionUtils.isEmpty(dto.getCountersignUserIds())
                || CollectionUtils.isEmpty(dto.getApproveUserIds())
                || CollectionUtils.isEmpty(dto.getSignUserIds())) {
            throw new BusinessException("会签、审批、签订人员均不能为空");
        }

        List<ContractProcess> processes = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        dto.getCountersignUserIds().forEach(userId -> processes.add(createProcess(dto.getContractId(), 1, userId, now)));
        dto.getApproveUserIds().forEach(userId -> processes.add(createProcess(dto.getContractId(), 2, userId, now)));
        dto.getSignUserIds().forEach(userId -> processes.add(createProcess(dto.getContractId(), 3, userId, now)));

        for (ContractProcess process : processes) {
            save(process);
        }

        for (ContractProcess process : processes.stream().filter(p -> p.getType() == 1).collect(Collectors.toList())) {
            notificationService.sendNotification(process.getUserId(),
                    "新的会签任务",
                    "合同《" + contract.getName() + "》已分配给您，请尽快处理",
                    "CONTRACT", contract.getId());
        }

        User operator = currentUser();
        logService.saveLog(operator.getId(), operator.getUsername(), "分配合同：" + contract.getName());
    }

    @Override
    public List<PendingTaskDTO> getPendingTasks(Long userId, Integer type) {
        return lambdaQuery()
                .eq(ContractProcess::getUserId, userId)
                .eq(type != null, ContractProcess::getType, type)
                .eq(ContractProcess::getState, 0)
                .list()
                .stream()
                .map(this::toPendingTask)
                .filter(task -> task != null && taskVisibleForCurrentState(task.getState(), task.getType()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void countersign(Long userId, ProcessDTO dto) {
        ContractProcess process = getTaskOrFail(userId, dto.getContractId(), 1);
        Contract contract = contractMapper.selectById(dto.getContractId());
        ensureContractState(contract, 1, "当前合同不能会签");

        process.setState(1);
        process.setContent(dto.getContent());
        process.setTime(LocalDateTime.now());
        updateById(process);

        if (allCompleted(dto.getContractId(), 1)) {
            saveContractState(dto.getContractId(), 2);
            contract.setState(2);
            contractMapper.updateById(contract);
            notificationService.sendNotification(contract.getUserId(),
                    "会签全部完成",
                    "合同《" + contract.getName() + "》的所有会签已完成，请进行定稿",
                    "CONTRACT", contract.getId());
        }

        logService.saveLog(userId, getUsername(userId), "会签合同：" + contract.getName());
    }

    @Override
    @Transactional
    public void approve(Long userId, ProcessDTO dto) {
        ContractProcess process = getTaskOrFail(userId, dto.getContractId(), 2);
        Contract contract = contractMapper.selectById(dto.getContractId());
        ensureContractState(contract, 3, "当前合同不能审批");

        boolean approved = Boolean.TRUE.equals(dto.getApproved());
        process.setState(approved ? 1 : 2);
        process.setContent(dto.getContent());
        process.setTime(LocalDateTime.now());
        updateById(process);

        if (approved && allApproved(dto.getContractId())) {
            saveContractState(dto.getContractId(), 4);
            contract.setState(4);
            contractMapper.updateById(contract);
            notifyPendingUsers(dto.getContractId(), 3, "新的签订任务",
                    "合同《" + contract.getName() + "》已通过审批，请进行签订");
        } else if (!approved) {
            saveContractState(dto.getContractId(), 2);
            contract.setState(2);
            contractMapper.updateById(contract);
            notificationService.sendNotification(contract.getUserId(),
                    "审批被拒绝",
                    "合同《" + contract.getName() + "》审批未通过，请查看审批意见并重新定稿",
                    "CONTRACT", contract.getId());
        }

        logService.saveLog(userId, getUsername(userId), "审批合同：" + contract.getName());
    }

    @Override
    @Transactional
    public void sign(Long userId, ProcessDTO dto) {
        ContractProcess process = getTaskOrFail(userId, dto.getContractId(), 3);
        Contract contract = contractMapper.selectById(dto.getContractId());
        ensureContractState(contract, 4, "当前合同不能签订");

        process.setState(1);
        process.setContent(dto.getContent());
        process.setTime(LocalDateTime.now());
        updateById(process);

        if (allCompleted(dto.getContractId(), 3)) {
            saveContractState(dto.getContractId(), 5);
            contract.setState(5);
            contractMapper.updateById(contract);
            notificationService.sendNotification(contract.getUserId(),
                    "签订全部完成",
                    "合同《" + contract.getName() + "》的所有签订已完成，合同流程结束",
                    "CONTRACT", contract.getId());
        }

        logService.saveLog(userId, getUsername(userId), "签订合同：" + contract.getName());
    }

    private ContractProcess createProcess(Long contractId, Integer type, Long userId, LocalDateTime now) {
        ContractProcess process = new ContractProcess();
        process.setContractId(contractId);
        process.setType(type);
        process.setState(0);
        process.setUserId(userId);
        process.setTime(now);
        return process;
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
        long pending = lambdaQuery()
                .eq(ContractProcess::getContractId, contractId)
                .eq(ContractProcess::getType, 2)
                .eq(ContractProcess::getState, 0)
                .count();
        long rejected = lambdaQuery()
                .eq(ContractProcess::getContractId, contractId)
                .eq(ContractProcess::getType, 2)
                .eq(ContractProcess::getState, 2)
                .count();
        return pending == 0 && rejected == 0;
    }

    private void saveContractState(Long contractId, Integer type) {
        ContractState state = new ContractState();
        state.setContractId(contractId);
        state.setType(type);
        state.setTime(LocalDateTime.now());
        contractStateMapper.insert(state);
    }

    private PendingTaskDTO toPendingTask(ContractProcess process) {
        Contract contract = contractMapper.selectById(process.getContractId());
        if (contract == null) {
            return null;
        }
        PendingTaskDTO dto = new PendingTaskDTO();
        dto.setId(process.getId());
        dto.setProcessId(process.getId());
        dto.setContractId(contract.getId());
        dto.setContractNum(contract.getNum());
        dto.setContractName(contract.getName());
        dto.setCustomerId(contract.getCustomerId());
        dto.setContent(contract.getContent());
        dto.setProcessContent(process.getContent());
        dto.setType(process.getType());
        dto.setState(contract.getState());
        dto.setUserId(process.getUserId());
        dto.setBeginTime(contract.getBeginTime());
        dto.setEndTime(contract.getEndTime());
        dto.setTime(process.getTime());
        dto.setCreateTime(contract.getCreateTime());
        return dto;
    }

    private boolean taskVisibleForCurrentState(Integer contractState, Integer taskType) {
        return (taskType == 1 && Objects.equals(contractState, 1))
                || (taskType == 2 && Objects.equals(contractState, 3))
                || (taskType == 3 && Objects.equals(contractState, 4));
    }

    private void notifyPendingUsers(Long contractId, Integer type, String title, String content) {
        lambdaQuery()
                .eq(ContractProcess::getContractId, contractId)
                .eq(ContractProcess::getType, type)
                .eq(ContractProcess::getState, 0)
                .list()
                .forEach(process -> notificationService.sendNotification(
                        process.getUserId(), title, content, "CONTRACT", contractId));
    }

    private void ensureContractState(Contract contract, int expectedState, String message) {
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }
        if (!Objects.equals(contract.getState(), expectedState)) {
            throw new BusinessException(message);
        }
    }

    private User currentUser() {
        User user = (User) StpUtil.getSession().get("user");
        if (user == null) {
            throw new BusinessException("当前用户信息不存在");
        }
        return user;
    }

    private String getUsername(Long userId) {
        User user = (User) StpUtil.getSession().get("user");
        return user != null ? user.getUsername() : String.valueOf(userId);
    }
}
