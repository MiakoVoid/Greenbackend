package com.it;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Slf4j
@SpringBootApplication
@EnableSwagger2
@EnableCaching
@EnableScheduling
@EnableTransactionManagement
@MapperScan("com.it.greenfinance.mapper")
public class AppStart {
    public static void main(String[] args) {
        SpringApplication.run(AppStart.class, args);
        log.info("启动成功");
    }
}