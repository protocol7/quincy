package com.protocol7.nettyquick.server;

import static com.protocol7.nettyquick.server.ServerState.Ready;
import static com.protocol7.nettyquick.server.ServerState.WaitingForFinished;

import com.protocol7.nettyquick.protocol.TransportError;
import com.protocol7.nettyquick.protocol.frames.*;
import com.protocol7.nettyquick.protocol.packets.*;
import com.protocol7.nettyquick.streams.Stream;
import com.protocol7.nettyquick.tls.ServerTlsSession;
import com.protocol7.nettyquick.tls.ServerTlsSession.ServerHelloAndHandshake;
import com.protocol7.nettyquick.utils.Rnd;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerStateMachine {

  private final Logger log = LoggerFactory.getLogger(ServerStateMachine.class);

  public ServerState getState() {
    return state;
  }

  private ServerState state = ServerState.BeforeInitial;
  private final ServerConnection connection;
  private final ServerTlsSession tlsEngine;

  public ServerStateMachine(
      final ServerConnection connection, List<byte[]> certificates, PrivateKey privateKey) {
    this.connection = connection;
    tlsEngine = new ServerTlsSession(certificates, privateKey);
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

        if (initialPacket.getToken().isPresent()) {
          //  TODO validate token

          connection.setDestinationConnectionId(packet.getDestinationConnectionId().get());

          CryptoFrame cf = (CryptoFrame) initialPacket.getPayload().getFrames().get(0);

          ServerHelloAndHandshake shah = tlsEngine.handleClientHello(cf.getCryptoData());

          connection.setHandshakeAead(shah.getHandshakeAEAD());
          connection.setOneRttAead(shah.getOneRttAEAD());

          InitialPacket serverHello =
              InitialPacket.create(
                  connection.getDestinationConnectionId(),
                  connection.getSourceConnectionId(),
                  connection.nextSendPacketNumber(),
                  connection.getVersion(),
                  Optional.empty(),
                  new CryptoFrame(0, shah.getServerHello()));
          connection.sendPacket(serverHello);

          HandshakePacket handshake =
              HandshakePacket.create(
                  connection.getDestinationConnectionId(),
                  connection.getSourceConnectionId(),
                  connection.nextSendPacketNumber(),
                  connection.getVersion(),
                  new CryptoFrame(0, shah.getServerHandshake()));
          connection.sendPacket(handshake);

          state = WaitingForFinished;
        } else {
          byte[] retryToken = Rnd.rndBytes(34); // TODO generate a useful token

          RetryPacket retry =
              new RetryPacket(
                  connection.getVersion(),
                  Optional.empty(),
                  initialPacket.getSourceConnectionId(),
                  initialPacket.getDestinationConnectionId().get(),
                  retryToken);
          connection.sendPacket(retry);
        }
      } else {
        log.warn("Unexpected packet in BeforeInitial: " + packet);
      }
    } else if (state == WaitingForFinished) {
      FullPacket fp = (FullPacket) packet;
      CryptoFrame cryptoFrame = (CryptoFrame) fp.getPayload().getFrames().get(0);
      tlsEngine.handleClientFinished(cryptoFrame.getCryptoData());

      state = Ready;

      handleFrames(fp);
    } else if (state == Ready) {
      handleFrames((FullPacket) packet);
    }
  }

  private void handleFrames(FullPacket packet) {
    for (Frame frame : packet.getPayload().getFrames()) {

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
      } else if (frame instanceof ConnectionCloseFrame) {
        handlePeerClose();
      }
    }
  }

  private void handlePeerClose() {
    log.debug("Peer closing connection");
    state = ServerState.Closing;
    connection.closeByPeer().awaitUninterruptibly(); // TODO fix, make async
    log.debug("Connection closed");
    state = ServerState.Closed;
  }

  public void closeImmediate() {
    connection.sendPacket(
        ConnectionCloseFrame.connection(
            TransportError.NO_ERROR.getValue(), 0, "Closing connection"));

    state = ServerState.Closing;

    state = ServerState.Closed;
  }
}
