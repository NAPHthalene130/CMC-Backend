package com.cmc.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.common.Constants;
import com.cmc.common.exception.BusinessException;
import com.cmc.dto.ContractDTO;
import com.cmc.entity.Contract;
import com.cmc.entity.ContractState;
import com.cmc.entity.Customer;
import com.cmc.entity.User;
import com.cmc.mapper.ContractMapper;
import com.cmc.mapper.ContractStateMapper;
import com.cmc.mapper.CustomerMapper;
import com.cmc.mapper.UserMapper;
import com.cmc.service.ContractProcessService;
import com.cmc.service.ContractService;
import com.cmc.service.ContractVersionService;
import com.cmc.service.LogService;
import com.cmc.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements ContractService {

    private final ContractStateMapper contractStateMapper;
    private final CustomerMapper customerMapper;
    private final UserMapper userMapper;
    private final LogService logService;
    private final NotificationService notificationService;
    private final ContractVersionService versionService;
    private final ContractProcessService processService;

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
        save(contract);

        ContractState state = new ContractState();
        state.setContractId(contract.getId());
        state.setType(Constants.CONTRACT_STATE_DRAFT);
        state.setTime(LocalDateTime.now());
        contractStateMapper.insert(state);

        versionService.saveVersion(contract.getId(), contract.getName(), contract.getContent(),
                "首次起草", userId);

        User operator = (User) StpUtil.getSession().get("user");
        if (operator != null) {
            logService.saveLog(operator.getId(), operator.getUsername(), "起草合同：" + contract.getName());

            List<User> admins = userMapper.selectList(
                    new LambdaQueryWrapper<User>().eq(User::getRoleId, Constants.ROLE_ADMIN_ID));
            for (User admin : admins) {
                notificationService.sendNotification(admin.getId(),
                        "新合同待分配",
                        "用户「" + operator.getUsername() + "」起草了合同「" + contract.getName() + "」，请及时分配",
                        "CONTRACT", contract.getId());
            }
        }

        return contract;
    }

    @Override
    public Contract finalize(Long id, ContractDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        return finalize(id, dto, userId);
    }

    @Override
    @Transactional
    public Contract finalize(Long id, ContractDTO dto, Long userId) {
        Contract contract = getById(id);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }
        if (!contract.getUserId().equals(userId)) {
            throw new BusinessException("仅合同起草人可进行定稿操作");
        }

        Integer currentState = processService.getCurrentState(id);
        if (currentState == null) {
            throw new BusinessException("合同状态异常");
        }
        if (!currentState.equals(Constants.CONTRACT_STATE_COUNTERSIGNED)) {
            if (currentState.equals(Constants.CONTRACT_STATE_DRAFT)) {
                List<com.cmc.entity.ContractProcess> processes = new ArrayList<>(
                        processService.lambdaQuery()
                                .eq(com.cmc.entity.ContractProcess::getContractId, id)
                                .eq(com.cmc.entity.ContractProcess::getType, Constants.PROCESS_TYPE_COUNTERSIGN)
                                .list());
                boolean hasCountersigners = !processes.isEmpty();
                if (hasCountersigners) {
                    throw new BusinessException("会签尚未全部完成，请等待所有会签人员完成会签后再定稿");
                }
            } else {
                String[] names = {null, "起草", "会签完成", "定稿完成", "审批完成", "签订完成"};
                String name = currentState < names.length ? names[currentState] : "状态" + currentState;
                throw new BusinessException("合同当前状态为「" + name + "」，无法重复定稿");
            }
        }

        contract.setName(dto.getName());
        contract.setContent(dto.getContent());
        contract.setCustomerId(dto.getCustomerId());
        contract.setBeginTime(dto.getBeginTime());
        contract.setEndTime(dto.getEndTime());
        updateById(contract);

        ContractState state = new ContractState();
        state.setContractId(contract.getId());
        state.setType(Constants.CONTRACT_STATE_FINALIZED);
        state.setTime(LocalDateTime.now());
        contractStateMapper.insert(state);

        Long currentUserId = StpUtil.getLoginIdAsLong();
        versionService.saveVersion(contract.getId(), contract.getName(), contract.getContent(),
                "定稿修订", currentUserId);

        User operator = (User) StpUtil.getSession().get("user");
        if (operator != null) {
            logService.saveLog(operator.getId(), operator.getUsername(), "定稿合同：" + contract.getName());
        }

        List<User> admins = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getRoleId, Constants.ROLE_ADMIN_ID));
        for (User admin : admins) {
            notificationService.sendNotification(admin.getId(),
                    "合同已定稿待审批",
                    "合同「" + contract.getName() + "」已定稿，请关注审批进度",
                    "CONTRACT", contract.getId());
        }

        return contract;
    }

    @Override
    public Page<Contract> pageContracts(long page, long pageSize, String keyword, Long userId) {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<Contract>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Contract::getName, keyword)
                    .or().like(Contract::getNum, keyword));
        }
        if (userId != null) {
            wrapper.and(w -> w
                    .eq(Contract::getUserId, userId)
                    .or()
                    .exists("SELECT 1 FROM contract_process cp WHERE cp.contract_id = contract.id AND cp.user_id = {0}", userId));
        }
        wrapper.orderByDesc(Contract::getCreateTime);
        Page<Contract> result = page(new Page<>(page, pageSize), wrapper);

        fillStates(result.getRecords());
        fillDraftUsers(result.getRecords());
        fillCustomerNames(result.getRecords());
        return result;
    }

    @Override
    public Page<Contract> pageByState(long page, long pageSize, Integer stateType, String keyword, Long userId) {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<Contract>()
                .orderByDesc(Contract::getCreateTime);
        if (stateType != null) {
            wrapper.apply("(SELECT MAX(cs.type) FROM contract_state cs WHERE cs.contract_id = contract.id) = {0}", stateType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Contract::getName, keyword)
                    .or().like(Contract::getNum, keyword));
        }
        if (userId != null) {
            wrapper.and(w -> w
                    .eq(Contract::getUserId, userId)
                    .or()
                    .exists("SELECT 1 FROM contract_process cp WHERE cp.contract_id = contract.id AND cp.user_id = {0}", userId));
        }
        Page<Contract> result = page(new Page<>(page, pageSize), wrapper);

        fillStates(result.getRecords());
        fillDraftUsers(result.getRecords());
        fillCustomerNames(result.getRecords());
        return result;
    }

    private void fillStates(List<Contract> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return;
        }
        Set<Long> contractIds = contracts.stream()
                .map(Contract::getId)
                .collect(Collectors.toSet());

        List<ContractState> states = contractStateMapper.selectList(
                new LambdaQueryWrapper<ContractState>()
                        .in(ContractState::getContractId, contractIds));

        Map<Long, Integer> stateMap = states.stream()
                .collect(Collectors.groupingBy(ContractState::getContractId,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparingInt(ContractState::getType)),
                                opt -> opt.map(ContractState::getType).orElse(null))));

        contracts.forEach(c -> c.setState(stateMap.get(c.getId())));
    }

    private void fillDraftUsers(List<Contract> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return;
        }
        Set<Long> userIds = contracts.stream()
                .map(Contract::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, String> usernameMap = users.stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        contracts.forEach(c -> {
            if (c.getUserId() != null) {
                c.setDraftUser(usernameMap.get(c.getUserId()));
            }
        });
    }

    private void fillCustomerNames(List<Contract> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return;
        }
        Set<Long> customerIds = contracts.stream()
                .map(Contract::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (customerIds.isEmpty()) {
            return;
        }
        List<Customer> customers = customerMapper.selectBatchIds(customerIds);
        Map<Long, String> nameMap = customers.stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
        contracts.forEach(c -> {
            if (c.getCustomerId() != null) {
                c.setCustomerName(nameMap.get(c.getCustomerId()));
            }
        });
    }
}