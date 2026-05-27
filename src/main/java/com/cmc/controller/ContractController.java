package com.cmc.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cmc.common.R;
import com.cmc.common.PageResult;
import com.cmc.dto.ContractDTO;
import com.cmc.entity.Contract;
import com.cmc.entity.ContractAttachment;
import com.cmc.service.ContractService;
import com.cmc.vo.ContractStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

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

    @Operation(summary = "上传合同附件")
    @PostMapping("/{id}/attachments")
    public R<ContractAttachment> uploadAttachment(@PathVariable Long id,
                                                  @RequestParam("file") MultipartFile file) throws IOException {
        return R.ok(contractService.uploadAttachment(id, file));
    }

    @Operation(summary = "查询合同附件")
    @GetMapping("/{id}/attachments")
    public R<List<ContractAttachment>> listAttachments(@PathVariable Long id) {
        return R.ok(contractService.listAttachments(id));
    }

    @Operation(summary = "下载合同附件")
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        ContractAttachment attachment = contractService.getAttachment(attachmentId);
        if (attachment == null) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(attachment.getPath());
        String filename = URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .body(resource);
    }

    @Operation(summary = "分页查询合同")
    @GetMapping
    public R<PageResult<Contract>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Integer stateType) {
        Page<Contract> result = stateType == null
                ? contractService.pageContracts(page, pageSize, keyword)
                : contractService.pageByState(page, pageSize, stateType, keyword);
        return R.ok(PageResult.of(result));
    }

    @Operation(summary = "合同状态统计")
    @GetMapping("/stats")
    public R<ContractStatsVO> stats() {
        return R.ok(contractService.getStats());
    }

    @Operation(summary = "获取合同详情")
    @GetMapping("/{id}")
    public R<Contract> getById(@PathVariable Long id) {
        return R.ok(contractService.getById(id));
    }

    @Operation(summary = "删除合同")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        contractService.removeById(id);
        return R.ok();
    }
}
