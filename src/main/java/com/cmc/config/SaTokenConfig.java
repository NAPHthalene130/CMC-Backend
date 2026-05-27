package com.cmc.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
                    SaRouter.match("/**")
                            .notMatch("/api/auth/login", "/api/auth/register",
                                    "/doc.html", "/swagger-ui/**", "/v3/api-docs/**",
                                    "/webjars/**", "/favicon.ico")
                            .check(r -> StpUtil.checkLogin());
                    SaRouter.match("/api/contracts/draft", r -> StpUtil.checkPermission("C_DRAFT"));
                    SaRouter.match("/api/contracts/*/attachments", r -> StpUtil.checkPermission("C_DRAFT"));
                    SaRouter.match("/api/contracts/attachments/*/download", r -> StpUtil.checkPermission("C_QUERY"));
                    SaRouter.match("/api/contracts/*/finalize", r -> StpUtil.checkPermission("C_FINAL"));
                    SaRouter.match("/api/contracts", r -> StpUtil.checkPermission("C_QUERY"));
                    SaRouter.match("/api/process/countersign", r -> StpUtil.checkPermission("P_COUNTER"));
                    SaRouter.match("/api/process/approve", r -> StpUtil.checkPermission("P_APPROVE"));
                    SaRouter.match("/api/process/sign", r -> StpUtil.checkPermission("P_SIGN"));
                    SaRouter.match("/api/process/assign", r -> StpUtil.checkPermission("P_ASSIGN"));
                    SaRouter.match("/api/users/**", r -> StpUtil.checkPermission("U_MANAGE"));
                    SaRouter.match("/api/roles/**", r -> StpUtil.checkPermission("R_MANAGE"));
                    SaRouter.match("/api/functions/**", r -> StpUtil.checkPermission("F_MANAGE"));
                    SaRouter.match("/api/customers/**", r -> StpUtil.checkPermission("CU_MANAGE"));
                    SaRouter.match("/api/logs/**", r -> StpUtil.checkPermission("L_MANAGE"));
                }))
                .addPathPatterns("/**");
    }
}
