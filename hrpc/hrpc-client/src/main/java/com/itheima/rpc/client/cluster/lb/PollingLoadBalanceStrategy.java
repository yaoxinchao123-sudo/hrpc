package com.itheima.rpc.client.cluster.lb;

import com.itheima.rpc.annotation.HrpcLoadBalance;
import com.itheima.rpc.client.cluster.LoadBalanceStrategy;
import com.itheima.rpc.provider.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@HrpcLoadBalance(strategy = "polling")
@Slf4j
public class PollingLoadBalanceStrategy implements LoadBalanceStrategy {

    private int index;

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
