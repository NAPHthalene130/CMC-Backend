package com.cmc.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.common.exception.BusinessException;
import com.cmc.dto.ContractDTO;
import com.cmc.entity.Contract;
import com.cmc.entity.ContractProcess;
import com.cmc.entity.ContractState;
import com.cmc.entity.User;
import com.cmc.mapper.ContractMapper;
import com.cmc.mapper.ContractProcessMapper;
import com.cmc.mapper.ContractStateMapper;
import com.cmc.mapper.UserMapper;
import com.cmc.service.ContractService;
import com.cmc.service.ContractVersionService;
import com.cmc.service.LogService;
import com.cmc.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements ContractService {

    private final ContractStateMapper contractStateMapper;
    private final UserMapper userMapper;
    private final ContractProcessMapper contractProcessMapper;
    private final LogService logService;
    private final NotificationService notificationService;
    private final ContractVersionService versionService;

    @Override
    @Transactional
    public Contract draft(Long userId, ContractDTO dto) {
        Contract contract = new Contract();
        contract.setNum("HT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        contract.setName(dto.getName());
        contract.setCustomerId(dto.getCustomerId());
        contract.setBeginTime(dto.getBeginTime());
        contract.setEndTime(dto.getEndTime());
        contract.setContent(dto.getContent());
        contract.setUserId(userId);
        contract.setState(1);
        save(contract);

        saveContractState(contract.getId(), 1);
        versionService.saveVersion(contract.getId(), contract.getName(), contract.getContent(), "首次起草", userId);

        User operator = currentUser();
        logService.saveLog(operator.getId(), operator.getUsername(), "起草合同：" + contract.getName());

        List<User> admins = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getRoleId, 1));
        for (User admin : admins) {
            notificationService.sendNotification(admin.getId(),
                    "新合同待分配",
                    "用户《" + operator.getUsername() + "》起草了合同《" + contract.getName() + "》，请及时分配",
                    "CONTRACT", contract.getId());
        }

        return contract;
    }

    @Override
    @Transactional
    public Contract finalize(Long id, ContractDTO dto) {
        Contract contract = getById(id);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }

        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!Objects.equals(contract.getUserId(), currentUserId)) {
            throw new BusinessException("只有合同起草人可以定稿");
        }
        if (!Objects.equals(contract.getState(), 2)) {
            throw new BusinessException("所有会签完成后才能定稿");
        }

        contract.setName(dto.getName());
        contract.setContent(dto.getContent());
        contract.setCustomerId(dto.getCustomerId());
        contract.setBeginTime(dto.getBeginTime());
        contract.setEndTime(dto.getEndTime());
        contract.setState(3);
        updateById(contract);

        saveContractState(contract.getId(), 3);
        versionService.saveVersion(contract.getId(), contract.getName(), contract.getContent(), "定稿修订", currentUserId);

        User operator = currentUser();
        logService.saveLog(operator.getId(), operator.getUsername(), "定稿合同：" + contract.getName());

        resetApprovalTasks(contract.getId());
        notifyPendingApprovers(contract);
        return contract;
    }

    @Override
    public Page<Contract> pageContracts(long page, long pageSize, String keyword) {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<Contract>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Contract::getName, keyword)
                        .or()
                        .like(Contract::getNum, keyword))
                .orderByDesc(Contract::getCreateTime);
        Page<Contract> result = page(new Page<>(page, pageSize), wrapper);
        fillDraftUsers(result.getRecords());
        return result;
    }

    @Override
    public Page<Contract> pageByState(long page, long pageSize, Integer stateType) {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<Contract>()
                .eq(stateType != null, Contract::getState, stateType)
                .orderByDesc(Contract::getCreateTime);
        if (Objects.equals(stateType, 1)) {
            wrapper.notExists("SELECT 1 FROM contract_process cp WHERE cp.contract_id = contract.id");
        }
        Page<Contract> result = page(new Page<>(page, pageSize), wrapper);
        fillDraftUsers(result.getRecords());
        return result;
    }

    private void saveContractState(Long contractId, Integer type) {
        ContractState state = new ContractState();
        state.setContractId(contractId);
        state.setType(type);
        state.setTime(LocalDateTime.now());
        contractStateMapper.insert(state);
    }

    private void notifyPendingApprovers(Contract contract) {
        List<ContractProcess> approvers = contractProcessMapper.selectList(
                new LambdaQueryWrapper<ContractProcess>()
                        .eq(ContractProcess::getContractId, contract.getId())
                        .eq(ContractProcess::getType, 2)
                        .eq(ContractProcess::getState, 0));
        for (ContractProcess process : approvers) {
            notificationService.sendNotification(process.getUserId(),
                    "新的审批任务",
                    "合同《" + contract.getName() + "》已定稿，请进行审批",
                    "CONTRACT", contract.getId());
        }
    }

    private void resetApprovalTasks(Long contractId) {
        List<ContractProcess> approvers = contractProcessMapper.selectList(
                new LambdaQueryWrapper<ContractProcess>()
                        .eq(ContractProcess::getContractId, contractId)
                        .eq(ContractProcess::getType, 2));
        for (ContractProcess process : approvers) {
            process.setState(0);
            process.setContent(null);
            process.setTime(null);
            contractProcessMapper.updateById(process);
        }
    }

    private void fillDraftUsers(List<Contract> contracts) {
        for (Contract contract : contracts) {
            if (contract.getUserId() != null) {
                User user = userMapper.selectById(contract.getUserId());
                if (user != null) {
                    contract.setDraftUser(user.getUsername());
                }
            }
        }
    }

    private User currentUser() {
        User user = (User) StpUtil.getSession().get("user");
        if (user == null) {
            throw new BusinessException("当前用户信息不存在");
        }
        return user;
    }
}
