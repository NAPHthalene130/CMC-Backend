package com.cmc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{3,}$", message = "用户名须以字母开头，至少4位")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
