package com.protocol7.quincy.connection;

import static com.protocol7.quincy.protocol.packets.Packet.getEncryptionLevel;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.Pipeline;
import com.protocol7.quincy.flowcontrol.FlowControlHandler;
import com.protocol7.quincy.logging.LoggingHandler;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.reliability.AckDelay;
import com.protocol7.quincy.reliability.PacketBufferManager;
import com.protocol7.quincy.streams.DefaultStreamManager;
import com.protocol7.quincy.streams.Stream;
import com.protocol7.quincy.streams.StreamListener;
import com.protocol7.quincy.streams.StreamManager;
import com.protocol7.quincy.termination.TerminationManager;
import com.protocol7.quincy.tls.CertificateValidator;
import com.protocol7.quincy.tls.ClientTlsManager;
import com.protocol7.quincy.tls.EncryptionLevel;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.utils.Ticker;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.MDC;

public class ClientConnection extends AbstractConnection {

  private final StreamManager streamManager;
  private final ClientTlsManager tlsManager;
  private final Pipeline pipeline;

  public ClientConnection(
      final Configuration configuration,
      final ConnectionId initialRemoteConnectionId,
      final ConnectionId sourceConnectionId,
      final StreamListener streamListener,
      final PacketSender packetSender,
      final FlowControlHandler flowControlHandler,
      final InetSocketAddress peerAddress,
      final CertificateValidator certificateValidator,
      final Timer timer) {
    super(configuration.getVersion(), peerAddress, sourceConnectionId, packetSender);

    this.stateMachine = new ClientStateMachine(this);

    setRemoteConnectionId(initialRemoteConnectionId);

    this.streamManager = new DefaultStreamManager(this, streamListener);

    final Ticker ticker = Ticker.systemTicker();

    final PacketBufferManager packetBuffer =
        new PacketBufferManager(
            new AckDelay(configuration.getAckDelayExponent(), ticker), this, timer, ticker);
    this.tlsManager =
        new ClientTlsManager(
            sourceConnectionId,
            configuration.getApplicationProtocols(),
            configuration.toTransportParameters(),
            certificateValidator);

    final LoggingHandler logger = new LoggingHandler(true);

    final TerminationManager terminationManager =
        new TerminationManager(this, timer, configuration.getIdleTimeout(), TimeUnit.SECONDS);

    this.pipeline =
        new Pipeline(
            List.of(
                logger,
                tlsManager,
                packetBuffer,
                streamManager,
                flowControlHandler,
                terminationManager),
            List.of(packetBuffer, logger));
  }

  public void handshake(final Promise promise) {
    MDC.put("actor", "client");
    tlsManager.handshake(getState(), this, stateMachine::setState, promise);
  }

  public void reset(final ConnectionId remoteConnectionId) {
    setRemoteConnectionId(remoteConnectionId);

    resetSendPacketNumber();

    tlsManager.resetTlsSession(remoteConnectionId);
  }

  public void onPacket(final Packet packet) {
    final EncryptionLevel encLevel = getEncryptionLevel(packet);
    if (tlsManager.available(encLevel)) {
      stateMachine.handlePacket(packet);
      if (getState() != State.Closed) {
        pipeline.onPacket(this, packet);
      }
    } else {
      // TODO handle unencryptable packet
    }
  }

  @Override
  public AEAD getAEAD(final EncryptionLevel level) {
    return tlsManager.getAEAD(level);
  }

  public Stream openStream() {
    return streamManager.openStream(true, true);
  }

  @Override
  protected Pipeline getPipeline() {
    return pipeline;
  }
}
