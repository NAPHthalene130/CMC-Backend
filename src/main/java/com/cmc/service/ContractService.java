package com.cmc.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cmc.dto.ContractDTO;
import com.cmc.entity.Contract;
import com.cmc.entity.ContractAttachment;
import com.cmc.vo.ContractStatsVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ContractService extends IService<Contract> {
    Contract draft(Long userId, ContractDTO dto);
    Contract finalize(Long id, ContractDTO dto);
    ContractAttachment uploadAttachment(Long contractId, MultipartFile file) throws IOException;
    List<ContractAttachment> listAttachments(Long contractId);
    ContractAttachment getAttachment(Long attachmentId);
    Page<Contract> pageContracts(long page, long pageSize, String keyword);
    Page<Contract> pageByState(long page, long pageSize, Integer stateType, String keyword);
    ContractStatsVO getStats();
}
