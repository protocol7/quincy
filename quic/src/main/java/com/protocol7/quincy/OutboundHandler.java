package com.protocol7.quincy;

import com.protocol7.quincy.protocol.packets.Packet;

public interface OutboundHandler {

  void beforeSendPacket(final Packet packet, final PipelineContext ctx);
}
