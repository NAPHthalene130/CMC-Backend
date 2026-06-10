package com.cmc.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.common.exception.BusinessException;
import com.cmc.dto.AssignDTO;
import com.cmc.dto.ProcessDTO;
import com.cmc.entity.Contract;
import com.cmc.entity.ContractProcess;
import com.cmc.entity.User;
import com.cmc.mapper.ContractMapper;
import com.cmc.mapper.ContractProcessMapper;
import com.cmc.mapper.ContractStateMapper;
import com.cmc.service.impl.ContractProcessServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

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
class ContractProcessServiceImplTest {

    @Mock
    private ContractProcessMapper contractProcessMapper;
    @Mock
    private ContractStateMapper contractStateMapper;
    @Mock
    private ContractMapper contractMapper;
    @Mock
    private LogService logService;
    @Mock
    private NotificationService notificationService;

    private ContractProcessServiceImpl service;
    private MockedStatic<StpUtil> stpUtilMock;

    @BeforeEach
    void setUp() throws Exception {
        service = new ContractProcessServiceImpl(contractStateMapper, contractMapper,
                logService, notificationService);
        var field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(service, contractProcessMapper);

        stpUtilMock = mockStatic(StpUtil.class);
        var session = mock(SaSession.class);
        var user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        lenient().when(session.get("user")).thenReturn(user);
        lenient().when(session.get("username")).thenReturn("testuser");
        stpUtilMock.when(StpUtil::getSession).thenReturn(session);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Test
    void getPendingTasks_shouldQueryByUserId() {
        when(contractProcessMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(new ContractProcess()));

        List<ContractProcess> result = service.getPendingTasks(1L, 1);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getPendingTasks_shouldReturnEmptyWhenNone() {
        when(contractProcessMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        List<ContractProcess> result = service.getPendingTasks(1L, null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void countersign_shouldFailWhenTaskNotFound() {
        when(contractProcessMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        ProcessDTO dto = new ProcessDTO();
        dto.setContractId(100L);
        dto.setContent("test");

        assertThrows(BusinessException.class, () -> service.countersign(1L, dto));
    }

    @Test
    void approve_shouldFailWhenTaskNotFound() {
        when(contractProcessMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        ProcessDTO dto = new ProcessDTO();
        dto.setContractId(100L);
        dto.setContent("test");
        dto.setApproved(true);

        assertThrows(BusinessException.class, () -> service.approve(1L, dto));
    }

    @Test
    void sign_shouldFailWhenTaskNotFound() {
        when(contractProcessMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        ProcessDTO dto = new ProcessDTO();
        dto.setContractId(100L);
        dto.setContent("test");

        assertThrows(BusinessException.class, () -> service.sign(1L, dto));
    }

    @Test
    void approve_shouldProcessApproval() {
        var process = new ContractProcess();
        process.setId(10L);
        process.setContractId(100L);
        process.setType(2);
        process.setState(0);
        process.setUserId(1L);

        when(contractProcessMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(process);
        when(contractProcessMapper.updateById(any(ContractProcess.class))).thenReturn(1);
        when(contractMapper.selectById(100L)).thenReturn(null);
        lenient().when(contractProcessMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        ProcessDTO dto = new ProcessDTO();
        dto.setContractId(100L);
        dto.setContent("approved");
        dto.setApproved(true);

        assertDoesNotThrow(() -> service.approve(1L, dto));
        verify(logService).saveLog(anyLong(), anyString(), anyString());
    }

    @Test
    void sign_shouldProcessSigning() {
        var process = new ContractProcess();
        process.setId(10L);
        process.setContractId(100L);
        process.setType(3);
        process.setState(0);
        process.setUserId(1L);

        when(contractProcessMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(process);
        when(contractProcessMapper.updateById(any(ContractProcess.class))).thenReturn(1);
        when(contractMapper.selectById(100L)).thenReturn(null);
        lenient().when(contractProcessMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        ProcessDTO dto = new ProcessDTO();
        dto.setContractId(100L);
        dto.setContent("signed");

        assertDoesNotThrow(() -> service.sign(1L, dto));
        verify(logService).saveLog(anyLong(), anyString(), anyString());
    }

    @Test
    void assignContract_shouldCreateProcesses() {
        lenient().when(contractProcessMapper.insert(any(ContractProcess.class))).thenReturn(1);
        when(contractMapper.selectById(100L)).thenReturn(new Contract());

        AssignDTO dto = new AssignDTO();
        dto.setContractId(100L);
        dto.setCountersignUserIds(List.of(1L, 2L));
        dto.setApproveUserIds(List.of(3L));

        assertDoesNotThrow(() -> service.assignContract(dto));
        verify(notificationService, atLeastOnce()).sendNotification(anyLong(), anyString(), anyString(), anyString(), anyLong());
    }
}
