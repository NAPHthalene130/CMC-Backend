package com.cmc.config;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmc.entity.Function;
import com.cmc.entity.Role;
import com.cmc.entity.User;
import com.cmc.mapper.FunctionMapper;
import com.cmc.mapper.RoleMapper;
import com.cmc.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sa-Token 权限认证接口实现
 *
 * @author NAPH130
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final FunctionMapper functionMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        User user = userMapper.selectById(userId);
        if (user == null || user.getRoleId() == null) {
            return Collections.emptyList();
        }

        Role role = roleMapper.selectById(user.getRoleId());
        if (role == null || role.getFunctions() == null || role.getFunctions().isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> functionIds = Arrays.stream(role.getFunctions().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());

        if (functionIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Function> functions = functionMapper.selectBatchIds(functionIds);
        return functions.stream()
                .map(Function::getName)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        User user = userMapper.selectById(userId);
        if (user == null || user.getRoleId() == null) {
            return Collections.emptyList();
        }

        Role role = roleMapper.selectById(user.getRoleId());
        if (role == null) {
            return Collections.emptyList();
        }

        List<String> roles = new ArrayList<>();
        roles.add(role.getName());
        return roles;
    }
}
