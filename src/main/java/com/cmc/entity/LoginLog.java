package com.cmc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("login_log")
public class LoginLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String ip;
    private String userAgent;
    private Integer status;
    private String msg;
    private LocalDateTime time;
}
