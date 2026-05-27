package com.cmc.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.common.exception.BusinessException;
import com.cmc.dto.ContractDTO;
import com.cmc.entity.Contract;
import com.cmc.entity.ContractAttachment;
import com.cmc.entity.ContractState;
import com.cmc.entity.User;
import com.cmc.mapper.ContractAttachmentMapper;
import com.cmc.mapper.ContractMapper;
import com.cmc.mapper.ContractStateMapper;
import com.cmc.service.ContractService;
import com.cmc.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements ContractService {

    private final ContractStateMapper contractStateMapper;
    private final ContractAttachmentMapper contractAttachmentMapper;
    private final LogService logService;

    @Value("${cmc.upload-dir:upload}")
    private String uploadDir;

    @Override
    @Transactional
    public Contract draft(Long userId, ContractDTO dto) {
        validateContractTime(dto);
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
        validateContractTime(dto);
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
    public ContractAttachment uploadAttachment(Long contractId, MultipartFile file) throws IOException {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("附件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        if (!List.of("doc", "jpg", "jpeg", "png", "bmp", "gif").contains(extension)) {
            throw new BusinessException("附件格式不正确");
        }

        Path contractDir = Paths.get(uploadDir, "contracts", String.valueOf(contractId));
        Files.createDirectories(contractDir);
        String storedFilename = UUID.randomUUID() + "." + extension;
        Path storedPath = contractDir.resolve(storedFilename);
        file.transferTo(storedPath.toFile());

        ContractAttachment attachment = new ContractAttachment();
        attachment.setContractId(contractId);
        attachment.setFileName(originalFilename);
        attachment.setPath(storedPath.toString());
        attachment.setType(extension);
        attachment.setUploadTime(LocalDateTime.now());
        contractAttachmentMapper.insert(attachment);

        User operator = (User) StpUtil.getSession().get("user");
        logService.saveLog(operator.getId(), operator.getUsername(), "上传合同附件：" + contract.getName());
        return attachment;
    }

    @Override
    public List<ContractAttachment> listAttachments(Long contractId) {
        return contractAttachmentMapper.selectList(new LambdaQueryWrapper<ContractAttachment>()
                .eq(ContractAttachment::getContractId, contractId)
                .orderByDesc(ContractAttachment::getUploadTime));
    }

    @Override
    public ContractAttachment getAttachment(Long attachmentId) {
        return contractAttachmentMapper.selectById(attachmentId);
    }

    @Override
    public Page<Contract> pageContracts(long page, long pageSize, String keyword) {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<Contract>()
                .and(StringUtils.hasText(keyword), w -> w.like(Contract::getName, keyword)
                        .or()
                        .like(Contract::getNum, keyword))
                .orderByDesc(Contract::getCreateTime);
        return page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public Page<Contract> pageByState(long page, long pageSize, Integer stateType, String keyword) {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<Contract>()
                .inSql(stateType != null, Contract::getId,
                        "select cs.contract_id from contract_state cs " +
                                "join (select contract_id, max(time) max_time from contract_state group by contract_id) latest " +
                                "on cs.contract_id = latest.contract_id and cs.time = latest.max_time " +
                                "where cs.type = " + stateType)
                .and(StringUtils.hasText(keyword), w -> w.like(Contract::getName, keyword)
                        .or()
                        .like(Contract::getNum, keyword))
                .orderByDesc(Contract::getCreateTime);
        return page(new Page<>(page, pageSize), wrapper);
    }

    private void validateContractTime(ContractDTO dto) {
        if (dto.getBeginTime() != null && dto.getEndTime() != null && dto.getEndTime().isBefore(dto.getBeginTime())) {
            throw new BusinessException("结束时间不能早于开始时间");
        }
    }

    private String getExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            throw new BusinessException("附件格式不正确");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
