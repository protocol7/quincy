package com.protocol7.nettyquic;

import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;

// TODO consolidate with Sender
public interface FrameSender {
  Packet sendPacket(Packet p);

  FullPacket send(Frame... frames);
}
