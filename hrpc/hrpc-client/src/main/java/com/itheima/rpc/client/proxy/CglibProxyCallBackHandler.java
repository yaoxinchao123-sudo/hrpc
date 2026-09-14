package com.itheima.rpc.client.proxy;

import com.itheima.rpc.client.request.RpcRequestManager;
import com.itheima.rpc.data.RpcRequest;
import com.itheima.rpc.data.RpcResponse;
import com.itheima.rpc.exception.RpcException;
import com.itheima.rpc.spring.factorybean.SpringBeanFactory;
import com.itheima.rpc.util.RequestIdUtil;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 *为什么方法调用会进入这个类？
 * 整个调用链路分三步：
 *   1.@HrpcRemote 注解 — 消费端声明需要远程调用的服务接口；
 *   2.RequestProxyFactory.newProxyInstance() — 使用 CGLIB 的 Enhancer 为该接口创建子类代理，并将 CglibProxyCallBackHandler 设置为回调对象（enhancer.setCallback(...)）；
 *   3.intercept() 被触发 — 当消费端调用接口方法时，CGLIB 拦截该方法调用，自动转发到本类的 intercept 方法，将本地调用转换为远程 RPC 请求。
 * 简而言之：CGLIB 代理机制 决定了所有对代理对象的方法调用，都会被路由到实现了 MethodInterceptor 接口的 intercept() 方法中。
 */
public class CglibProxyCallBackHandler implements MethodInterceptor {


    /**
     * 代理的拦截
     * @param o
     * @param method
     * @param parameters
     * @param methodProxy
     * @return
     * @throws Throwable
     */
    public Object intercept(Object o, Method method, Object[] parameters, MethodProxy methodProxy) throws Throwable {
        //放过toString,hashcode，equals等方法，采用spring工具类
        if ( ReflectionUtils.isObjectMethod(method)) {
            return method.invoke(method.getDeclaringClass().newInstance(),parameters);
        }
        //封装RPC调用
        String requestId = RequestIdUtil.requestId();
        String serviceName = method.getDeclaringClass().getName();
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        // 1. 创建请求对象
        RpcRequest request = RpcRequest.builder()
                .requestId(requestId)
                .className(serviceName)
                .methodName(methodName)
                .parameterTypes(parameterTypes)
                .parameters(parameters)
                .build();

        // 2.使用请求处理器发送请求获取响应
        RpcRequestManager rpcRequestManager = SpringBeanFactory.getBean(RpcRequestManager.class);
        if (rpcRequestManager==null) {
            throw new RpcException("spring ioc exception");
        }
        RpcResponse response = rpcRequestManager.sendRequest(request);
        // 3.返回结果
        return response.getResult();
    }
}
