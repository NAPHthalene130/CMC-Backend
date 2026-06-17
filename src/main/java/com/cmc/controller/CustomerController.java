package com.cmc.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cmc.common.R;
import com.cmc.common.PageResult;
import com.cmc.dto.CustomerDTO;
import com.cmc.entity.Customer;
import com.cmc.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "客户管理")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "分页查询客户")
    @GetMapping
    public R<PageResult<Customer>> page(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long pageSize,
                                         @RequestParam(required = false) String keyword) {
        Page<Customer> result = customerService.pageCustomers(page, pageSize, keyword);
        return R.ok(PageResult.of(result));
    }

    @Operation(summary = "获取客户详情")
    @GetMapping("/{id}")
    public R<Customer> getById(@PathVariable Long id) {
        return R.ok(customerService.getById(id));
    }

    @SaCheckRole("ADMIN")
    @Operation(summary = "新增客户")
    @PostMapping
    public R<Customer> add(@Valid @RequestBody CustomerDTO dto) {
        return R.ok(customerService.addCustomer(dto));
    }

    @SaCheckRole("ADMIN")
    @Operation(summary = "修改客户")
    @PutMapping("/{id}")
    public R<Customer> update(@PathVariable Long id, @Valid @RequestBody CustomerDTO dto) {
        return R.ok(customerService.updateCustomer(id, dto));
    }

    @SaCheckRole("ADMIN")
    @Operation(summary = "删除客户")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        customerService.removeById(id);
        return R.ok();
    }
}
