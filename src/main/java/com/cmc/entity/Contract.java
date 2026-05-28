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
    private Integer state;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String draftUser;

    @TableField(exist = false)
    private String customerName;
}
