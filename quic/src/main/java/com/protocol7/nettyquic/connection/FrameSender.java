package com.protocol7.nettyquic.connection;

import com.protocol7.nettyquic.protocol.TransportError;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.frames.FrameType;

public interface FrameSender {

  void send(Frame... frames);

  void closeConnection(TransportError error, final FrameType frameType, final String msg);
}
