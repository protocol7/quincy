package com.protocol7.nettyquic.flowcontrol;

import com.protocol7.nettyquic.connection.FrameSender;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;

public interface FlowControlHandler {

  void beforeSendPacket(final Packet packet, final FrameSender sender);

  void onReceivePacket(final FullPacket packet, final FrameSender sender);
}
