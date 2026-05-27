package com.cmc.config;

import cn.dev33.satoken.secure.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmc.entity.Contract;
import com.cmc.entity.ContractProcess;
import com.cmc.entity.ContractState;
import com.cmc.entity.Customer;
import com.cmc.entity.Function;
import com.cmc.entity.Role;
import com.cmc.entity.User;
import com.cmc.mapper.ContractMapper;
import com.cmc.mapper.ContractProcessMapper;
import com.cmc.mapper.ContractStateMapper;
import com.cmc.mapper.CustomerMapper;
import com.cmc.mapper.FunctionMapper;
import com.cmc.mapper.RoleMapper;
import com.cmc.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final CustomerMapper customerMapper;
    private final ContractMapper contractMapper;
    private final ContractStateMapper contractStateMapper;
    private final ContractProcessMapper contractProcessMapper;

    @Override
    public void run(String... args) {
        initFunctions();
        Role newUserRole = getOrCreateRole("NEW_USER", "新用户，等待管理员授权", "");
        Role operatorRole = getOrCreateRole("OPERATOR", "合同操作员", operatorPermissions());
        Role adminRole = getOrCreateRole("ADMIN", "合同管理员", allPermissions());
        initAdmin(adminRole.getId());
        initDemoData(adminRole.getId(), operatorRole.getId(), newUserRole.getId());
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
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin"));
        if (existing != null) {
            existing.setPassword(BCrypt.hashpw("123456"));
            existing.setRoleId(roleId);
            userMapper.updateById(existing);
            return;
        }
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(BCrypt.hashpw("123456"));
        admin.setRoleId(roleId);
        userMapper.insert(admin);
    }

    private void initDemoData(Long adminRoleId, Long operatorRoleId, Long newUserRoleId) {
        User drafter = getOrCreateUser("operator", operatorRoleId);
        User countersigner = getOrCreateUser("counter_user", operatorRoleId);
        User approver = getOrCreateUser("approve_user", operatorRoleId);
        User signer = getOrCreateUser("sign_user", operatorRoleId);
        getOrCreateUser("manager", adminRoleId);
        getOrCreateUser("new_user", newUserRoleId);

        Customer ruanko = getOrCreateCustomer("KH-DEMO-001", "软酷科技有限公司", "深圳市南山区科技园", "0755-10000001");
        Customer naph = getOrCreateCustomer("KH-DEMO-002", "NAPH 智能制造有限公司", "广州市天河区软件路", "020-10000002");

        Contract draft = getOrCreateContract("HT-DEMO-001", "演示-待分配采购合同", ruanko.getId(), drafter.getId(), "用于演示管理员分配流程的起草合同。", 1);
        Contract countersigned = getOrCreateContract("HT-DEMO-002", "演示-待定稿服务合同", naph.getId(), drafter.getId(), "用于演示会签完成后定稿的服务合同。", 2);
        Contract finalized = getOrCreateContract("HT-DEMO-003", "演示-待审批框架合同", ruanko.getId(), drafter.getId(), "用于演示定稿完成后审批的框架合同。", 3);
        Contract approved = getOrCreateContract("HT-DEMO-004", "演示-待签订运维合同", naph.getId(), drafter.getId(), "用于演示审批完成后签订的运维合同。", 4);
        getOrCreateContract("HT-DEMO-005", "演示-已签订年度合同", ruanko.getId(), drafter.getId(), "用于演示完整闭环的年度合同。", 5);

        getOrCreateProcess(countersigned.getId(), 1, 1, countersigner.getId(), "会签意见：条款清晰，同意进入定稿。", LocalDateTime.now().minusDays(2));
        getOrCreateProcess(finalized.getId(), 2, 0, approver.getId(), null, LocalDateTime.now().minusDays(1));
        getOrCreateProcess(approved.getId(), 3, 0, signer.getId(), null, LocalDateTime.now().minusHours(12));
        getOrCreateProcess(draft.getId(), 1, 0, countersigner.getId(), null, LocalDateTime.now().minusHours(6));
    }

    private User getOrCreateUser(String username, Long roleId) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user != null) {
            user.setPassword(BCrypt.hashpw("123456"));
            if (roleId != null && !roleId.equals(user.getRoleId())) {
                user.setRoleId(roleId);
            }
            userMapper.updateById(user);
            return user;
        }
        User created = new User();
        created.setUsername(username);
        created.setPassword(BCrypt.hashpw("123456"));
        created.setRoleId(roleId);
        userMapper.insert(created);
        return created;
    }

    private Customer getOrCreateCustomer(String num, String name, String address, String tel) {
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>().eq(Customer::getNum, num));
        if (customer != null) {
            return customer;
        }
        Customer created = new Customer();
        created.setNum(num);
        created.setName(name);
        created.setAddress(address);
        created.setTel(tel);
        created.setFax("0755-88888888");
        created.setCode("518000");
        created.setBank("招商银行深圳分行");
        created.setAccount("6222000000000000000");
        customerMapper.insert(created);
        return created;
    }

    private Contract getOrCreateContract(String num, String name, Long customerId, Long userId, String content, Integer stateType) {
        Contract contract = contractMapper.selectOne(new LambdaQueryWrapper<Contract>().eq(Contract::getNum, num));
        if (contract == null) {
            contract = new Contract();
            contract.setNum(num);
            contract.setName(name);
            contract.setCustomerId(customerId);
            contract.setUserId(userId);
            contract.setBeginTime(LocalDate.now().minusDays(10));
            contract.setEndTime(LocalDate.now().plusMonths(6));
            contract.setContent(content);
            contractMapper.insert(contract);
        }
        getOrCreateState(contract.getId(), stateType);
        return contract;
    }

    private void getOrCreateState(Long contractId, Integer type) {
        Long count = contractStateMapper.selectCount(new LambdaQueryWrapper<ContractState>()
                .eq(ContractState::getContractId, contractId)
                .eq(ContractState::getType, type));
        if (count > 0) {
            return;
        }
        ContractState state = new ContractState();
        state.setContractId(contractId);
        state.setType(type);
        state.setTime(LocalDateTime.now().minusDays(6 - type));
        contractStateMapper.insert(state);
    }

    private void getOrCreateProcess(Long contractId, Integer type, Integer state, Long userId, String content, LocalDateTime time) {
        Long count = contractProcessMapper.selectCount(new LambdaQueryWrapper<ContractProcess>()
                .eq(ContractProcess::getContractId, contractId)
                .eq(ContractProcess::getType, type)
                .eq(ContractProcess::getUserId, userId));
        if (count > 0) {
            return;
        }
        ContractProcess process = new ContractProcess();
        process.setContractId(contractId);
        process.setType(type);
        process.setState(state);
        process.setUserId(userId);
        process.setContent(content);
        process.setTime(time);
        contractProcessMapper.insert(process);
    }
}
