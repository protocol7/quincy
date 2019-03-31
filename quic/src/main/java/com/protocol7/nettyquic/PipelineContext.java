package com.protocol7.nettyquic;

import com.protocol7.nettyquic.protocol.TransportError;
import com.protocol7.nettyquic.protocol.frames.FrameType;
import com.protocol7.nettyquic.protocol.packets.Packet;

public interface PipelineContext extends FrameSender {

  void next(Packet packet);

  void closeConnection(TransportError error, final FrameType frameType, final String msg);
}
