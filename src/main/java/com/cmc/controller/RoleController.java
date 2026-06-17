package com.cmc.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cmc.common.R;
import com.cmc.common.PageResult;
import com.cmc.dto.RoleDTO;
import com.cmc.entity.Role;
import com.cmc.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckRole("ADMIN")
@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "分页查询角色")
    @GetMapping
    public R<PageResult<Role>> page(@RequestParam(defaultValue = "1") long page,
                                     @RequestParam(defaultValue = "10") long pageSize,
                                     @RequestParam(required = false) String keyword) {
        Page<Role> result = roleService.pageRoles(page, pageSize, keyword);
        return R.ok(PageResult.of(result));
    }

    @Operation(summary = "获取角色列表")
    @GetMapping("/list")
    public R<java.util.List<Role>> list() {
        return R.ok(roleService.list());
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    public R<Role> getById(@PathVariable Long id) {
        return R.ok(roleService.getById(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    public R<Role> add(@Valid @RequestBody RoleDTO dto) {
        return R.ok(roleService.addRole(dto));
    }

    @Operation(summary = "修改角色")
    @PutMapping("/{id}")
    public R<Role> update(@PathVariable Long id, @RequestBody RoleDTO dto) {
        return R.ok(roleService.updateRole(id, dto));
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.removeById(id);
        return R.ok();
    }
}
