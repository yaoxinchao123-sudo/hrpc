package com.itheima.rpc.netty.handler;

import com.itheima.rpc.data.RpcRequest;
import com.itheima.rpc.data.RpcResponse;
import com.itheima.rpc.spring.factorybean.SpringBeanFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

@Slf4j
@ChannelHandler.Sharable
public class RpcRequestHandler extends SimpleChannelInboundHandler<RpcRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {
        log.info("收到了请求request={}",request);
        RpcResponse response = new RpcResponse();
        response.setRequestId(request.getRequestId());
        try {
            //请求中还有其他什么信息呢?
            String interfaceName = request.getClassName();
            String methodName = request.getMethodName();
            Class<?>[] parameterTypes = request.getParameterTypes();
            Object[] parameters = request.getParameters();

            // 1.从容器中找到目标bean
            Object bean = SpringBeanFactory.getBean(Class.forName(interfaceName));
            // 2.获取要执行的方法
            Method method = bean.getClass().getMethod(methodName, parameterTypes);
            // 3.执行方法
            //【熔断】【可以在这里处理熔断】：判断某接口调用错误超过阈值，就不调用
            Object result = method.invoke(bean, parameters);
            // 4.封装结果
            response.setResult(result);
        }  catch (Exception e) {
            // 【降级】【可以在这里处理降级】： 出现异常了，返回降级的结果
            response.setCause(e);
            log.error("rpc server invoke error,msg={}",e.getMessage());
        } finally {
            // 将响应写回
            log.info("服务端执行成功，响应为:{}",response);
            ctx.writeAndFlush(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("服务端出现异常,异常信息为:{}",cause.getCause());
        super.exceptionCaught(ctx, cause);
    }
}
