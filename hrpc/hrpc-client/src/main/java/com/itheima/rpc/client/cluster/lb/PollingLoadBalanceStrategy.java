package com.itheima.rpc.client.cluster.lb;

import com.itheima.rpc.annotation.HrpcLoadBalance;
import com.itheima.rpc.client.cluster.LoadBalanceStrategy;
import com.itheima.rpc.provider.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 轮询负载均衡策略: 如果当前用了index这个下标，那么下次就要使用index+1
 */
@HrpcLoadBalance(strategy = "polling")
@Slf4j
public class PollingLoadBalanceStrategy implements LoadBalanceStrategy {

    /**
     * 定义一个指针：表示当前使用的
     */
    private int index;

    // 并发排序， 保证线程安全，防止多个线程同时修改index【多个线程同时不会获取同一个index值】
    private ReentrantLock lock = new ReentrantLock();

    @Override
    public ServiceProvider select(List<ServiceProvider> serviceProviders) {
        try {
            lock.tryLock(10, TimeUnit.SECONDS);
           /* if(index>=serviceProviders.size()) {
                index = 0;
            }*/
            ServiceProvider serviceProvider = serviceProviders.get(index);
//            index++;
            // [0,3)  0  1   2
            index = (index+1) %  serviceProviders.size();
            return serviceProvider;
        } catch (InterruptedException e) {
            log.error("轮询策略获取锁失败,msg={}",e.getMessage());
        } finally {
            lock.unlock();
        }
        return null;
    }
}
