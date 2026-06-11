package com.cmc.common;

/**
 * 系统常量
 */
public final class Constants {

    private Constants() {}

    /** 管理员角色 ID */
    public static final Long ROLE_ADMIN_ID = 1L;

    /** 管理员角色名称 */
    public static final String ROLE_ADMIN = "ADMIN";

    /** 合同流程类型 */
    public static final int PROCESS_TYPE_COUNTERSIGN = 1;
    public static final int PROCESS_TYPE_APPROVE = 2;
    public static final int PROCESS_TYPE_SIGN = 3;

    /** 合同流程状态 */
    public static final int PROCESS_STATE_PENDING = 0;
    public static final int PROCESS_STATE_COMPLETED = 1;
    public static final int PROCESS_STATE_REJECTED = 2;

    /** 合同状态类型 */
    public static final int CONTRACT_STATE_DRAFT = 1;
    public static final int CONTRACT_STATE_COUNTERSIGNED = 2;
    public static final int CONTRACT_STATE_FINALIZED = 3;
    public static final int CONTRACT_STATE_APPROVED = 4;
    public static final int CONTRACT_STATE_SIGNED = 5;
}
