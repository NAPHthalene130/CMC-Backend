package com.cmc.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cmc.dto.CustomerDTO;
import com.cmc.entity.Customer;

public interface CustomerService extends IService<Customer> {
    Customer addCustomer(CustomerDTO dto);
    Customer updateCustomer(Long id, CustomerDTO dto);
    Page<Customer> pageCustomers(long page, long pageSize, String keyword);
}
