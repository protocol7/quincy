package com.protocol7.nettyquic;

import com.protocol7.nettyquic.protocol.packets.Packet;

public interface OutboundHandler {

  void beforeSendPacket(final Packet packet, final PipelineContext ctx);
}
