package com.cmc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan("com.cmc.mapper")
@EnableTransactionManagement
public class CmcApplication {
    public static void main(String[] args) {
        SpringApplication.run(CmcApplication.class, args);
    }
}
