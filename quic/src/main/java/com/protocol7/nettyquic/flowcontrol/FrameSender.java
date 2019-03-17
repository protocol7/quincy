package com.protocol7.nettyquic.flowcontrol;

import com.protocol7.nettyquic.protocol.TransportError;
import com.protocol7.nettyquic.protocol.frames.Frame;

public interface FrameSender {

  void send(Frame... frames);

  void closeConnection(TransportError error);
}
