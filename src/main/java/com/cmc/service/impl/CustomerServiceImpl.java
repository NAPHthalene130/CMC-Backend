package com.cmc.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.common.exception.BusinessException;
import com.cmc.dto.CustomerDTO;
import com.cmc.entity.Customer;
import com.cmc.entity.User;
import com.cmc.mapper.CustomerMapper;
import com.cmc.service.CustomerService;
import com.cmc.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    private final LogService logService;

    @Override
    @Transactional
    public Customer addCustomer(CustomerDTO dto) {
        Customer customer = new Customer();
        customer.setNum("KH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        customer.setName(dto.getName());
        customer.setTel(dto.getTel());
        customer.setAddress(dto.getAddress());
        customer.setFax(dto.getFax());
        customer.setCode(dto.getCode());
        customer.setBank(dto.getBank());
        customer.setAccount(dto.getAccount());
        save(customer);

        User operator = (User) StpUtil.getSession().get("user");
        if (operator != null) {
            logService.saveLog(operator.getId(), operator.getUsername(), "新增客户：" + customer.getName());
        }
        return customer;
    }

    @Override
    @Transactional
    public Customer updateCustomer(Long id, CustomerDTO dto) {
        Customer customer = getById(id);
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }
        customer.setName(dto.getName());
        customer.setTel(dto.getTel());
        customer.setAddress(dto.getAddress());
        customer.setFax(dto.getFax());
        customer.setCode(dto.getCode());
        customer.setBank(dto.getBank());
        customer.setAccount(dto.getAccount());
        updateById(customer);

        User operator = (User) StpUtil.getSession().get("user");
        if (operator != null) {
            logService.saveLog(operator.getId(), operator.getUsername(), "修改客户：" + customer.getName());
        }
        return customer;
    }

    @Override
    public Page<Customer> pageCustomers(long page, long pageSize, String keyword) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .like(StringUtils.hasText(keyword), Customer::getName, keyword)
                .orderByDesc(Customer::getCreateTime);
        return page(new Page<>(page, pageSize), wrapper);
    }
}
