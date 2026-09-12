package com.itheima.rpc.client.boot;

import com.itheima.rpc.client.discovery.RpcServiceDiscovery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RpcClientRunner {

    @Autowired
    private RpcServiceDiscovery serviceDiscovery;

    /**
     * 1、服务发现
     *   从zk中获取根节点下的所有字节点信息,每个接口子节点就代表一个服务,它下面的节点就是提供者信息
     *
     * 2、考虑代理如何产生？
     */
    public void run() {
        serviceDiscovery.serviceDiscovery();
    }
}
