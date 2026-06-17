package com.cmc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cmc.entity.ContractVersion;

import java.util.List;

/**
 * @author NAPH130
 */
public interface ContractVersionService extends IService<ContractVersion> {
    void saveVersion(Long contractId, String name, String content, String changeDesc, Long userId);
    List<ContractVersion> getVersions(Long contractId);
}
