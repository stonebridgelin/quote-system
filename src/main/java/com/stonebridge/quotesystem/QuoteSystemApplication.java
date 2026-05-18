package com.stonebridge.quotesystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.stonebridge.quotesystem.mapper") // 扫描 mapper 包
public class QuoteSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuoteSystemApplication.class, args);
    }

}
