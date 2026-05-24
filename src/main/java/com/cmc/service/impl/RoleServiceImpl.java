package com.cmc.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.common.exception.BusinessException;
import com.cmc.dto.RoleDTO;
import com.cmc.entity.Role;
import com.cmc.entity.User;
import com.cmc.mapper.RoleMapper;
import com.cmc.service.LogService;
import com.cmc.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final LogService logService;

    @Override
    public Role addRole(RoleDTO dto) {
        if (lambdaQuery().eq(Role::getName, dto.getName()).count() > 0) {
            throw new BusinessException("角色名称已存在");
        }
        Role role = new Role();
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        if (dto.getFunctionIds() != null && !dto.getFunctionIds().isEmpty()) {
            role.setFunctions(dto.getFunctionIds().stream()
                    .map(String::valueOf).collect(Collectors.joining(",")));
        }
        save(role);

        User operator = (User) StpUtil.getSession().get("user");
        logService.saveLog(operator.getId(), operator.getUsername(), "新增角色：" + role.getName());
        return role;
    }

    @Override
    public Role updateRole(Long id, RoleDTO dto) {
        Role role = getById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        if (dto.getFunctionIds() != null) {
            role.setFunctions(dto.getFunctionIds().stream()
                    .map(String::valueOf).collect(Collectors.joining(",")));
        }
        updateById(role);

        User operator = (User) StpUtil.getSession().get("user");
        logService.saveLog(operator.getId(), operator.getUsername(), "修改角色：" + role.getName());
        return role;
    }

    @Override
    public Page<Role> pageRoles(long page, long pageSize, String keyword) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<Role>()
                .like(StringUtils.hasText(keyword), Role::getName, keyword)
                .orderByDesc(Role::getCreateTime);
        return page(new Page<>(page, pageSize), wrapper);
    }
}
