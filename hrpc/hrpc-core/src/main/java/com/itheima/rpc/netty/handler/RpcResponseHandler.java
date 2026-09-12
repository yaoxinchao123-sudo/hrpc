package com.itheima.rpc.netty.handler;

import com.itheima.rpc.data.RpcResponse;
import com.itheima.rpc.netty.request.RequestPromise;
import com.itheima.rpc.netty.request.RpcRequestHolder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class RpcResponseHandler extends SimpleChannelInboundHandler<RpcResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        //处理响应
        // 从映射中获取promise
        RequestPromise requestPromise = RpcRequestHolder.getRequestPromise(response.getRequestId());
        if (requestPromise!=null) {
            // 设置响应结果， 这里设置后，promise就结束阻塞，可以获取结果了
            requestPromise.setSuccess(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("客户端Channel异常，remoteAddress={}，异常信息：{}", ctx.channel().remoteAddress(), cause.getMessage());
        // 通道异常时，将所有等待中的Promise设置为失败，防止线程永久阻塞
        RpcRequestHolder.failAllPromises(cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.warn("客户端Channel连接已关闭，remoteAddress={}", ctx.channel().remoteAddress());
        // 通道关闭时，将所有等待中的Promise设置为失败
        RpcRequestHolder.failAllPromises(new RuntimeException("Channel连接已关闭"));
        super.channelInactive(ctx);
    }
}
