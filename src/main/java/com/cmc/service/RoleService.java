package com.cmc.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cmc.dto.RoleDTO;
import com.cmc.entity.Role;

public interface RoleService extends IService<Role> {
    Role addRole(RoleDTO dto);
    Role updateRole(Long id, RoleDTO dto);
    Page<Role> pageRoles(long page, long pageSize, String keyword);
}
