package com.cmc.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.common.exception.BusinessException;
import com.cmc.dto.RegisterDTO;
import com.cmc.dto.UserDTO;
import com.cmc.entity.User;
import com.cmc.mapper.RoleMapper;
import com.cmc.mapper.UserMapper;
import com.cmc.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author NAPH130
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private LogService logService;

    private UserServiceImpl userService;
    private MockedStatic<StpUtil> stpUtilMock;

    @BeforeEach
    void setUp() throws Exception {
        userService = new UserServiceImpl(logService, roleMapper);
        var field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(userService, userMapper);

        stpUtilMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Test
    void register_shouldSucceedWhenValid() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("testuser");
        dto.setPassword("pass123");
        dto.setConfirmPassword("pass123");

        lenient().when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        assertDoesNotThrow(() -> userService.register(dto));
    }

    @Test
    void register_shouldFailWhenPasswordMismatch() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("testuser");
        dto.setPassword("pass123");
        dto.setConfirmPassword("different");

        assertThrows(BusinessException.class, () -> userService.register(dto));
    }

    @Test
    void register_shouldFailWhenUsernameExists() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("existing");
        dto.setPassword("pass123");
        dto.setConfirmPassword("pass123");

        lenient().when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(BusinessException.class, () -> userService.register(dto));
    }

    @Test
    void addUser_shouldSetRoleId() {
        UserDTO dto = new UserDTO();
        dto.setUsername("newuser");
        dto.setPassword("pass123");
        dto.setConfirmPassword("pass123");
        dto.setRoleId(2L);

        lenient().when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        lenient().when(userMapper.insert(any(User.class))).thenReturn(1);
        var operator = new User();
        operator.setId(1L);
        operator.setUsername("admin");
        lenient().when(userMapper.selectById(anyLong())).thenReturn(operator);
        lenient().when(StpUtil.getLoginIdAsLong()).thenReturn(1L);

        User result = userService.addUser(dto);
        assertNotNull(result);
        verify(logService).saveLog(anyLong(), eq("admin"), anyString());
    }
}
