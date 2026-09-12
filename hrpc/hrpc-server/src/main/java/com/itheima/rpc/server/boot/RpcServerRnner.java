package com.itheima.rpc.server.boot;

import com.itheima.rpc.server.registry.RpcRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RpcServerRnner {


    @Autowired
    private RpcRegistry registry;

    @Autowired
    RpcServer server;
    /**
     * 运行hprc-server服务端程序
     */
    public void run() {
        /**
         * 1,完成服务的注册【以接口为维度注册】
         *          /rpc
         *         /com.itheima.order.OrderService
         *        /192.168.200.200:8888
         * 2、基于netty编写一个服务端程序
         *     2.1,监听端口接受连接
         *     2.2,客户端数据发送过来之后,1次解码,2次解码
         *     2.3,进行逻辑处理:根据发送过来的数据,调用某个接口的某个实现类的某个方法
         *     2.4,将业务逻辑返回的数据写回到客户端,2次编码,1次编码
         */

        registry.serviceRegistry();

        server.start();
    }
}
