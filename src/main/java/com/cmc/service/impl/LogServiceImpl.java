package com.cmc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.entity.Log;
import com.cmc.entity.LoginLog;
import com.cmc.mapper.LogMapper;
import com.cmc.mapper.LoginLogMapper;
import com.cmc.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LogServiceImpl extends ServiceImpl<LogMapper, Log> implements LogService {

    private final LoginLogMapper loginLogMapper;

    @Override
    public Page<Log> pageLogs(long page, long pageSize, String keyword) {
        LambdaQueryWrapper<Log> wrapper = new LambdaQueryWrapper<Log>()
                .like(StringUtils.hasText(keyword), Log::getContent, keyword)
                .orderByDesc(Log::getTime);
        return page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public void saveLog(Long userId, String username, String content) {
        Log log = new Log();
        log.setUserId(userId);
        log.setUsername(username);
        log.setContent(content);
        log.setTime(LocalDateTime.now());
        log.setType("OPERATION");
        save(log);
    }

    @Override
    public void saveLoginLog(Long userId, String username, String ip, String userAgent, int status, String msg) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setIp(ip);
        log.setUserAgent(userAgent);
        log.setStatus(status);
        log.setMsg(msg);
        log.setTime(LocalDateTime.now());
        loginLogMapper.insert(log);
    }
}
