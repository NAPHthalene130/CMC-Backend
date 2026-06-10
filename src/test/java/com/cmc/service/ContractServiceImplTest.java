package com.cmc.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.common.exception.BusinessException;
import com.cmc.dto.ContractDTO;
import com.cmc.entity.Contract;
import com.cmc.entity.User;
import com.cmc.mapper.ContractMapper;
import com.cmc.mapper.ContractStateMapper;
import com.cmc.mapper.UserMapper;
import com.cmc.service.impl.ContractServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author NAPH130
 */
@ExtendWith(MockitoExtension.class)
class ContractServiceImplTest {

    @Mock
    private ContractMapper contractMapper;
    @Mock
    private ContractStateMapper contractStateMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private LogService logService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ContractVersionService versionService;

    private ContractServiceImpl contractService;
    private MockedStatic<StpUtil> stpUtilMock;

    @BeforeEach
    void setUp() throws Exception {
        contractService = new ContractServiceImpl(contractStateMapper, userMapper,
                logService, notificationService, versionService);
        var field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(contractService, contractMapper);

        stpUtilMock = mockStatic(StpUtil.class);
        var session = mock(SaSession.class);
        var user = new User();
        user.setId(1L);
        user.setUsername("admin");
        lenient().when(session.get("user")).thenReturn(user);
        stpUtilMock.when(StpUtil::getSession).thenReturn(session);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Test
    void draft_shouldCreateContractWithState() {
        ContractDTO dto = new ContractDTO();
        dto.setName("测试合同");
        dto.setCustomerId(1L);
        dto.setBeginTime(LocalDate.now());
        dto.setEndTime(LocalDate.now().plusDays(30));
        dto.setContent("合同内容");

        when(contractMapper.insert(any(Contract.class))).thenAnswer(inv -> {
            Contract c = inv.getArgument(0);
            c.setId(1L);
            return 1;
        });
        when(contractStateMapper.insert(any())).thenReturn(1);
        lenient().when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        Contract result = contractService.draft(1L, dto);
        assertNotNull(result);
        assertEquals("测试合同", result.getName());
        assertTrue(result.getNum().startsWith("HT-"));
        assertEquals(1L, result.getId());
        verify(contractStateMapper).insert(any());
    }

    @Test
    void finalize_shouldFailWhenContractNotFound() {
        when(contractMapper.selectById(100L)).thenReturn(null);

        ContractDTO dto = new ContractDTO();
        dto.setName("测试");

        assertThrows(BusinessException.class, () -> contractService.finalize(100L, dto));
    }
}
