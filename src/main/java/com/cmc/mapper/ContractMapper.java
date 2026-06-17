package com.cmc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmc.entity.Contract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * @author NAPH130
 */
@Mapper
public interface ContractMapper extends BaseMapper<Contract> {

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') as month, COUNT(*) as count " +
            "FROM contract WHERE deleted = 0 " +
            "AND create_time >= DATE_SUB(NOW(), INTERVAL 6 MONTH) " +
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m') ORDER BY month")
    List<Map<String, Object>> monthlyTrend();
}
