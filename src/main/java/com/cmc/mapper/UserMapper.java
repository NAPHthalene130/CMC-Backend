package com.cmc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmc.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
