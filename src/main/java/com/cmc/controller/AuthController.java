package com.cmc.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.cmc.common.R;
import com.cmc.dto.LoginDTO;
import com.cmc.dto.RegisterDTO;
import com.cmc.entity.User;
import com.cmc.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return R.ok("注册成功");
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        User user = userService.login(dto.getUsername(), dto.getPassword());
        Map<String, Object> data = new HashMap<>();
        data.put("token", StpUtil.getTokenValue());
        data.put("user", user);
        return R.ok(data);
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public R<User> me() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = (User) StpUtil.getSession().get("user");
        return R.ok(user);
    }
}
