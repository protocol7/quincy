package com.protocol7.nettyquic.server;

import static com.protocol7.nettyquic.server.ServerState.Ready;
import static com.protocol7.nettyquic.server.ServerState.WaitingForFinished;

import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.TransportError;
import com.protocol7.nettyquic.protocol.frames.*;
import com.protocol7.nettyquic.protocol.packets.*;
import com.protocol7.nettyquic.streams.Stream;
import com.protocol7.nettyquic.tls.ServerTlsSession;
import com.protocol7.nettyquic.tls.ServerTlsSession.ServerHelloAndHandshake;
import com.protocol7.nettyquic.tls.extensions.TransportParameters;
import com.protocol7.nettyquic.utils.Rnd;
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
      final ServerConnection connection,
      final TransportParameters transportParameters,
      PrivateKey privateKey,
      List<byte[]> certificates) {
    this.connection = connection;
    tlsEngine = new ServerTlsSession(transportParameters, certificates, privateKey);
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

          connection.setRemoteConnectionId(packet.getSourceConnectionId().get());

          CryptoFrame cf = (CryptoFrame) initialPacket.getPayload().getFrames().get(0);

          ServerHelloAndHandshake shah = tlsEngine.handleClientHello(cf.getCryptoData());

          connection.setHandshakeAead(shah.getHandshakeAEAD());
          connection.setOneRttAead(shah.getOneRttAEAD());

          InitialPacket serverHello =
              InitialPacket.create(
                  connection.getRemoteConnectionId(),
                  connection.getLocalConnectionId(),
                  connection.nextSendPacketNumber(),
                  connection.getVersion(),
                  Optional.empty(),
                  new CryptoFrame(0, shah.getServerHello()));
          connection.sendPacket(serverHello);

          HandshakePacket handshake =
              HandshakePacket.create(
                  connection.getRemoteConnectionId(),
                  connection.getLocalConnectionId(),
                  connection.nextSendPacketNumber(),
                  connection.getVersion(),
                  new CryptoFrame(0, shah.getServerHandshake()));
          connection.sendPacket(handshake);

          state = WaitingForFinished;
        } else {
          byte[] retryToken = Rnd.rndBytes(34); // TODO generate a useful token

          ConnectionId newLocalConnectionId = ConnectionId.random();
          connection.setLocalConnectionId(newLocalConnectionId);

          RetryPacket retry =
              new RetryPacket(
                  connection.getVersion(),
                  initialPacket.getSourceConnectionId(),
                  Optional.of(newLocalConnectionId),
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

  public void closeImmediate(final ConnectionCloseFrame ccf) {
    connection.sendPacket(ccf);

    state = ServerState.Closing;

    state = ServerState.Closed;
  }

  public void closeImmediate() {
    closeImmediate(
        ConnectionCloseFrame.connection(
            TransportError.NO_ERROR.getValue(), 0, "Closing connection"));
  }
}
