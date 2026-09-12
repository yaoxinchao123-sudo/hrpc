package com.itheima.rpc.client.cluster.lb;

import com.itheima.rpc.annotation.HrpcLoadBalance;
import com.itheima.rpc.client.cluster.LoadBalanceStrategy;
import com.itheima.rpc.provider.ServiceProvider;
import com.itheima.rpc.util.IpUtil;

import java.util.List;

@HrpcLoadBalance(strategy = "hash")
public class HashLoadBalanceStrategy implements LoadBalanceStrategy {
    @Override
    public ServiceProvider select(List<ServiceProvider> serviceProviders) {
        /**
         * 1、获取客户端ip
         * 2、获取ip hash
         * 3、index = hash % serviceProviders.size()
         * 4、get(index)
         */
        String ip = IpUtil.getRealIp();
        int hashCode = ip.hashCode();
        int index = Math.abs(hashCode % serviceProviders.size());
        return serviceProviders.get(index);
    }
}
