package com.cmc.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cmc.common.R;
import com.cmc.common.PageResult;
import com.cmc.entity.Function;
import com.cmc.service.FunctionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckRole("ADMIN")
@Tag(name = "功能操作管理")
@RestController
@RequestMapping("/api/functions")
@RequiredArgsConstructor
public class FunctionController {

    private final FunctionService functionService;

    @Operation(summary = "分页查询功能")
    @GetMapping
    public R<PageResult<Function>> page(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long pageSize,
                                         @RequestParam(required = false) String keyword) {
        Page<Function> result = functionService.pageFunctions(page, pageSize, keyword);
        return R.ok(PageResult.of(result));
    }

    @Operation(summary = "获取功能列表(树形)")
    @GetMapping("/list")
    public R<java.util.List<Function>> list() {
        return R.ok(functionService.listTree());
    }

    @Operation(summary = "新增功能")
    @PostMapping
    public R<Function> add(@RequestBody Function function) {
        functionService.save(function);
        return R.ok(function);
    }

    @Operation(summary = "修改功能")
    @PutMapping("/{id}")
    public R<Function> update(@PathVariable Long id, @RequestBody Function function) {
        function.setId(id);
        functionService.updateById(function);
        return R.ok(function);
    }

    @Operation(summary = "删除功能")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        functionService.removeById(id);
        return R.ok();
    }
}
