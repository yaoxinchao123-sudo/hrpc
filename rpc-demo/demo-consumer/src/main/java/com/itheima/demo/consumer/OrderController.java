package com.itheima.demo.consumer;

import com.itheima.demo.api.OrderService;
import com.itheima.rpc.annotation.HrpcRemote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单控制器 - 通过 @HrpcRemote 注入远程服务代理
 */
@Slf4j
@RestController
public class OrderController {

    /**
     * 使用 @HrpcRemote 注解标记的字段会被 RpcAnnotationProcessor 替换为 CGLIB 代理对象
     * 调用方法时实际通过 Netty 远程调用 provider 端的实现
     */
    @HrpcRemote
    private OrderService orderService;

    @GetMapping("/order/{orderId}")
    public String getOrder(@PathVariable String orderId) {
        log.info("consumer 收到请求, orderId={}", orderId);
        // 这里调用的是 hrpc 生成的代理对象，实际走 Netty 远程调用
        String order = orderService.getOrder(orderId);
        log.info("consumer 获取订单结果, order={}", order);
        return order;
    }
}
