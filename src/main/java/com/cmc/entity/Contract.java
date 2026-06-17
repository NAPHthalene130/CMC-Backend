package com.cmc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("contract")
public class Contract {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String num;
    private String name;
    private Long customerId;
    private LocalDate beginTime;
    private LocalDate endTime;
    private String content;
    private Long userId;

    /** 当前状态（来自 contract_state 表，非数据库字段） */
    @TableField(exist = false)
    private Integer state;

    /** 起草人（来自 user 表，非数据库字段） */
    @TableField(exist = false)
    private String draftUser;

    /** 客户名称（来自 customer 表，非数据库字段） */
    @TableField(exist = false)
    private String customerName;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
