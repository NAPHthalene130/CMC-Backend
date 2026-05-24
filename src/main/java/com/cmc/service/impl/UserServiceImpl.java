package com.cmc.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmc.common.exception.BusinessException;
import com.cmc.dto.RegisterDTO;
import com.cmc.dto.UserDTO;
import com.cmc.entity.User;
import com.cmc.mapper.UserMapper;
import com.cmc.service.LogService;
import com.cmc.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final LogService logService;

    @Override
    public void register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (lambdaQuery().eq(User::getUsername, dto.getUsername()).count() > 0) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        save(user);
    }

    @Override
    public User login(String username, String password) {
        User user = lambdaQuery().eq(User::getUsername, username).one();
        if (user == null || !BCrypt.checkpw(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        StpUtil.login(user.getId());
        StpUtil.getSession().set("user", user);
        return user;
    }

    @Override
    public User addUser(UserDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (lambdaQuery().eq(User::getUsername, dto.getUsername()).count() > 0) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setRoleId(dto.getRoleId());
        save(user);

        Long operatorId = StpUtil.getLoginIdAsLong();
        User operator = getById(operatorId);
        logService.saveLog(operatorId, operator.getUsername(), "新增用户：" + user.getUsername());
        return user;
    }

    @Override
    public User updateUser(Long id, UserDTO dto) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(BCrypt.hashpw(dto.getPassword()));
        }
        if (dto.getRoleId() != null) {
            user.setRoleId(dto.getRoleId());
        }
        updateById(user);

        Long operatorId = StpUtil.getLoginIdAsLong();
        User operator = getById(operatorId);
        logService.saveLog(operatorId, operator.getUsername(), "修改用户：" + user.getUsername());
        return user;
    }

    @Override
    public Page<User> pageUsers(long page, long pageSize, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(StringUtils.hasText(keyword), User::getUsername, keyword)
                .orderByDesc(User::getCreateTime);
        return page(new Page<>(page, pageSize), wrapper);
    }
}
