package com.cmc.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.cmc.common.R;
import com.cmc.dto.LoginDTO;
import com.cmc.dto.RegisterDTO;
import com.cmc.entity.LoginLog;
import com.cmc.entity.Role;
import com.cmc.entity.User;
import com.cmc.mapper.LoginLogMapper;
import com.cmc.mapper.RoleMapper;
import com.cmc.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final LoginLogMapper loginLogMapper;
    private final RoleMapper roleMapper;

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
        } catch (Exception e) {
            recordLoginLog(null, dto.getUsername(), request, 0, e.getMessage());
            throw e;
        }

        recordLoginLog(user.getId(), user.getUsername(), request, 1, "登录成功");
        Map<String, Object> data = new HashMap<>();
        data.put("token", StpUtil.getTokenValue());
        data.put("user", user);

        if (user.getRoleId() != null) {
            Role role = roleMapper.selectById(user.getRoleId());
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
        Map<String, Object> data = new HashMap<>();
        data.put("userInfo", user);
        if (user.getRoleId() != null) {
            Role role = roleMapper.selectById(user.getRoleId());
            data.put("role", role != null ? role.getName() : "");
            data.put("permissions", role != null ? role.getFunctions() : "");
        } else {
            data.put("role", "");
            data.put("permissions", "");
        }
        return R.ok(data);
    }

    private void recordLoginLog(Long userId, String username, HttpServletRequest request, int status, String msg) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setIp(getClientIp(request));
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setStatus(status);
        log.setMsg(msg);
        log.setTime(LocalDateTime.now());
        loginLogMapper.insert(log);
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
