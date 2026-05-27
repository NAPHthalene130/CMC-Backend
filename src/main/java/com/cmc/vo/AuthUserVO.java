package com.cmc.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 登录用户返回信息，避免向前端暴露密码等敏感字段。
 *
 * @author NAPH130
 */
@Data
public class AuthUserVO {
    private Long id;
    private String username;
    private Long roleId;
    private String role;
    private List<String> permissions = new ArrayList<>();
    private LocalDateTime createTime;
}
