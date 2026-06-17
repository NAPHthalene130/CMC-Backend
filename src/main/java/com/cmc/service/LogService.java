package com.cmc.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cmc.entity.Log;

public interface LogService extends IService<Log> {
    Page<Log> pageLogs(long page, long pageSize, String keyword);
    void saveLog(Long userId, String username, String content);
    void saveLoginLog(Long userId, String username, String ip, String userAgent, int status, String msg);
}
