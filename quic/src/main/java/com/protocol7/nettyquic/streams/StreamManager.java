package com.protocol7.nettyquic.streams;

import com.protocol7.nettyquic.connection.PacketHandler;

public interface StreamManager extends PacketHandler {

  Stream openStream(boolean client, boolean bidirectional);
}
