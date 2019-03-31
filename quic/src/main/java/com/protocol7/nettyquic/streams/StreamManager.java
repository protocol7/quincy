package com.protocol7.nettyquic.streams;

import com.protocol7.nettyquic.InboundHandler;

public interface StreamManager extends InboundHandler {

  Stream openStream(boolean client, boolean bidirectional);
}
