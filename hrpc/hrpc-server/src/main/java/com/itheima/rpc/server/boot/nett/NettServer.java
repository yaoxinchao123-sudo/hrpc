package com.itheima.rpc.server.boot.nett;

import com.itheima.rpc.netty.codec.FrameDecoder;
import com.itheima.rpc.netty.codec.FrameEncoder;
import com.itheima.rpc.netty.codec.RpcRequestDecoder;
import com.itheima.rpc.netty.codec.RpcResponseEncoder;
import com.itheima.rpc.netty.handler.RpcRequestHandler;
import com.itheima.rpc.server.boot.RpcServer;
import com.itheima.rpc.server.config.RpcServerConfiguration;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NettServer implements RpcServer {

    @Autowired
    private RpcServerConfiguration rpcServerConfiguration;


    /**
     * 基于netty编写一个服务端程序
     *          *     2.1,监听端口接受连接
     *          *     2.2,客户端数据发送过来之后,1次解码,2次解码
     *          *     2.3,进行逻辑处理:根据发送过来的数据,调用某个接口的某个实现类的某个方法
     *          *     2.4,将业务逻辑返回的数据写回到客户端,2次编码,1次编码
     */
    @Override
    public void start() {
        EventLoopGroup boss = new NioEventLoopGroup(1,new DefaultThreadFactory("boss"));
        EventLoopGroup worker = new NioEventLoopGroup(0,new DefaultThreadFactory("worker"));
        EventExecutorGroup business = new UnorderedThreadPoolEventExecutor(NettyRuntime.availableProcessors()*2,new DefaultThreadFactory("business"));
        // 在独立线程中启动 Netty 服务器，避免阻塞 Spring Boot 主线程
        new Thread(() -> {
            try {
                ServerBootstrap serverBootstrap = new ServerBootstrap();
                serverBootstrap.group(boss,worker)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG,1024)
                        .childOption(ChannelOption.TCP_NODELAY,true)
                        .childOption(ChannelOption.SO_KEEPALIVE,true)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline pipeline = ch.pipeline();
                                //编码
                                pipeline.addLast("frameEncoder",new FrameEncoder());
                                pipeline.addLast("responseEncoder",new RpcResponseEncoder());
                                // 解码
                                pipeline.addLast("frameDecoder",new FrameDecoder());
                                pipeline.addLast("requestDecoder",new RpcRequestDecoder());
                                // business处理handler
                                pipeline.addLast(business,"rpcRequestHandler",new RpcRequestHandler());
                            }
                        });
                // bind是个异步的，sync将异步是个同步的
                ChannelFuture future = serverBootstrap.bind(rpcServerConfiguration.getRpcPort()).sync();
                log.info("服务端绑定端口{}启动成功",rpcServerConfiguration.getRpcPort());
                // closeFuture是个异步的，sync将异步操作转换成同步操作， 阻塞当前线程，直到服务端关闭
                future.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                log.error("服务端出现异常,msg={}",e.getMessage());
            } finally {
                business.shutdownGracefully();
                worker.shutdownGracefully();
                boss.shutdownGracefully();
            }
        }, "netty-server-starter").start();
    }


}
