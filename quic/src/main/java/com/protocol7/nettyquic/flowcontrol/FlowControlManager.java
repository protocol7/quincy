package com.protocol7.nettyquic.flowcontrol;

import com.protocol7.nettyquic.protocol.StreamId;
import com.protocol7.nettyquic.protocol.packets.FullPacket;

public interface FlowControlManager {

  boolean tryConsume(final StreamId sid, final long offset, final FrameSender sender);

  void onReceivePacket(final FullPacket packet, final FrameSender sender);
}
