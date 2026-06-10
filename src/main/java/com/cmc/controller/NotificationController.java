package com.cmc.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cmc.common.R;
import com.cmc.common.PageResult;
import com.cmc.entity.Notification;
import com.cmc.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author NAPH130
 */
@Tag(name = "通知消息管理")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "分页查询通知")
    @GetMapping
    public R<PageResult<Notification>> page(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long pageSize) {
        long userId = StpUtil.getLoginIdAsLong();
        Page<Notification> result = notificationService.pageNotifications(page, pageSize, userId);
        return R.ok(PageResult.of(result));
    }

    @Operation(summary = "获取未读通知数量")
    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        long userId = StpUtil.getLoginIdAsLong();
        return R.ok(notificationService.getUnreadCount(userId));
    }

    @Operation(summary = "标记通知为已读")
    @PutMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        long userId = StpUtil.getLoginIdAsLong();
        notificationService.markRead(id, userId);
        return R.ok();
    }

    @Operation(summary = "全部标记为已读")
    @PutMapping("/read-all")
    public R<Void> markAllRead() {
        long userId = StpUtil.getLoginIdAsLong();
        notificationService.markAllRead(userId);
        return R.ok();
    }
}
