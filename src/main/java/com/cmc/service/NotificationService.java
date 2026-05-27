package com.cmc.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cmc.entity.Notification;

/**
 * @author NAPH130
 */
public interface NotificationService extends IService<Notification> {
    void sendNotification(Long userId, String title, String content, String type, Long relatedId);
    long getUnreadCount(Long userId);
    void markRead(Long id, Long userId);
    void markAllRead(Long userId);
    Page<Notification> pageNotifications(long page, long pageSize, Long userId);
}
