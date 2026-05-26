package com.cmc.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cmc.common.R;
import com.cmc.common.PageResult;
import com.cmc.dto.UserDTO;
import com.cmc.entity.User;
import com.cmc.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckRole("ADMIN")
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping
    public R<PageResult<User>> page(@RequestParam(defaultValue = "1") long page,
                                     @RequestParam(defaultValue = "10") long pageSize,
                                     @RequestParam(required = false) String keyword) {
        Page<User> result = userService.pageUsers(page, pageSize, keyword);
        return R.ok(PageResult.of(result));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public R<User> getById(@PathVariable Long id) {
        return R.ok(userService.getById(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public R<User> add(@Valid @RequestBody UserDTO dto) {
        return R.ok(userService.addUser(dto));
    }

    @Operation(summary = "修改用户")
    @PutMapping("/{id}")
    public R<User> update(@PathVariable Long id, @RequestBody UserDTO dto) {
        return R.ok(userService.updateUser(id, dto));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return R.ok();
    }
}
