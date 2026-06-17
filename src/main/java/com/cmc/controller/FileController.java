package com.cmc.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.cmc.common.R;
import com.cmc.entity.ContractAttachment;
import com.cmc.mapper.ContractAttachmentMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Tag(name = "文件管理")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    @Value("${app.upload.dir:./data}")
    private String uploadDir;

    @Value("${app.upload.chunk-size:5242880}")
    private long chunkSize;

    private final ContractAttachmentMapper attachmentMapper;

    private Path basePath;
    private Path chunkPath;

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
            chunkPath = basePath.resolve("chunks");
            Files.createDirectories(chunkPath);
            log.info("文件上传目录: {}, 分块目录: {}", basePath, chunkPath);
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

    private record ChunkInfo(String fileName, long fileSize, long chunkSize, int totalChunks,
                             Set<Integer> uploadedChunks, Map<String, Object> extra) {
        ChunkInfo(String fileName, long fileSize, long chunkSize, int totalChunks) {
            this(fileName, fileSize, chunkSize, totalChunks, new HashSet<>(), new HashMap<>());
        }
    }

    private Path chunkDir(String fileId) {
        return chunkPath.resolve(fileId);
    }

    private Path metaFile(String fileId) {
        return chunkDir(fileId).resolve(".meta");
    }

    private ChunkInfo readMeta(String fileId) throws IOException {
        Path meta = metaFile(fileId);
        if (!Files.exists(meta)) return null;
        return new ObjectMapper().readValue(meta.toFile(), ChunkInfo.class);
    }

    private void writeMeta(String fileId, ChunkInfo info) throws IOException {
        Path dir = chunkDir(fileId);
        Files.createDirectories(dir);
        new ObjectMapper().writeValue(metaFile(fileId).toFile(), info);
    }

    @Operation(summary = "检查分块上传状态（断点续传）")
    @GetMapping("/chunk/check")
    public R<Map<String, Object>> checkChunks(@RequestParam String fileId) {
        try {
            ChunkInfo info = readMeta(fileId);
            if (info == null) {
                return R.ok(Map.of("exists", false));
            }
            List<Integer> uploaded = new ArrayList<>(info.uploadedChunks());
            Collections.sort(uploaded);
            return R.ok(Map.of(
                    "exists", true,
                    "fileName", info.fileName(),
                    "fileSize", info.fileSize(),
                    "chunkSize", info.chunkSize(),
                    "totalChunks", info.totalChunks(),
                    "uploadedChunks", uploaded
            ));
        } catch (IOException e) {
            log.error("检查分块状态失败: {}", fileId, e);
            return R.fail("检查分块状态失败");
        }
    }

    @Operation(summary = "上传文件分块")
    @PostMapping("/chunk/upload")
    public R<Map<String, Object>> uploadChunk(@RequestParam("file") MultipartFile file,
                                               @RequestParam String fileId,
                                               @RequestParam int chunkIndex,
                                               @RequestParam int totalChunks,
                                               @RequestParam String fileName,
                                               @RequestParam long fileSize,
                                               @RequestParam(required = false) Long contractId) {
        try {
            Path dir = chunkDir(fileId);
            Files.createDirectories(dir);

            ChunkInfo info = readMeta(fileId);
            if (info == null) {
                long cs = Math.max(fileSize / totalChunks, 1);
                info = new ChunkInfo(fileName, fileSize, cs, totalChunks);
            }

            Path chunkFile = dir.resolve(String.valueOf(chunkIndex));
            file.transferTo(chunkFile.toFile());
            info.uploadedChunks().add(chunkIndex);
            writeMeta(fileId, info);
            log.info("分块上传: {} chunk {}/{}", fileId, chunkIndex + 1, totalChunks);

            boolean allDone = info.uploadedChunks().size() >= totalChunks;
            ContractAttachment attachment = null;
            if (allDone) {
                attachment = mergeChunks(fileId, info, contractId);
            }

            List<Integer> uploaded = new ArrayList<>(info.uploadedChunks());
            Collections.sort(uploaded);
            Map<String, Object> result = new HashMap<>();
            result.put("uploadedChunks", uploaded);
            result.put("totalChunks", totalChunks);
            result.put("completed", allDone);
            if (attachment != null) {
                result.put("attachment", attachment);
            }
            return R.ok(result);
        } catch (IOException e) {
            log.error("分块上传失败: {}", fileId, e);
            return R.fail("分块上传失败: " + e.getMessage());
        }
    }

    private ContractAttachment mergeChunks(String fileId, ChunkInfo info, Long contractId) throws IOException {
        String originalName = info.fileName();
        String ext = "";
        if (originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID().toString() + ext;
        Path targetPath = basePath.resolve(storedName);
        Path dir = chunkDir(fileId);

        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(targetPath))) {
            for (int i = 0; i < info.totalChunks(); i++) {
                Path chunkFile = dir.resolve(String.valueOf(i));
                if (!Files.exists(chunkFile)) {
                    throw new IOException("缺失分块 " + i);
                }
                Files.copy(chunkFile, out);
            }
        }

        ContractAttachment attachment = new ContractAttachment();
        attachment.setContractId(contractId);
        attachment.setFileName(originalName);
        attachment.setPath(storedName);
        attachment.setType(ext);
        attachment.setUploadTime(LocalDateTime.now());
        attachment.setFileSize(info.fileSize());
        attachment.setStatus(1);
        attachmentMapper.insert(attachment);

        deleteDirectory(dir);
        log.info("分块合并完成: {} -> {}", originalName, targetPath);
        return attachment;
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var s = Files.walk(dir)) {
                s.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
            }
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
