package com.itheima.rpc.netty.codec;

import com.itheima.rpc.data.RpcRequest;
import com.itheima.rpc.util.ProtostuffUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class RpcRequestEncoder extends MessageToMessageEncoder<RpcRequest> {
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcRequest request, List<Object> out) throws Exception {
        try {
            byte[] bytes = ProtostuffUtil.serialize(request);
            ByteBuf buffer = ctx.alloc().buffer(bytes.length);
            buffer.writeBytes(bytes);
            out.add(buffer);
        } catch (Exception e) {
            log.error("RpcRequestEncoder encode error,msg={}",e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
