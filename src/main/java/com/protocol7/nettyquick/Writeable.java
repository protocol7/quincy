package com.protocol7.nettyquick;

import io.netty.buffer.ByteBuf;

public interface Writeable {

    void write(ByteBuf bb);
}
