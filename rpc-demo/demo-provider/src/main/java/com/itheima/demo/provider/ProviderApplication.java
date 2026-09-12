package com.itheima.demo.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 服务提供者启动类
 * scanBasePackages 需要包含 com.itheima 以扫描到 hrpc-server 中的组件
 */
@SpringBootApplication(scanBasePackages = "com.itheima")
public class ProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }
}
