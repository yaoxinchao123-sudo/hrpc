package com.itheima.demo.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 服务消费者启动类
 * scanBasePackages 需要包含 com.itheima 以扫描到 hrpc-client 中的组件
 */
@SpringBootApplication(scanBasePackages = "com.itheima")
public class ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}
