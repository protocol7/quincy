package com.protocol7.nettyquick.server;

import com.protocol7.nettyquick.protocol.Header;
import com.protocol7.nettyquick.protocol.LongHeader;
import com.protocol7.nettyquick.protocol.PacketType;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.protocol.frames.PingFrame;
import com.protocol7.nettyquick.protocol.frames.PongFrame;
import com.protocol7.nettyquick.protocol.frames.RstStreamFrame;
import com.protocol7.nettyquick.protocol.frames.StreamFrame;
import com.protocol7.nettyquick.protocol.packets.HandshakePacket;
import com.protocol7.nettyquick.protocol.packets.InitialPacket;
import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerStateMachine {

  private final Logger log = LoggerFactory.getLogger(ServerStateMachine.class);

  public ServerState getState() {
    return state;
  }

  protected enum ServerState {
    BeforeInitial,
    Ready
  }

  private ServerState state = ServerState.BeforeInitial;
  private final ServerConnection connection;

  public ServerStateMachine(final ServerConnection connection) {
    this.connection = connection;
  }

  public synchronized void processPacket(Packet packet) {
    log.info("Server got {} in state {} with connection ID {}", packet.getClass().getName(), state, packet.getDestinationConnectionId());

      // TODO check version
      if (state == ServerState.BeforeInitial) {
        if (packet instanceof InitialPacket) {
          if (packet instanceof InitialPacket) {
            InitialPacket initialPacket = (InitialPacket) packet;
            connection.setDestinationConnectionId(packet.getSourceConnectionId().get());

            Packet handshakePacket = HandshakePacket.create(packet.getSourceConnectionId(),
                                                            packet.getDestinationConnectionId(),
                                                            connection.nextSendPacketNumber(),
                                                            initialPacket.getVersion());
            connection.sendPacket(handshakePacket);
            state = ServerState.Ready;
            log.info("Server connection state ready");
          } else {
            log.warn("Unexpected packet type");
          }
        }
      } else if (state == ServerState.Ready) {
        for (Frame frame : packet.getPayload().getFrames()) {
          if (frame instanceof StreamFrame) {
            StreamFrame sf = (StreamFrame) frame;
            Stream stream = connection.getOrCreateStream(sf.getStreamId());
            stream.onData(sf.getOffset(), sf.isFin(), sf.getData());
          } else if (frame instanceof RstStreamFrame) {
            RstStreamFrame rsf = (RstStreamFrame) frame;
            Stream stream = connection.getOrCreateStream(rsf.getStreamId());
            stream.onReset(rsf.getApplicationErrorCode(), rsf.getOffset());
          } else if (frame instanceof PingFrame) {
            PingFrame pf = (PingFrame) frame;
            if (!pf.isEmpty()) {
              connection.sendPacket(new PongFrame(pf.getData()));
            }
          }
        }
      }
  }

}
