package com.cmc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmc.entity.ContractState;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * @author NAPH130
 */
@Mapper
public interface ContractStateMapper extends BaseMapper<ContractState> {

    @Select("SELECT cs.type, COUNT(DISTINCT cs.contract_id) as count " +
            "FROM contract_state cs " +
            "INNER JOIN contract c ON c.id = cs.contract_id AND c.deleted = 0 " +
            "WHERE cs.id IN (SELECT MAX(cs2.id) FROM contract_state cs2 GROUP BY cs2.contract_id) " +
            "GROUP BY cs.type")
    List<Map<String, Object>> countByType();
}
