package com.itheima.rpc.client.cluster.lb;

import com.itheima.rpc.annotation.HrpcLoadBalance;
import com.itheima.rpc.client.cluster.LoadBalanceStrategy;
import com.itheima.rpc.provider.ServiceProvider;
import org.apache.commons.lang3.RandomUtils;

import java.util.List;

/**
 * 随机负载均衡策略
 */
@HrpcLoadBalance(strategy = "random")
public class RandomLoadBalanceStrategy implements LoadBalanceStrategy {

    @Override
    public ServiceProvider select(List<ServiceProvider> serviceProviders) {
        /**
         * [0,len)
         */
        int len = serviceProviders.size();
        int index = RandomUtils.nextInt(0,len);
        return serviceProviders.get(index);
    }

}
