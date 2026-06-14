package com.cmc.entity;

import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
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
    @ColumnWidth(22)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime time;
    private String ip;
    private String type;
}
