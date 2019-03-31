package com.protocol7.nettyquic;

import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.packets.FullPacket;

public interface FrameSender {
  FullPacket send(Frame... frames);
}
