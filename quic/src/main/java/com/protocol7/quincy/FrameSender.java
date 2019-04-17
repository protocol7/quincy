package com.protocol7.quincy;

import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.protocol.packets.Packet;

// TODO consolidate with Sender
public interface FrameSender {
  Packet sendPacket(Packet p);

  FullPacket send(Frame... frames);
}
