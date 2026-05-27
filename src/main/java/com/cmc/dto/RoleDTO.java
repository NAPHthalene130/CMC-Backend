package com.cmc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class RoleDTO {
    @NotBlank(message = "角色名称不能为空")
    private String name;

    private String description;
    private List<String> functionIds;
}
