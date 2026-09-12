package com.itheima.rpc.client.cluster;

import com.itheima.rpc.annotation.HrpcLoadBalance;
import com.itheima.rpc.client.config.RpcClientConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 根据用户在 yml中的配置，加载对应的策略
 */
@Component
@Slf4j
public class DefaultStartegyProvider implements StartegyProvider, ApplicationContextAware {

    /**
     * 用户的yml配置
     */
    @Autowired
    private RpcClientConfiguration clientConfiguration;

    LoadBalanceStrategy strategy;

    /**
     * 返回根据用户yml中配置的策略
     * @return
     */
    @Override
    public LoadBalanceStrategy getStrategy() {
        return strategy;
    }

    /**
     * 根据用户配置的策略，加载对应的策略类
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(HrpcLoadBalance.class);
        for (Object bean : beansWithAnnotation.values()) {
            HrpcLoadBalance loadBalance = bean.getClass().getAnnotation(HrpcLoadBalance.class);
            if (clientConfiguration.getRpcClientClusterStrategy().equals(loadBalance.strategy())) {
                strategy = (LoadBalanceStrategy) bean;
                break;
            }
        }
    }
}
