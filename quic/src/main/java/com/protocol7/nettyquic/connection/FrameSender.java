package com.protocol7.nettyquic.connection;

import com.protocol7.nettyquic.protocol.TransportError;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.frames.FrameType;
import com.protocol7.nettyquic.protocol.packets.FullPacket;

public interface FrameSender {

  FullPacket send(Frame... frames);

  void closeConnection(TransportError error, final FrameType frameType, final String msg);
}
