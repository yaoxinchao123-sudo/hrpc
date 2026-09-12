package com.itheima.rpc.netty.codec;

import com.itheima.rpc.data.RpcRequest;
import com.itheima.rpc.util.ProtostuffUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class RpcRequestDecoder extends MessageToMessageDecoder<ByteBuf> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        try {
            int length = msg.readableBytes();
            byte[] bytes = new byte[length];
            msg.readBytes(bytes);
            //反序列化
            RpcRequest request = ProtostuffUtil.deserialize(bytes, RpcRequest.class);
            // 解码结果放到out中，等待业务逻辑处理
            out.add(request);
        } catch (Exception e) {
            log.error("RpcRequestDecoder decode error,msg={}",e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
