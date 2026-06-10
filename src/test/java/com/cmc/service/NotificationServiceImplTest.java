package com.cmc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.entity.Notification;
import com.cmc.mapper.NotificationMapper;
import com.cmc.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author NAPH130
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() throws Exception {
        notificationService = new NotificationServiceImpl();
        var field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(notificationService, notificationMapper);
    }

    @Test
    void sendNotification_shouldSetDefaultValues() {
        when(notificationMapper.insert(any(Notification.class))).thenReturn(1);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        notificationService.sendNotification(1L, "测试标题", "测试内容", "CONTRACT", 100L);

        verify(notificationMapper).insert(captor.capture());
        Notification n = captor.getValue();
        assertEquals(1L, n.getUserId());
        assertEquals("测试标题", n.getTitle());
        assertEquals("CONTRACT", n.getType());
        assertEquals(0, n.getIsRead());
    }

    @Test
    void getUnreadCount_shouldReturnCount() {
        when(notificationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);
        assertEquals(5L, notificationService.getUnreadCount(1L));
    }

    @Test
    void markRead_shouldUpdateNotification() {
        when(notificationMapper.update(any(), any())).thenReturn(1);
        assertDoesNotThrow(() -> notificationService.markRead(1L, 1L));
    }

    @Test
    void markAllRead_shouldUpdateAllForUser() {
        when(notificationMapper.update(any(), any())).thenReturn(3);
        assertDoesNotThrow(() -> notificationService.markAllRead(1L));
    }
}
