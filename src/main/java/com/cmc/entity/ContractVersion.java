package com.cmc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 合同版本历史实体
 *
 * @author NAPH130
 */
@Data
@TableName("contract_version")
public class ContractVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private Integer versionNum;
    private String name;
    private String content;
    private String changeDesc;
    private Long createUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
