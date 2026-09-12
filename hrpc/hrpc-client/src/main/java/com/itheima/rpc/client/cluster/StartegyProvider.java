package com.itheima.rpc.client.cluster;


/**
 * @description
 * @author: ts
 * @create:2021-05-13 11:51
 */
public interface StartegyProvider {

    /***
     * 策略模式
     * @return
     */
    LoadBalanceStrategy getStrategy();
}
