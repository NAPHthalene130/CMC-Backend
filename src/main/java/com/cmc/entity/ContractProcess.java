package com.cmc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("contract_process")
public class ContractProcess {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private Integer type;
    private Integer state;
    private Long userId;
    private String content;
    private LocalDateTime time;
}
