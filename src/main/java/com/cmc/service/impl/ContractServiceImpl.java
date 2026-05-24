package com.cmc.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.common.exception.BusinessException;
import com.cmc.dto.ContractDTO;
import com.cmc.entity.Contract;
import com.cmc.entity.ContractState;
import com.cmc.entity.User;
import com.cmc.mapper.ContractMapper;
import com.cmc.mapper.ContractStateMapper;
import com.cmc.service.ContractService;
import com.cmc.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements ContractService {

    private final ContractStateMapper contractStateMapper;
    private final LogService logService;

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
        state.setType(1);
        state.setTime(LocalDateTime.now());
        contractStateMapper.insert(state);

        User operator = (User) StpUtil.getSession().get("user");
        logService.saveLog(operator.getId(), operator.getUsername(), "起草合同：" + contract.getName());
        return contract;
    }

    @Override
    @Transactional
    public Contract finalize(Long id, ContractDTO dto) {
        Contract contract = getById(id);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }
        contract.setName(dto.getName());
        contract.setContent(dto.getContent());
        contract.setCustomerId(dto.getCustomerId());
        contract.setBeginTime(dto.getBeginTime());
        contract.setEndTime(dto.getEndTime());
        updateById(contract);

        ContractState state = new ContractState();
        state.setContractId(contract.getId());
        state.setType(3);
        state.setTime(LocalDateTime.now());
        contractStateMapper.insert(state);

        User operator = (User) StpUtil.getSession().get("user");
        logService.saveLog(operator.getId(), operator.getUsername(), "定稿合同：" + contract.getName());
        return contract;
    }

    @Override
    public Page<Contract> pageContracts(long page, long pageSize, String keyword) {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<Contract>()
                .like(StringUtils.hasText(keyword), Contract::getName, keyword)
                .or().like(StringUtils.hasText(keyword), Contract::getNum, keyword)
                .orderByDesc(Contract::getCreateTime);
        return page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public Page<Contract> pageByState(long page, long pageSize, Integer stateType) {
        return null;
    }
}
