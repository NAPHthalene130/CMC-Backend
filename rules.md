# CMC-Backend 开发规范

> 合同管理系统后端项目 · Spring Boot 3 + MyBatis-Plus + Sa-Token

## 1. 项目规范

- 技术栈：Java 17、Spring Boot 3.2、MyBatis-Plus、Sa-Token、MySQL 8、Redis、EasyExcel、Knife4j。
- 所有业务接口统一以 `/api` 为前缀。
- 所有接口统一返回 `com.cmc.common.R`，分页数据统一返回 `PageResult`。
- 业务异常统一抛出 `BusinessException`，由 `GlobalExceptionHandler` 转换为标准响应。
- 数据库字段使用下划线命名，Java 属性使用 camelCase，依赖 MyBatis-Plus 自动映射。
- 删除操作默认使用逻辑删除，不直接物理删除业务数据。
- 环境差异配置通过 `.env` 或系统环境变量注入，禁止在代码中硬编码数据库密码、Token 密钥、上传目录等敏感配置。
- 需求文档以 `../full.md` 为准，A 类需求必须优先实现并测试。

## 2. Git 规范

- 开发分支：从 `dev` 创建 `naph-fastDev-new`，禁止在 `naph-fastDev-1` 上继续开发。
- 提交信息格式：`type(scope): 中文描述`。
- 常用 type：`feat`、`fix`、`test`、`docs`、`refactor`、`chore`。
- 示例：`feat(auth): 完成新用户注册登录`。
- 每次提交只包含一个明确主题，避免混合无关模块。
- 提交前必须检查 `git status`、`git diff`，确认没有误提交 `.env`、日志、构建产物或 IDE 文件。
- 禁止使用破坏性命令回滚他人改动，除非得到明确授权。

## 3. 架构规范

- 包结构保持清晰分层：
  - `controller`：接收请求、参数校验、返回响应，不写复杂业务逻辑。
  - `service`：定义业务接口。
  - `service.impl`：实现业务规则、事务、权限相关业务判断。
  - `mapper`：数据库访问层，仅放 MyBatis-Plus Mapper。
  - `entity`：数据库实体，与表结构对应。
  - `dto`：请求入参对象，必须使用 Bean Validation 标注必要校验。
  - `common`：通用响应、分页、异常等基础能力。
  - `config`：框架配置。
  - `aspect`：横切能力，如日志记录。
  - `utils`：无状态工具类。
- 写操作必须考虑事务一致性，涉及多表写入的方法加 `@Transactional`。
- Controller 不直接操作 Mapper，必须通过 Service。
- Service 不返回前端不应看到的敏感字段，尤其是用户密码。
- 权限判断以后端为准，前端隐藏菜单不能代替接口鉴权。
- 合同流程状态必须由后端统一流转，禁止前端直接提交最终状态。
- 操作日志应记录新增、修改、删除、授权、合同流程推进等关键动作。

## 4. 注释规范

- Java 注释采用 JavaDoc 格式，作者统一为 `NAPH130`。
- 公共类、Controller、Service 接口、复杂业务方法必须添加 JavaDoc。
- JavaDoc 基本格式：

```java
/**
 * 用户认证控制器。
 *
 * @author NAPH130
 */
```

- 方法 JavaDoc 需说明业务含义、参数和返回值：

```java
/**
 * 注册新用户，注册后默认处于待授权状态。
 *
 * @param dto 注册参数
 * @author NAPH130
 */
```

- 禁止添加无意义注释，例如“设置用户名”“返回结果”。
- 对流程状态、权限编码、复杂查询条件必须补充说明。

## 5. 测试规范

- 每个业务模块必须有测试文件，至少覆盖成功路径、参数校验失败、权限失败、关键边界条件。
- 后端测试优先使用 JUnit 5、Spring Boot Test、MockMvc。
- 涉及数据库的测试应使用独立测试配置或可重复初始化的数据。
- 提交前至少执行 `mvn test`。

## 6. 安全规范

- 密码必须加密存储，禁止明文落库。
- 登录态与权限必须由 Sa-Token 统一控制。
- 上传文件必须校验扩展名、大小和保存路径。
- 日志禁止输出密码、Token、数据库密码等敏感信息。
- 管理员权限与合同操作权限必须区分，新注册用户默认无业务权限。
