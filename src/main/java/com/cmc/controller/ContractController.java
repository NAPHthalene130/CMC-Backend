package com.cmc.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cmc.common.Constants;
import com.cmc.common.R;
import com.cmc.common.PageResult;
import com.cmc.dto.ContractDTO;
import com.cmc.entity.Contract;
import com.cmc.entity.User;
import com.cmc.service.ContractProcessService;
import com.cmc.service.ContractService;
import com.cmc.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "合同管理")
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final UserService userService;
    private final ContractProcessService processService;

    private boolean isAdmin() {
        long userId = StpUtil.getLoginIdAsLong();
        User sessionUser = (User) StpUtil.getSession().get("user");
        if (sessionUser != null && sessionUser.getRoleId() != null) {
            return Constants.ROLE_ADMIN_ID.equals(sessionUser.getRoleId());
        }
        User dbUser = userService.getById(userId);
        return dbUser != null && dbUser.getRoleId() != null
                && Constants.ROLE_ADMIN_ID.equals(dbUser.getRoleId());
    }

    @Operation(summary = "起草合同")
    @PostMapping("/draft")
    public R<Contract> draft(@Valid @RequestBody ContractDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        return R.ok(contractService.draft(userId, dto));
    }

    @Operation(summary = "定稿合同")
    @PutMapping("/{id}/finalize")
    public R<Contract> finalize(@PathVariable Long id, @Valid @RequestBody ContractDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        return R.ok(contractService.finalize(id, dto, userId));
    }

    @Operation(summary = "重新起草（审批被拒后）")
    @PostMapping("/{id}/redraft")
    public R<Void> redraft(@PathVariable Long id) {
        long userId = StpUtil.getLoginIdAsLong();
        processService.redraft(userId, id);
        return R.ok("已重新提交，请等待管理员重新分配审批");
    }

    @Operation(summary = "分页查询合同")
    @GetMapping
    public R<PageResult<Contract>> page(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long pageSize,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Integer state) {
        long userId = StpUtil.getLoginIdAsLong();
        Long filterUserId = isAdmin() ? null : userId;

        Page<Contract> result;
        if (state != null) {
            result = contractService.pageByState(page, pageSize, state, keyword, filterUserId);
        } else {
            result = contractService.pageContracts(page, pageSize, keyword, filterUserId);
        }
        return R.ok(PageResult.of(result));
    }

    @Operation(summary = "获取合同详情")
    @GetMapping("/{id}")
    public R<Contract> getById(@PathVariable Long id) {
        return R.ok(contractService.getById(id));
    }

    @SaCheckRole("ADMIN")
    @Operation(summary = "删除合同")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        contractService.removeById(id);
        return R.ok();
    }
}