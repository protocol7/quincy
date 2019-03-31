package com.protocol7.nettyquic;

import com.protocol7.nettyquic.protocol.packets.Packet;

public interface InboundHandler {

  void onReceivePacket(final Packet packet, final PipelineContext ctx);
}
