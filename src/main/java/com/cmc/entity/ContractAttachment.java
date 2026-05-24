package com.cmc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("contract_attachment")
public class ContractAttachment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private String fileName;
    private String path;
    private String type;
    private LocalDateTime uploadTime;
}
