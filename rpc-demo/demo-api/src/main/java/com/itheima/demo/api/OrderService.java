package com.itheima.demo.api;

/**
 * 订单服务接口 - 由 provider 实现，consumer 远程调用
 */
public interface OrderService {

    /**
     * 根据订单ID查询订单信息
     *
     * @param orderId 订单ID
     * @return 订单信息
     */
    String getOrder(String orderId);
}
