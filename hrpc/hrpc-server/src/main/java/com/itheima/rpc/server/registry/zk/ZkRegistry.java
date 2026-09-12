package com.itheima.rpc.server.registry.zk;

import com.itheima.rpc.annotation.HrpcService;
import com.itheima.rpc.server.config.RpcServerConfiguration;
import com.itheima.rpc.server.registry.RpcRegistry;
import com.itheima.rpc.spring.factorybean.SpringBeanFactory;
import com.itheima.rpc.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class ZkRegistry implements RpcRegistry {

    @Autowired
    private ServerZKit zKit;

    @Autowired
    private RpcServerConfiguration configuration;

    @Autowired
    private SpringBeanFactory springBeanFactory;


    /**
     * 完成服务的注册【以接口为维度注册】
     *          *     /rpc
     *          *       /com.itheima.order.OrderService
     *          *        /192.168.200.200:8888
     */
    @Override
    public void serviceRegistry() {
        // 找 @HrpcService 注解即可 ,找到了之后,根据其interfaceClass属性值就可以找到我们要向注册中心注册哪个接口子节点
        Map<String, Object> beanListByAnnotationClass = SpringBeanFactory.getBeanListByAnnotationClass(HrpcService.class);
        if (!beanListByAnnotationClass.isEmpty()) {

            //创建一个根节点
            zKit.createRootNode();
            String realIp = IpUtil.getRealIp();

            for (Object bean:beanListByAnnotationClass.values()) {
                //获取bean上的注解
                HrpcService hrpcService = bean.getClass().getAnnotation(HrpcService.class);
                //拿到 interfaceClass 属性 其实就是接口
                Class<?> interfaceClass = hrpcService.interfaceClass();
                // 获取接口名称
                String serviceName = interfaceClass.getName();
                //创建接口代表的字节点
                zKit.createPersistentNode(serviceName);

                //在代表接口的节点下创建,提供者信息节点
                String provierNode = serviceName +"/"+realIp + ":" + configuration.getRpcPort();
                zKit.createNode(provierNode);

                log.info("注册服务信息为:serviceName={},provierNode={}",serviceName,provierNode);
            }
        }
    }

}
