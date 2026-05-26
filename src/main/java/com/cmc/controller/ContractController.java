package com.cmc.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cmc.common.R;
import com.cmc.common.PageResult;
import com.cmc.dto.ContractDTO;
import com.cmc.entity.Contract;
import com.cmc.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "合同管理")
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @Operation(summary = "起草合同")
    @PostMapping("/draft")
    public R<Contract> draft(@Valid @RequestBody ContractDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        return R.ok(contractService.draft(userId, dto));
    }

    @Operation(summary = "定稿合同")
    @PutMapping("/{id}/finalize")
    public R<Contract> finalize(@PathVariable Long id, @Valid @RequestBody ContractDTO dto) {
        return R.ok(contractService.finalize(id, dto));
    }

    @Operation(summary = "分页查询合同")
    @GetMapping
    public R<PageResult<Contract>> page(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long pageSize,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Integer state) {
        Page<Contract> result;
        if (state != null) {
            result = contractService.pageByState(page, pageSize, state);
        } else {
            result = contractService.pageContracts(page, pageSize, keyword);
        }
        return R.ok(PageResult.of(result));
    }

    @Operation(summary = "获取合同详情")
    @GetMapping("/{id}")
    public R<Contract> getById(@PathVariable Long id) {
        return R.ok(contractService.getById(id));
    }

    @SaCheckRole("ADMIN")
    @Operation(summary = "删除合同")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        contractService.removeById(id);
        return R.ok();
    }
}
