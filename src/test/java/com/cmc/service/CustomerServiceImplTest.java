package com.cmc.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.common.exception.BusinessException;
import com.cmc.dto.CustomerDTO;
import com.cmc.entity.Customer;
import com.cmc.entity.User;
import com.cmc.mapper.CustomerMapper;
import com.cmc.service.impl.CustomerServiceImpl;
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
class CustomerServiceImplTest {

    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private LogService logService;

    private CustomerServiceImpl customerService;
    private MockedStatic<StpUtil> stpUtilMock;

    @BeforeEach
    void setUp() throws Exception {
        customerService = new CustomerServiceImpl(logService);
        var field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(customerService, customerMapper);

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
    void addCustomer_shouldGenerateNum() {
        CustomerDTO dto = new CustomerDTO();
        dto.setName("测试客户");
        dto.setTel("12345678901");
        dto.setAddress("测试地址");

        lenient().when(customerMapper.insert(any(Customer.class))).thenReturn(1);

        Customer result = customerService.addCustomer(dto);
        assertNotNull(result);
        assertTrue(result.getNum().startsWith("KH-"));
        verify(logService).saveLog(anyLong(), anyString(), anyString());
    }

    @Test
    void updateCustomer_shouldFailWhenNotFound() {
        when(customerMapper.selectById(100L)).thenReturn(null);

        CustomerDTO dto = new CustomerDTO();
        dto.setName("测试");

        assertThrows(BusinessException.class, () -> customerService.updateCustomer(100L, dto));
    }
}
