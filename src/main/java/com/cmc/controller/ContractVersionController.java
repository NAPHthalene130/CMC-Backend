package com.cmc.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.cmc.common.R;
import com.cmc.entity.ContractVersion;
import com.cmc.service.ContractVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author NAPH130
 */
@SaCheckLogin
@Tag(name = "合同版本管理")
@RestController
@RequestMapping("/api/contract-versions")
@RequiredArgsConstructor
public class ContractVersionController {

    private final ContractVersionService versionService;

    @Operation(summary = "获取合同版本历史")
    @GetMapping("/{contractId}")
    public R<List<ContractVersion>> list(@PathVariable Long contractId) {
        return R.ok(versionService.getVersions(contractId));
    }

    @Operation(summary = "获取版本详情")
    @GetMapping("/detail/{versionId}")
    public R<ContractVersion> detail(@PathVariable Long versionId) {
        return R.ok(versionService.getById(versionId));
    }
}
