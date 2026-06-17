package com.cmc.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.cmc.common.R;
import com.cmc.common.exception.BusinessException;
import com.cmc.dto.LoginDTO;
import com.cmc.dto.RegisterDTO;
import com.cmc.entity.Role;
import com.cmc.entity.User;
import com.cmc.service.LogService;
import com.cmc.service.RoleService;
import com.cmc.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    private final LogService logService;
    private final RoleService roleService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return R.ok("注册成功");
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        User user;
        try {
            user = userService.login(dto.getUsername(), dto.getPassword());
        } catch (BusinessException e) {
            logService.saveLoginLog(null, dto.getUsername(), getClientIp(request),
                    request.getHeader("User-Agent"), 0, e.getMessage());
            throw e;
        }

        logService.saveLoginLog(user.getId(), user.getUsername(), getClientIp(request),
                request.getHeader("User-Agent"), 1, "登录成功");

        // 清除敏感字段再返回
        user.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("token", StpUtil.getTokenValue());
        data.put("user", user);

        if (user.getRoleId() != null) {
            Role role = roleService.getById(user.getRoleId());
            data.put("role", role != null ? role.getName() : "");
        } else {
            data.put("role", "");
        }
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
    public R<Map<String, Object>> me() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        if (user == null) {
            return R.fail(401, "用户不存在");
        }
        // 清除敏感字段再返回
        user.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("userInfo", user);
        if (user.getRoleId() != null) {
            Role role = roleService.getById(user.getRoleId());
            data.put("role", role != null ? role.getName() : "");
            data.put("permissions", role != null ? role.getFunctions() : "");
        } else {
            data.put("role", "");
            data.put("permissions", "");
        }
        return R.ok(data);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
