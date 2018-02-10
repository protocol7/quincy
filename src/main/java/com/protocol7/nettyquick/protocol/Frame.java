package com.protocol7.nettyquick.protocol;

import io.netty.buffer.ByteBuf;

public interface Frame {

  static Frame parse(ByteBuf bb) {
    return null;
  }

  void write(ByteBuf bb);
}
