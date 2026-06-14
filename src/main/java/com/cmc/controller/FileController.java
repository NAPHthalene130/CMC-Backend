package com.cmc.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.cmc.common.R;
import com.cmc.entity.ContractAttachment;
import com.cmc.mapper.ContractAttachmentMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Tag(name = "文件管理")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    @Value("${app.upload.dir:./data}")
    private String uploadDir;

    private final ContractAttachmentMapper attachmentMapper;

    /** 解析后的绝对路径，所有文件操作统一使用此路径 */
    private Path basePath;

    @PostConstruct
    public void init() {
        Path configured = Paths.get(uploadDir);
        if (configured.isAbsolute()) {
            basePath = configured;
        } else {
            // 相对路径 → 基于 user.dir（java -jar 的启动目录）解析为绝对路径
            basePath = Paths.get(System.getProperty("user.dir")).resolve(uploadDir).normalize().toAbsolutePath();
        }
        try {
            Files.createDirectories(basePath);
            log.info("文件上传目录: {}", basePath);
        } catch (IOException e) {
            log.error("无法创建文件上传目录: {}", basePath, e);
        }
    }

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

            // 确保目录存在
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
            }

            Path targetPath = basePath.resolve(storedName);
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

            log.info("文件上传成功: {} -> {}", originalName, targetPath);
            return R.ok(attachment);
        } catch (IOException e) {
            log.error("文件上传失败", e);
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

        Path filePath = basePath.resolve(attachment.getPath());
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

    @Operation(summary = "预览合同附件（图片/PDF内联显示）")
    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> preview(@PathVariable Long id) {
        ContractAttachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = basePath.resolve(attachment.getPath());
        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = resolveMediaType(attachment.getType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + attachment.getFileName() + "\"")
                .body(resource);
    }

    private MediaType resolveMediaType(String ext) {
        if (ext == null) return MediaType.APPLICATION_OCTET_STREAM;
        return switch (ext.toLowerCase()) {
            case ".jpg", ".jpeg" -> MediaType.IMAGE_JPEG;
            case ".png" -> MediaType.IMAGE_PNG;
            case ".gif" -> MediaType.IMAGE_GIF;
            case ".bmp" -> MediaType.valueOf("image/bmp");
            case ".webp" -> MediaType.valueOf("image/webp");
            case ".svg" -> MediaType.valueOf("image/svg+xml");
            case ".pdf" -> MediaType.APPLICATION_PDF;
            case ".txt", ".md", ".csv", ".log", ".sql", ".yaml", ".yml",
                 ".json", ".xml", ".ini", ".cfg", ".properties",
                 ".java", ".py", ".js", ".ts", ".sh", ".bat", ".cmd" -> MediaType.TEXT_PLAIN;
            case ".html", ".htm" -> MediaType.TEXT_HTML;
            case ".css" -> MediaType.valueOf("text/css");
            case ".doc" -> MediaType.valueOf("application/msword");
            case ".docx" -> MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case ".xls", ".xlsx" -> MediaType.valueOf("application/vnd.ms-excel");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
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

    @SaCheckRole("ADMIN")
    @Operation(summary = "删除附件")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        ContractAttachment attachment = attachmentMapper.selectById(id);
        if (attachment != null) {
            try {
                Files.deleteIfExists(basePath.resolve(attachment.getPath()));
            } catch (IOException ignored) {
            }
            attachmentMapper.deleteById(id);
        }
        return R.ok();
    }
}
