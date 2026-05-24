package com.cmc.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cmc.common.R;
import com.cmc.common.PageResult;
import com.cmc.entity.Log;
import com.cmc.service.LogService;
import com.cmc.utils.ExcelUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Tag(name = "日志管理")
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @Operation(summary = "分页查询日志")
    @GetMapping
    public R<PageResult<Log>> page(@RequestParam(defaultValue = "1") long page,
                                    @RequestParam(defaultValue = "10") long pageSize,
                                    @RequestParam(required = false) String keyword) {
        Page<Log> result = logService.pageLogs(page, pageSize, keyword);
        return R.ok(PageResult.of(result));
    }

    @Operation(summary = "导出日志（Excel）")
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String keyword,
                       HttpServletResponse response) throws IOException {
        Page<Log> result = logService.pageLogs(1, 10000, keyword);
        List<Log> data = result.getRecords();
        ExcelUtils.export(response, "操作日志", "日志", Log.class, data);
    }
}
