package com.cmc.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.cmc.common.R;
import com.cmc.dto.LoginDTO;
import com.cmc.dto.RegisterDTO;
import com.cmc.entity.LoginLog;
import com.cmc.entity.User;
import com.cmc.mapper.LoginLogMapper;
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

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return R.ok("注册成功");
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        try {
            User user = userService.login(dto.getUsername(), dto.getPassword());
            recordLoginLog(user.getId(), user.getUsername(), request, 1, "登录成功");
            Map<String, Object> data = new HashMap<>();
            data.put("token", StpUtil.getTokenValue());
            data.put("user", user);
            return R.ok(data);
        } catch (Exception e) {
            recordLoginLog(null, dto.getUsername(), request, 0, e.getMessage());
            throw e;
        }
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
