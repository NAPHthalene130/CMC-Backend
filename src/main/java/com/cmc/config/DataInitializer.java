package com.cmc.config;

import cn.dev33.satoken.secure.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmc.entity.Function;
import com.cmc.entity.Role;
import com.cmc.entity.User;
import com.cmc.mapper.FunctionMapper;
import com.cmc.mapper.RoleMapper;
import com.cmc.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 初始化系统基础角色、功能点和默认管理员。
 *
 * @author NAPH130
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleMapper roleMapper;
    private final FunctionMapper functionMapper;
    private final UserMapper userMapper;

    @Override
    public void run(String... args) {
        initFunctions();
        Role newUserRole = getOrCreateRole("NEW_USER", "新用户，等待管理员授权", "");
        Role operatorRole = getOrCreateRole("OPERATOR", "合同操作员", operatorPermissions());
        Role adminRole = getOrCreateRole("ADMIN", "合同管理员", allPermissions());
        initAdmin(adminRole.getId());
    }

    private void initFunctions() {
        List<Function> functions = Arrays.asList(
                function("C_DRAFT", "起草合同", "/api/contracts/draft", "合同起草"),
                function("C_FINAL", "定稿合同", "/api/contracts/*/finalize", "合同定稿"),
                function("C_QUERY", "查询合同", "/api/contracts", "合同查询"),
                function("C_DELETE", "删除合同", "/api/contracts/*", "合同删除"),
                function("P_COUNTER", "会签合同", "/api/process/countersign", "合同会签"),
                function("P_APPROVE", "审批合同", "/api/process/approve", "合同审批"),
                function("P_SIGN", "签订合同", "/api/process/sign", "合同签订"),
                function("P_ASSIGN", "分配合同", "/api/process/assign", "合同分配"),
                function("P_QUERY", "流程查询", "/api/process/pending", "流程查询"),
                function("U_MANAGE", "用户管理", "/api/users", "用户管理"),
                function("R_MANAGE", "角色管理", "/api/roles", "角色管理"),
                function("F_MANAGE", "功能管理", "/api/functions", "功能管理"),
                function("CU_MANAGE", "客户管理", "/api/customers", "客户管理"),
                function("L_MANAGE", "日志管理", "/api/logs", "日志管理")
        );
        for (Function function : functions) {
            Long count = functionMapper.selectCount(new LambdaQueryWrapper<Function>().eq(Function::getNum, function.getNum()));
            if (count == 0) {
                functionMapper.insert(function);
            }
        }
    }

    private Function function(String num, String name, String url, String description) {
        Function function = new Function();
        function.setNum(num);
        function.setName(name);
        function.setUrl(url);
        function.setDescription(description);
        return function;
    }

    private Role getOrCreateRole(String name, String description, String functions) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getName, name));
        if (role != null) {
            if (!StringUtils.hasText(role.getFunctions()) && StringUtils.hasText(functions)) {
                role.setFunctions(functions);
                roleMapper.updateById(role);
            }
            return role;
        }
        Role created = new Role();
        created.setName(name);
        created.setDescription(description);
        created.setFunctions(functions);
        roleMapper.insert(created);
        return created;
    }

    private String operatorPermissions() {
        return String.join(",", "C_DRAFT", "C_FINAL", "C_QUERY",
                "P_COUNTER", "P_APPROVE", "P_SIGN", "P_QUERY");
    }

    private String allPermissions() {
        return functionMapper.selectList(null).stream()
                .map(Function::getNum)
                .collect(Collectors.joining(","));
    }

    private void initAdmin(Long roleId) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin"));
        if (count > 0) {
            return;
        }
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(BCrypt.hashpw("123456"));
        admin.setRoleId(roleId);
        userMapper.insert(admin);
    }
}
