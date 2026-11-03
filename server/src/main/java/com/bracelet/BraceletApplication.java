package com.bracelet;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 手串DIY定制后端启动类
 */
@SpringBootApplication
@MapperScan("com.bracelet.mapper")
public class BraceletApplication {

    public static void main(String[] args) {
        SpringApplication.run(BraceletApplication.class, args);
        System.out.println("========================================");
        System.out.println("  手串DIY定制服务启动成功!");
        System.out.println("  http://localhost:8080");
        System.out.println("========================================");
    }
}
