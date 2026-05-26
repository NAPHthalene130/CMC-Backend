package com.cmc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("log")
public class Log {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String content;
    private LocalDateTime time;
    private String ip;
    private String type;
}
