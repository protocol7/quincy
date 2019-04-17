package com.protocol7.quincy;

import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.protocol.TransportError;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.frames.FrameType;
import com.protocol7.quincy.protocol.packets.Packet;
import java.net.InetSocketAddress;

public interface PipelineContext extends FrameSender {

  void next(Packet packet);

  Version getVersion();

  void closeConnection(TransportError error, final FrameType frameType, final String msg);

  InetSocketAddress getPeerAddress();

  State getState();

  void setState(final State state);
}
