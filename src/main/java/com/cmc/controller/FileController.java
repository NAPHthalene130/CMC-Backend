package com.cmc.controller;

import com.cmc.common.R;
import com.cmc.entity.ContractAttachment;
import com.cmc.mapper.ContractAttachmentMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Tag(name = "文件管理")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    @Value("${app.upload.dir:upload}")
    private String uploadDir;

    private final ContractAttachmentMapper attachmentMapper;

    @Operation(summary = "上传合同附件")
    @PostMapping("/upload")
    public R<ContractAttachment> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam(required = false) Long contractId) {
        try {
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String storedName = UUID.randomUUID().toString() + ext;

            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path targetPath = uploadPath.resolve(storedName);
            file.transferTo(targetPath.toFile());

            ContractAttachment attachment = new ContractAttachment();
            attachment.setContractId(contractId);
            attachment.setFileName(originalName);
            attachment.setPath(storedName);
            attachment.setType(ext);
            attachment.setUploadTime(LocalDateTime.now());
            attachment.setFileSize(file.getSize());
            attachment.setStatus(1);
            attachmentMapper.insert(attachment);

            return R.ok(attachment);
        } catch (IOException e) {
            return R.fail("文件上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "下载合同附件")
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        ContractAttachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = Paths.get(uploadDir, attachment.getPath());
        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getFileName() + "\"")
                .body(resource);
    }

    @Operation(summary = "获取合同附件列表")
    @GetMapping("/contract/{contractId}")
    public R<java.util.List<ContractAttachment>> listByContract(@PathVariable Long contractId) {
        java.util.List<ContractAttachment> list = attachmentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ContractAttachment>()
                        .eq(ContractAttachment::getContractId, contractId)
                        .orderByDesc(ContractAttachment::getUploadTime)
        );
        return R.ok(list);
    }

    @Operation(summary = "删除附件")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        ContractAttachment attachment = attachmentMapper.selectById(id);
        if (attachment != null) {
            try {
                Files.deleteIfExists(Paths.get(uploadDir, attachment.getPath()));
            } catch (IOException ignored) {
            }
            attachmentMapper.deleteById(id);
        }
        return R.ok();
    }
}
