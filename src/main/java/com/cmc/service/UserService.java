package com.cmc.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cmc.dto.RegisterDTO;
import com.cmc.dto.UserDTO;
import com.cmc.entity.User;
import com.cmc.vo.AuthUserVO;

/**
 * 用户业务接口。
 *
 * @author NAPH130
 */
public interface UserService extends IService<User> {
    void register(RegisterDTO dto);
    User login(String username, String password);
    AuthUserVO buildAuthUser(User user);
    User addUser(UserDTO dto);
    User updateUser(Long id, UserDTO dto);
    Page<User> pageUsers(long page, long pageSize, String keyword);
}
