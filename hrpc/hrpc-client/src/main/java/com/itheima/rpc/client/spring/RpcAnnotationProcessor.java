package com.itheima.rpc.client.spring;

import com.itheima.rpc.annotation.HrpcRemote;
import com.itheima.rpc.proxy.ProxyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component // 首先将这个bean放到容器中
@Slf4j
public class RpcAnnotationProcessor implements BeanPostProcessor, ApplicationContextAware {

    private ProxyFactory proxyFacotory;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * bean初始化之后  会进入这个方法
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 对该bean进行处理

        // 1. 获取bean的所有的字段
        Field[] fields = bean.getClass().getDeclaredFields();
        // 遍历所有的字段
        for (Field field : fields) {
            // 如果字段不可访问，则设置可访问
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            // 获取字段上的HrpcRemote注解
            HrpcRemote hrpcRemote = field.getAnnotation(HrpcRemote.class);
            if (hrpcRemote!=null) {
                // 为该Field代表的类型生成代理  考虑代理技术  proxy,cglib,.....

                // 拿到字段的类型 (OrderService.class、UserService.class)
                Class<?> type = field.getType();
                // 为该类型生成代理
                Object proxy = proxyFacotory.newProxyInstance(type);
                log.info("为HrpcRemote注解标注的属性生成的代理对象:{}",proxy);
                if (proxy!=null) {
                    //完成代理的注入
                    try {
                        // 为该字段设置代理对象（为该field赋值）
                        /**
                         * field.set(bean, proxy) 就是反射赋值，完全等价于在这个 bean 内部执行了一次 this.orderService = proxy;。
                         *  - bean：当前正在被 Spring 初始化的那个对象实例（业务 bean 本身）
                         *  - field：它上面标了 @HrpcRemote 的那个字段
                         * 上面几行的 field.setAccessible(true) 是为了能写 private 字段；注意如果字段是 final，这里会直接抛 IllegalAccessException
                         * 因为 @HrpcRemote 的 @Target 是 ElementType.FIELD（见 HrpcRemote），所以只能这么按字段注入。
                         *
                         * 【**】赋值那一刻，业务 bean 的字段里躺着的不再是一个 null，而是一个 CGLIB 生成的代理对象。
                         *
                         * 【总结】 这个类就是为了给标注 @HrpcRemote 的field设置一个代理对象
                         */
                        field.set(bean,proxy);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.proxyFacotory = applicationContext.getBean(ProxyFactory.class);
    }
}
