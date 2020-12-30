package com.protocol7.quincy.connection;

import com.protocol7.quincy.FrameSender;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.TransportError;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.frames.FrameType;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.streams.Stream;
import com.protocol7.quincy.tls.EncryptionLevel;
import com.protocol7.quincy.tls.aead.AEAD;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.Optional;

public interface Connection extends FrameSender {

  Packet sendPacket(Packet p);

  Optional<ConnectionId> getLocalConnectionId();

  Optional<ConnectionId> getRemoteConnectionId();

  Version getVersion();

  AEAD getAEAD(EncryptionLevel level);

  Future<Void> close(TransportError error, FrameType frameType, String msg);

  InetSocketAddress getPeerAddress();

  Stream openStream();

  State getState();
}
