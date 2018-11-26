package com.protocol7.nettyquick.server;

import com.protocol7.nettyquick.protocol.frames.*;
import com.protocol7.nettyquick.protocol.packets.FullPacket;
import com.protocol7.nettyquick.protocol.packets.InitialPacket;
import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.streams.Stream;
import com.protocol7.nettyquick.tls.ClientTlsSession;
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
  private final ClientTlsSession tlsEngine = new ClientTlsSession();

  public ServerStateMachine(final ServerConnection connection) {
    this.connection = connection;
  }

  public synchronized void processPacket(Packet packet) {
    log.info(
        "Server got {} in state {} with connection ID {}",
        packet.getClass().getName(),
        state,
        packet.getDestinationConnectionId());

    // TODO check version
    if (state == ServerState.BeforeInitial) {
      if (packet instanceof InitialPacket) {
        InitialPacket initialPacket = (InitialPacket) packet;
        connection.setDestinationConnectionId(packet.getSourceConnectionId().get());

        CryptoFrame clientHello = (CryptoFrame) initialPacket.getPayload().getFrames().get(0);

        throw new RuntimeException("Not implemented");

        // CryptoFrame serverHello = new CryptoFrame(0,
        // tlsEngine.handleServerHello(clientHello.getCryptoData()));

        // Packet handshakePacket = InitialPacket.create(packet.getSourceConnectionId(),
        //                                                 connection.getSourceConnectionId(),
        //                                                connection.nextSendPacketNumber(),
        //                                                 initialPacket.getVersion(),
        //                                                Optional.empty(),
        //                                                 serverHello);
        // connection.sendPacket(handshakePacket);
        // state = ServerState.Ready;
        // log.info("Server connection state ready");
      } else {
        throw new RuntimeException("Unexpected packet in BeforeInitial: " + packet);
      }
    } else if (state == ServerState.Ready) {
      for (Frame frame : ((FullPacket) packet).getPayload().getFrames()) {
        if (frame instanceof StreamFrame) {
          StreamFrame sf = (StreamFrame) frame;
          Stream stream = connection.getOrCreateStream(sf.getStreamId());
          stream.onData(sf.getOffset(), sf.isFin(), sf.getData());
        } else if (frame instanceof ResetStreamFrame) {
          ResetStreamFrame rsf = (ResetStreamFrame) frame;
          Stream stream = connection.getOrCreateStream(rsf.getStreamId());
          stream.onReset(rsf.getApplicationErrorCode(), rsf.getOffset());
        } else if (frame instanceof PingFrame) {
          PingFrame pf = (PingFrame) frame;
        }
      }
    }
  }
}
