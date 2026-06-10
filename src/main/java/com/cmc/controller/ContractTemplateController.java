package com.cmc.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cmc.common.R;
import com.cmc.common.PageResult;
import com.cmc.entity.ContractTemplate;
import com.cmc.service.ContractTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author NAPH130
 */
@SaCheckRole("ADMIN")
@Tag(name = "合同模板管理")
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class ContractTemplateController {

    private final ContractTemplateService templateService;

    @Operation(summary = "分页查询模板")
    @GetMapping
    public R<PageResult<ContractTemplate>> page(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long pageSize,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String category) {
        Page<ContractTemplate> result = templateService.pageTemplates(page, pageSize, keyword, category);
        return R.ok(PageResult.of(result));
    }

    @Operation(summary = "获取模板列表")
    @GetMapping("/list")
    public R<java.util.List<ContractTemplate>> list() {
        return R.ok(templateService.list());
    }

    @Operation(summary = "获取模板详情")
    @GetMapping("/{id}")
    public R<ContractTemplate> getById(@PathVariable Long id) {
        return R.ok(templateService.getById(id));
    }

    @Operation(summary = "新增模板")
    @PostMapping
    public R<ContractTemplate> add(@RequestBody ContractTemplate template) {
        templateService.save(template);
        return R.ok(template);
    }

    @Operation(summary = "修改模板")
    @PutMapping("/{id}")
    public R<ContractTemplate> update(@PathVariable Long id, @RequestBody ContractTemplate template) {
        template.setId(id);
        templateService.updateById(template);
        return R.ok(template);
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        templateService.removeById(id);
        return R.ok();
    }
}
