package com.cmc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("contract_state")
public class ContractState {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private Integer type;
    private LocalDateTime time;
}
