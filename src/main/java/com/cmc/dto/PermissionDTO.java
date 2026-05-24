package com.cmc.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PermissionDTO {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "角色ID不能为空")
    private Long roleId;
}
