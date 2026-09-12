package com.itheima.rpc.netty.codec;

import io.netty.handler.codec.LengthFieldPrepender;
import lombok.extern.slf4j.Slf4j;

/**
 * 一次解码（协议解析）
 */
@Slf4j
public class FrameEncoder extends LengthFieldPrepender {
    public FrameEncoder() {
        super(4);
    }
}
