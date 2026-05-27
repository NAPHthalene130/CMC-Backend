package com.cmc.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.cmc.common.R;
import com.cmc.dto.AssignDTO;
import com.cmc.dto.ProcessDTO;
import com.cmc.entity.ContractProcess;
import com.cmc.service.ContractProcessService;
import com.cmc.vo.ProcessTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "合同流程管理")
@RestController
@RequestMapping("/api/process")
@RequiredArgsConstructor
public class ContractProcessController {

    private final ContractProcessService processService;

    @Operation(summary = "分配合同（管理员）")
    @PostMapping("/assign")
    public R<Void> assign(@Valid @RequestBody AssignDTO dto) {
        processService.assignContract(dto);
        return R.ok("分配成功");
    }

    @Operation(summary = "获取我的待办任务")
    @GetMapping("/pending")
    public R<List<ProcessTaskVO>> pending(@RequestParam(required = false) Integer type) {
        long userId = StpUtil.getLoginIdAsLong();
        return R.ok(processService.getPendingTasks(userId, type));
    }

    @Operation(summary = "查询合同流程记录")
    @GetMapping("/contracts/{contractId}")
    public R<List<ProcessTaskVO>> contractProcesses(@PathVariable Long contractId,
                                                    @RequestParam(required = false) Integer type) {
        return R.ok(processService.getContractProcesses(contractId, type));
    }

    @Operation(summary = "会签合同")
    @PostMapping("/countersign")
    public R<Void> countersign(@Valid @RequestBody ProcessDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        processService.countersign(userId, dto);
        return R.ok("会签成功");
    }

    @Operation(summary = "审批合同")
    @PostMapping("/approve")
    public R<Void> approve(@Valid @RequestBody ProcessDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        processService.approve(userId, dto);
        return R.ok("审批完成");
    }

    @Operation(summary = "签订合同")
    @PostMapping("/sign")
    public R<Void> sign(@Valid @RequestBody ProcessDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        processService.sign(userId, dto);
        return R.ok("签订成功");
    }
}
