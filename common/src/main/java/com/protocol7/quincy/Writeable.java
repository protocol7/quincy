package com.protocol7.quincy;

import io.netty.buffer.ByteBuf;

public interface Writeable {

  void write(ByteBuf bb);
}
