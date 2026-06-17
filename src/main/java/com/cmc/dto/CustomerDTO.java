package com.cmc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerDTO {
    @NotBlank(message = "客户名称不能为空")
    private String name;

    @NotBlank(message = "电话不能为空")
    private String tel;

    @NotBlank(message = "地址不能为空")
    private String address;

    private String fax;
    private String code;
    private String bank;
    private String account;
}
