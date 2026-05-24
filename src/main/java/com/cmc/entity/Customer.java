package com.cmc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("customer")
public class Customer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String num;
    private String name;
    private String address;
    private String tel;
    private String fax;
    private String code;
    private String bank;
    private String account;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
