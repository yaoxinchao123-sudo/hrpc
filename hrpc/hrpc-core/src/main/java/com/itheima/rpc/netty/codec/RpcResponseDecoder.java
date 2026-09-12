package com.itheima.rpc.netty.codec;

import com.itheima.rpc.data.RpcRequest;
import com.itheima.rpc.data.RpcResponse;
import com.itheima.rpc.util.ProtostuffUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class RpcResponseDecoder extends MessageToMessageDecoder<ByteBuf> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        try {
            int length = msg.readableBytes();
            byte[] bytes = new byte[length];
            msg.readBytes(bytes);
            //反序列化
            RpcResponse response = ProtostuffUtil.deserialize(bytes, RpcResponse.class);
            out.add(response);
        } catch (Exception e) {
            log.error("RpcResponseDecoder decode error,msg={}",e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
