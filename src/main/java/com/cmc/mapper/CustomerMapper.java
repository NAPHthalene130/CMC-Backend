package com.cmc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmc.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
