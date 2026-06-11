package com.cmc.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.common.Constants;
import com.cmc.common.exception.BusinessException;
import com.cmc.dto.AssignDTO;
import com.cmc.dto.PendingTaskVO;
import com.cmc.dto.ProcessDTO;
import com.cmc.entity.*;
import com.cmc.mapper.*;
import com.cmc.service.ContractProcessService;
import com.cmc.service.LogService;
import com.cmc.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractProcessServiceImpl extends ServiceImpl<ContractProcessMapper, ContractProcess>
        implements ContractProcessService {

    private final ContractStateMapper contractStateMapper;
    private final ContractMapper contractMapper;
    private final CustomerMapper customerMapper;
    private final UserMapper userMapper;
    private final LogService logService;
    private final NotificationService notificationService;

    @Override
    public Integer getCurrentState(Long contractId) {
        List<ContractState> states = contractStateMapper.selectList(
                new LambdaQueryWrapper<ContractState>()
                        .eq(ContractState::getContractId, contractId));
        if (states.isEmpty()) {
            return null;
        }
        return states.stream()
                .max(Comparator.comparingInt(ContractState::getType))
                .map(ContractState::getType)
                .orElse(null);
    }

    private static final String[] STATE_NAMES = {null, "起草", "会签完成", "定稿完成", "审批完成", "签订完成"};

    private void requireState(Long contractId, Integer expectedState, String actionName) {
        Integer current = getCurrentState(contractId);
        if (current == null) {
            throw new BusinessException("合同状态异常，无法" + actionName);
        }
        if (!current.equals(expectedState)) {
            String expected = expectedState < STATE_NAMES.length ? STATE_NAMES[expectedState] : "状态" + expectedState;
            String actual = current < STATE_NAMES.length ? STATE_NAMES[current] : "状态" + current;
            throw new BusinessException("合同当前状态为「" + actual + "」，需先完成「" + expected + "」才能" + actionName);
        }
    }

    private boolean hasCountersigners(Long contractId) {
        return lambdaQuery()
                .eq(ContractProcess::getContractId, contractId)
                .eq(ContractProcess::getType, Constants.PROCESS_TYPE_COUNTERSIGN)
                .count() > 0;
    }

    @Override
    @Transactional
    public void assignContract(AssignDTO dto) {
        List<ContractProcess> processes = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (dto.getCountersignUserIds() != null) {
            for (Long userId : dto.getCountersignUserIds()) {
                ContractProcess p = new ContractProcess();
                p.setContractId(dto.getContractId());
                p.setType(Constants.PROCESS_TYPE_COUNTERSIGN);
                p.setState(Constants.PROCESS_STATE_PENDING);
                p.setUserId(userId);
                p.setTime(now);
                processes.add(p);
            }
        }
        if (dto.getApproveUserIds() != null) {
            for (Long userId : dto.getApproveUserIds()) {
                ContractProcess p = new ContractProcess();
                p.setContractId(dto.getContractId());
                p.setType(Constants.PROCESS_TYPE_APPROVE);
                p.setState(Constants.PROCESS_STATE_PENDING);
                p.setUserId(userId);
                p.setTime(now);
                processes.add(p);
            }
        }
        if (dto.getSignUserIds() != null) {
            for (Long userId : dto.getSignUserIds()) {
                ContractProcess p = new ContractProcess();
                p.setContractId(dto.getContractId());
                p.setType(Constants.PROCESS_TYPE_SIGN);
                p.setState(Constants.PROCESS_STATE_PENDING);
                p.setUserId(userId);
                p.setTime(now);
                processes.add(p);
            }
        }

        saveBatch(processes);

        Contract contract = contractMapper.selectById(dto.getContractId());
        String contractName = contract != null ? contract.getName() : "未知合同";

        for (ContractProcess p : processes) {
            String typeName = p.getType().equals(Constants.PROCESS_TYPE_COUNTERSIGN) ? "会签"
                    : p.getType().equals(Constants.PROCESS_TYPE_APPROVE) ? "审批" : "签订";
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
                .eq(ContractProcess::getState, Constants.PROCESS_STATE_PENDING)
                .list();

        if (processes.isEmpty()) {
            return Collections.emptyList();
        }

        return buildTaskVOs(processes);
    }

    @Override
    public List<PendingTaskVO> getContractProcesses(Long contractId) {
        List<ContractProcess> processes = lambdaQuery()
                .eq(ContractProcess::getContractId, contractId)
                .orderByAsc(ContractProcess::getType)
                .orderByAsc(ContractProcess::getId)
                .list();

        if (processes.isEmpty()) {
            return Collections.emptyList();
        }

        return buildTaskVOs(processes);
    }

    private List<PendingTaskVO> buildTaskVOs(List<ContractProcess> processes) {
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

        Set<Long> userIds = processes.stream()
                .map(ContractProcess::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> userNameMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getUsername));

        return processes.stream().map(p -> {
            PendingTaskVO vo = new PendingTaskVO();
            vo.setId(p.getId());
            vo.setContractId(p.getContractId());
            vo.setType(p.getType());
            vo.setState(p.getState());
            vo.setUserId(p.getUserId());
            vo.setContent(p.getContent());
            vo.setTime(p.getTime());
            vo.setUsername(userNameMap.get(p.getUserId()));

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
        requireState(dto.getContractId(), Constants.CONTRACT_STATE_DRAFT, "会签");

        ContractProcess process = getTaskOrFail(userId, dto.getContractId(), Constants.PROCESS_TYPE_COUNTERSIGN);
        process.setState(Constants.PROCESS_STATE_COMPLETED);
        process.setContent(dto.getContent());
        process.setTime(LocalDateTime.now());
        updateById(process);

        Contract contract = contractMapper.selectById(dto.getContractId());
        String contractName = contract != null ? contract.getName() : "未知合同";

        if (allCompleted(dto.getContractId(), Constants.PROCESS_TYPE_COUNTERSIGN)) {
            saveContractState(dto.getContractId(), Constants.CONTRACT_STATE_COUNTERSIGNED);
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
        requireState(dto.getContractId(), Constants.CONTRACT_STATE_FINALIZED, "审批");

        ContractProcess process = getTaskOrFail(userId, dto.getContractId(), Constants.PROCESS_TYPE_APPROVE);
        process.setState(dto.getApproved() != null && dto.getApproved()
                ? Constants.PROCESS_STATE_COMPLETED : Constants.PROCESS_STATE_REJECTED);
        process.setContent(dto.getContent());
        process.setTime(LocalDateTime.now());
        updateById(process);

        Contract contract = contractMapper.selectById(dto.getContractId());
        String contractName = contract != null ? contract.getName() : "未知合同";

        if (dto.getApproved() != null && dto.getApproved() && allApproved(dto.getContractId())) {
            saveContractState(dto.getContractId(), Constants.CONTRACT_STATE_APPROVED);
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
                        "合同「" + contractName + "」审批未通过，请查看审批意见并修改后重新提交",
                        "CONTRACT", dto.getContractId());
            }
        }

        logService.saveLog(userId, getUsername(userId), "审批合同：" + dto.getContractId());
    }

    @Override
    @Transactional
    public void sign(Long userId, ProcessDTO dto) {
        requireState(dto.getContractId(), Constants.CONTRACT_STATE_APPROVED, "签订");

        ContractProcess process = getTaskOrFail(userId, dto.getContractId(), Constants.PROCESS_TYPE_SIGN);
        process.setState(Constants.PROCESS_STATE_COMPLETED);
        process.setContent(dto.getContent());
        process.setTime(LocalDateTime.now());
        updateById(process);

        Contract contract2 = contractMapper.selectById(dto.getContractId());
        String contractName2 = contract2 != null ? contract2.getName() : "未知合同";

        if (allCompleted(dto.getContractId(), Constants.PROCESS_TYPE_SIGN)) {
            saveContractState(dto.getContractId(), Constants.CONTRACT_STATE_SIGNED);
            if (contract2 != null) {
                notificationService.sendNotification(contract2.getUserId(),
                        "签订全部完成",
                        "合同「" + contractName2 + "」的所有签订已完成，合同流程结束",
                        "CONTRACT", dto.getContractId());
            }
        }

        logService.saveLog(userId, getUsername(userId), "签订合同：" + dto.getContractId());
    }

    @Override
    @Transactional
    public void redraft(Long userId, Long contractId) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }

        long rejectedCount = lambdaQuery()
                .eq(ContractProcess::getContractId, contractId)
                .eq(ContractProcess::getType, Constants.PROCESS_TYPE_APPROVE)
                .eq(ContractProcess::getState, Constants.PROCESS_STATE_REJECTED)
                .count();
        if (rejectedCount == 0) {
            throw new BusinessException("当前合同未被拒绝，无需重新起草");
        }

        lambdaUpdate()
                .eq(ContractProcess::getContractId, contractId)
                .eq(ContractProcess::getType, Constants.PROCESS_TYPE_APPROVE)
                .eq(ContractProcess::getState, Constants.PROCESS_STATE_REJECTED)
                .remove();

        lambdaUpdate()
                .eq(ContractProcess::getContractId, contractId)
                .eq(ContractProcess::getType, Constants.PROCESS_TYPE_APPROVE)
                .eq(ContractProcess::getState, Constants.PROCESS_STATE_COMPLETED)
                .remove();

        List<ContractState> states = contractStateMapper.selectList(
                new LambdaQueryWrapper<ContractState>()
                        .eq(ContractState::getContractId, contractId));
        for (ContractState s : states) {
            if (s.getType() >= Constants.CONTRACT_STATE_FINALIZED) {
                contractStateMapper.deleteById(s.getId());
            }
        }

        saveContractState(contractId, Constants.CONTRACT_STATE_DRAFT);

        List<User> admins = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getRoleId, Constants.ROLE_ADMIN_ID));
        for (User admin : admins) {
            notificationService.sendNotification(admin.getId(),
                    "合同重新提交",
                    "合同「" + contract.getName() + "」已被重新提交，请重新分配审批人员并进入审批流程",
                    "CONTRACT", contractId);
        }

        User operator = (User) StpUtil.getSession().get("user");
        if (operator != null) {
            logService.saveLog(operator.getId(), operator.getUsername(),
                    "重新起草合同：" + contract.getName());
        }
    }

    private ContractProcess getTaskOrFail(Long userId, Long contractId, Integer type) {
        ContractProcess process = lambdaQuery()
                .eq(ContractProcess::getUserId, userId)
                .eq(ContractProcess::getContractId, contractId)
                .eq(ContractProcess::getType, type)
                .eq(ContractProcess::getState, Constants.PROCESS_STATE_PENDING)
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
                .eq(ContractProcess::getState, Constants.PROCESS_STATE_PENDING)
                .count() == 0;
    }

    private boolean allApproved(Long contractId) {
        long rejectedCount = lambdaQuery()
                .eq(ContractProcess::getContractId, contractId)
                .eq(ContractProcess::getType, Constants.PROCESS_TYPE_APPROVE)
                .eq(ContractProcess::getState, Constants.PROCESS_STATE_REJECTED)
                .count();
        if (rejectedCount > 0) return false;
        return lambdaQuery()
                .eq(ContractProcess::getContractId, contractId)
                .eq(ContractProcess::getType, Constants.PROCESS_TYPE_APPROVE)
                .eq(ContractProcess::getState, Constants.PROCESS_STATE_PENDING)
                .count() == 0;
    }

    void saveContractState(Long contractId, Integer type) {
        ContractState state = new ContractState();
        state.setContractId(contractId);
        state.setType(type);
        state.setTime(LocalDateTime.now());
        contractStateMapper.insert(state);
    }

    private String getUsername(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getUsername() : "未知用户";
    }
}