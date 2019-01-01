package com.protocol7.nettyquic;

import io.netty.buffer.ByteBuf;

public interface Writeable {

  void write(ByteBuf bb);
}
