package com.protocol7.nettyquic.connection;

import com.protocol7.nettyquic.protocol.packets.FullPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;

public interface PacketHandler {

  void beforeSendPacket(final Packet packet, final FrameSender sender);

  void onReceivePacket(final FullPacket packet, final FrameSender sender);
}
