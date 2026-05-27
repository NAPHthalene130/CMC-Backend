package com.cmc.config;

import cn.dev33.satoken.stp.StpInterface;
import com.cmc.entity.Role;
import com.cmc.entity.User;
import com.cmc.mapper.RoleMapper;
import com.cmc.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限数据源实现。
 *
 * @author NAPH130
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        User user = userMapper.selectById(Long.valueOf(String.valueOf(loginId)));
        if (user == null || user.getRoleId() == null) {
            return Collections.emptyList();
        }
        Role role = roleMapper.selectById(user.getRoleId());
        if (role == null || !StringUtils.hasText(role.getFunctions())) {
            return Collections.emptyList();
        }
        return Arrays.stream(role.getFunctions().split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        User user = userMapper.selectById(Long.valueOf(String.valueOf(loginId)));
        if (user == null || user.getRoleId() == null) {
            return Collections.emptyList();
        }
        Role role = roleMapper.selectById(user.getRoleId());
        return role == null ? Collections.emptyList() : List.of(role.getName());
    }
}
