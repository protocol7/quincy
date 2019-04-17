package com.protocol7.quincy.streams;

import com.protocol7.quincy.InboundHandler;

public interface StreamManager extends InboundHandler {

  Stream openStream(boolean client, boolean bidirectional);
}
