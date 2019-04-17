package com.protocol7.quincy;

import com.protocol7.quincy.protocol.packets.Packet;

public interface InboundHandler {

  void onReceivePacket(final Packet packet, final PipelineContext ctx);
}
