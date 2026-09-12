package com.itheima.rpc.client.discovery.zk;

import com.itheima.rpc.cache.ServiceProviderCache;
import com.itheima.rpc.client.discovery.RpcServiceDiscovery;
import com.itheima.rpc.provider.ServiceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class ZkServiceDiscovery implements RpcServiceDiscovery {



    @Autowired
    private ClientZKit zKit;

    @Autowired
    private ServiceProviderCache cache;

    //完成服务发现
    @Override
    public void serviceDiscovery() {
        //拉取所有的服务列表
        List<String> serviceList = zKit.getServiceList();
        if (!serviceList.isEmpty()) {

            for (String serviceName : serviceList) {//serviceName=com.itheima.shop.order.OrderService
                //获取该接口下的提供者信息
                List<ServiceProvider> providers = zKit.getServiceInfos(serviceName);
                //将该接口服务及提供者信息缓存
                log.info("订阅的服务名为={},服务提供者有={}",serviceName,providers);
                cache.put(serviceName,providers);
                //订阅变更
                zKit.subscribeZKEvent(serviceName);
            }
        }
    }
}
