package com.itheima.demo.provider;

import com.itheima.demo.api.OrderService;
import com.itheima.rpc.annotation.HrpcService;
import lombok.extern.slf4j.Slf4j;

/**
 * 订单服务实现类
 * 使用 @HrpcService 注解将服务发布到注册中心（ZooKeeper）
 */
@Slf4j
@HrpcService(interfaceClass = OrderService.class)
public class OrderServiceImpl implements OrderService {

    @Override
    public String getOrder(String orderId) {
        log.info("收到订单查询请求, orderId={}", orderId);
        return "Order{id=" + orderId + ", status=PAID, amount=99.90}";
    }
}
