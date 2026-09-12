package com.itheima.rpc.client.request;

import com.itheima.rpc.cache.ServiceProviderCache;
import com.itheima.rpc.client.cluster.LoadBalanceStrategy;
import com.itheima.rpc.client.cluster.StartegyProvider;
import com.itheima.rpc.data.RpcRequest;
import com.itheima.rpc.data.RpcResponse;
import com.itheima.rpc.enums.StatusEnum;
import com.itheima.rpc.exception.RpcException;
import com.itheima.rpc.netty.codec.FrameDecoder;
import com.itheima.rpc.netty.codec.FrameEncoder;
import com.itheima.rpc.netty.codec.RpcRequestEncoder;
import com.itheima.rpc.netty.codec.RpcResponseDecoder;
import com.itheima.rpc.netty.handler.RpcResponseHandler;
import com.itheima.rpc.netty.request.ChannelMapping;
import com.itheima.rpc.netty.request.RequestPromise;
import com.itheima.rpc.netty.request.RpcRequestHolder;
import com.itheima.rpc.provider.ServiceProvider;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class RpcRequestManager {

    @Autowired
    private ServiceProviderCache providerCache;


    @Autowired
    StartegyProvider startegyProvider;

    public RpcResponse sendRequest(RpcRequest request) {
        // 根据请求信息找到要将请求发送倒哪儿
        List<ServiceProvider> serviceProviders = providerCache.get(request.getClassName());
        // 说明该接口没有提供者（没有注册到zookeeper）
        if (CollectionUtils.isEmpty(serviceProviders)) {
            log.info("客户端没有发现可用发服务节点");
            throw new RpcException(StatusEnum.NOT_FOUND_SERVICE_PROVINDER);
        }
        // 找出一个具体的提供者
        // 负载均衡
        // 得到用户配置的负载均衡策略
        LoadBalanceStrategy strategy = startegyProvider.getStrategy();
        // 根据负载均衡策略，从服务提供者列表中选择一个具体的服务提供者
        ServiceProvider provider = strategy.select(serviceProviders);
        return requestByNetty(provider,request);
    }

    // 通过netty发送请求：创建一个netty客户端，发送请求
    private RpcResponse requestByNetty(ServiceProvider provider, RpcRequest request) {
        Channel channel = null;
        try {
            // 判断对端的channel是否已建立好: 客户端应用程序和服务端应用程序，只需要建立一次连接，后期复用
            // key:   ip:port
            // value: channel对象
            if (!RpcRequestHolder.channelExist(provider.getServerIp(),provider.getRpcPort())) {
                // netty的客户端代码
                EventLoopGroup group = new NioEventLoopGroup(0,new DefaultThreadFactory("worker" ));
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {

                                /**
                                 * 发送端（outbound, tail→head）：
                                 *   应用层对象 RpcRequest
                                 *     ↓ RpcRequestEncoder（序列化）
                                 *   ByteBuf
                                 *     ↓ FrameEncoder（加长度头）
                                 *   [4字节长度][字节流] → 网络
                                 *
                                 * 接收端（inbound, head→tail）：
                                 *   网络 → [4字节长度][字节流]
                                 *     ↓ FrameDecoder（剥长度头）
                                 *   ByteBuf
                                 *     ↓ RpcResponseDecoder（反序列化）
                                 *   应用层对象 RpcResponse
                                 *     ↓ RpcResponseHandler（业务分发）
                                 *   唤醒 Promise
                                 */
                                // 外层是帧协议（解决粘包），内层是序列化协议（解决对象<-->字节）。
                                // 发送时从内到外打包，接收时从外到内解包，顺序严格对称。
                                // 如果哪天要加加密/压缩，就在最外层再加一个 handler：发送端在 FrameEncoder 之后（更靠近 tail），接收端在 FrameDecoder 之前（更靠近 head），保持对称。
                                ChannelPipeline pipeline = ch.pipeline();
                                // [head] → FrameEncoder(out) → RpcRequestEncoder(out) → FrameDecoder(in) → RpcResponseDecoder(in) → RpcResponseHandler(in) → [tail]
                                // 写出去（发请求）的流向：RpcRequestEncoder（先）→ FrameEncoder（后）→ socket
                                //编码（发数据）
                                pipeline.addLast("FrameEncoder",new FrameEncoder());
                                pipeline.addLast("rpcRequestEncoder",new RpcRequestEncoder());
                                //解码（收数据）
                                // 收进来（收响应）的流向：socket → FrameDecoder（先）→ RpcResponseDecoder（后）→ RpcResponseHandler（最后）
                                pipeline.addLast("FrameDecoder",new FrameDecoder());
                                pipeline.addLast("rpcresponseDecoder",new RpcResponseDecoder());
                                pipeline.addLast("rpcResponseHandler",new RpcResponseHandler());
                            }
                        });
                try {
                    // 1. 建立连接，sync()是同步阻塞的，等待连接建立成功
                    ChannelFuture future = bootstrap.connect(provider.getServerIp(), provider.getRpcPort()).sync();
                    if (future.isSuccess()) {
                        // 连接建立成功，拿到channel
                        channel = future.channel();
                        // 保存channel，方便后续复用
                        RpcRequestHolder.addChannelMapping(new ChannelMapping(provider.getServerIp(), provider.getRpcPort(),channel));
                    }
                } catch (Exception e) {
                    log.error("连接建立失败, provider"+provider);
                    throw new RpcException("连接建立失败, provider"+provider);
                }
            }
            channel = RpcRequestHolder.getChannel(provider.getServerIp(), provider.getRpcPort());

            //2. 向对端发送数据
            // 创建promise，用于获取对端的响应结果
            // 针对每个请求构建一个promise，拿到响应后使用其对应的promise设置结果【RpcResponseHandler中设置 requestPromise.setSuccess(response); 然后requestPromise.get();结束阻塞，拿到结果返回】
            RequestPromise requestPromise = new RequestPromise(channel.eventLoop());
            // 建立请求和promise的映射
            RpcRequestHolder.addRequestPromise(request.getRequestId(), requestPromise);
            // 使用channel，发送数据
            channel.writeAndFlush(request);

            // 3. 等待promise返回结果
            try {
                // 阻塞等待，直到获取到异步异步执行的响应结果
                RpcResponse response = (RpcResponse) requestPromise.get();
                return response;
            } catch (ExecutionException e) {
                e.printStackTrace();
            }finally {
                // 请求处理完成，移除request和promise的映射
                RpcRequestHolder.removeRequestPromise(request.getRequestId());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 出现异常，返回一个默认的 RpcResponse对象
        return new RpcResponse();
    }
}
