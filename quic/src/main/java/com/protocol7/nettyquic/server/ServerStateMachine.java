package com.protocol7.nettyquic.server;

import com.protocol7.nettyquic.connection.State;
import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.TransportError;
import com.protocol7.nettyquic.protocol.frames.*;
import com.protocol7.nettyquic.protocol.packets.*;
import com.protocol7.nettyquic.tls.EncryptionLevel;
import com.protocol7.nettyquic.tls.ServerTlsSession;
import com.protocol7.nettyquic.tls.ServerTlsSession.ServerHelloAndHandshake;
import com.protocol7.nettyquic.tls.aead.AEAD;
import com.protocol7.nettyquic.tls.aead.InitialAEAD;
import com.protocol7.nettyquic.tls.extensions.TransportParameters;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerStateMachine {

  private final Logger log = LoggerFactory.getLogger(ServerStateMachine.class);

  public State getState() {
    return state;
  }

  private State state = State.Started;
  private final ServerConnection connection;
  private ServerTlsSession tlsSession;
  private final TransportParameters transportParameters;
  private final PrivateKey privateKey;
  private final List<byte[]> certificates;

  public ServerStateMachine(
      final ServerConnection connection,
      final TransportParameters transportParameters,
      PrivateKey privateKey,
      List<byte[]> certificates) {
    this.connection = connection;
    this.privateKey = privateKey;
    this.certificates = certificates;
    this.transportParameters = transportParameters;

    resetTlsSession(connection.getLocalConnectionId().get());
  }

  public void resetTlsSession(ConnectionId localConnectionId) {
    this.tlsSession =
        new ServerTlsSession(
            InitialAEAD.create(localConnectionId.asBytes(), true),
            transportParameters,
            certificates,
            privateKey);
  }

  public synchronized void processPacket(Packet packet) {
    log.info(
        "Server got {} in state {} with connection ID {}",
        packet.getClass().getName(),
        state,
        packet.getDestinationConnectionId());

    // TODO check version
    if (state == State.Started) {
      if (packet instanceof InitialPacket) {
        InitialPacket initialPacket = (InitialPacket) packet;

        if (initialPacket.getToken().isPresent()) {
          //  TODO validate token

          connection.setRemoteConnectionId(packet.getSourceConnectionId().get());

          CryptoFrame cf = (CryptoFrame) initialPacket.getPayload().getFrames().get(0);

          ServerHelloAndHandshake shah = tlsSession.handleClientHello(cf.getCryptoData());

          tlsSession.setHandshakeAead(shah.getHandshakeAEAD());
          tlsSession.setOneRttAead(shah.getOneRttAEAD());

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

          state = State.BeforeReady;
        }
      } else {
        log.warn("Unexpected packet in BeforeInitial: " + packet);
      }
    } else if (state == State.BeforeReady) {
      FullPacket fp = (FullPacket) packet;
      CryptoFrame cryptoFrame = (CryptoFrame) fp.getPayload().getFrames().get(0);
      tlsSession.handleClientFinished(cryptoFrame.getCryptoData());

      state = State.Ready;

      handleFrames(fp);
    } else if (state == State.Ready) {
      handleFrames((FullPacket) packet);
    }
  }

  private void handleFrames(FullPacket packet) {
    for (Frame frame : packet.getPayload().getFrames()) {
      if (frame instanceof PingFrame) {
        // ignore
      } else if (frame instanceof ConnectionCloseFrame) {
        handlePeerClose();
      }
    }
  }

  private void handlePeerClose() {
    log.debug("Peer closing connection");
    state = State.Closing;
    connection.closeByPeer().awaitUninterruptibly(); // TODO fix, make async
    log.debug("Connection closed");
    state = State.Closed;
  }

  public void closeImmediate(final ConnectionCloseFrame ccf) {
    connection.send(ccf);

    state = State.Closing;

    state = State.Closed;
  }

  public void closeImmediate() {
    closeImmediate(
        ConnectionCloseFrame.connection(
            TransportError.NO_ERROR.getValue(), 0, "Closing connection"));
  }

  public void setState(final State state) {
    this.state = state;
  }

  public AEAD getAEAD(final EncryptionLevel level) {
    return tlsSession.getAEAD(level);
  }
}
