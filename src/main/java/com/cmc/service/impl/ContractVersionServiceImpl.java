package com.cmc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.entity.ContractVersion;
import com.cmc.mapper.ContractVersionMapper;
import com.cmc.service.ContractVersionService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author NAPH130
 */
@Service
public class ContractVersionServiceImpl extends ServiceImpl<ContractVersionMapper, ContractVersion>
        implements ContractVersionService {

    @Override
    public void saveVersion(Long contractId, String name, String content, String changeDesc, Long userId) {
        int maxVersion = 0;
        List<ContractVersion> existing = getVersions(contractId);
        if (!existing.isEmpty()) {
            maxVersion = existing.get(0).getVersionNum();
        }

        ContractVersion version = new ContractVersion();
        version.setContractId(contractId);
        version.setVersionNum(maxVersion + 1);
        version.setName(name);
        version.setContent(content);
        version.setChangeDesc(changeDesc);
        version.setCreateUserId(userId);
        save(version);
    }

    @Override
    public List<ContractVersion> getVersions(Long contractId) {
        return list(new LambdaQueryWrapper<ContractVersion>()
                .eq(ContractVersion::getContractId, contractId)
                .orderByDesc(ContractVersion::getVersionNum));
    }
}
