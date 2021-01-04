package com.protocol7.quincy.connection;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.Pipeline;
import com.protocol7.quincy.addressvalidation.ServerRetryHandler;
import com.protocol7.quincy.flowcontrol.FlowControlHandler;
import com.protocol7.quincy.logging.LoggingHandler;
import com.protocol7.quincy.netty2.api.QuicTokenHandler;
import com.protocol7.quincy.protocol.*;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.reliability.AckDelay;
import com.protocol7.quincy.reliability.PacketBufferManager;
import com.protocol7.quincy.streams.DefaultStreamManager;
import com.protocol7.quincy.streams.Stream;
import com.protocol7.quincy.streams.StreamListener;
import com.protocol7.quincy.streams.StreamManager;
import com.protocol7.quincy.termination.TerminationManager;
import com.protocol7.quincy.tls.EncryptionLevel;
import com.protocol7.quincy.tls.ServerTLSManager;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.utils.Ticker;
import io.netty.util.Timer;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ServerConnection extends AbstractConnection {

  private final ServerTLSManager tlsManager;
  private final Pipeline pipeline;
  private final StreamManager streamManager;

  public ServerConnection(
      final Configuration configuration,
      final ConnectionId localConnectionId,
      final StreamListener streamListener,
      final PacketSender packetSender,
      final List<byte[]> certificates,
      final PrivateKey privateKey,
      final FlowControlHandler flowControlHandler,
      final InetSocketAddress peerAddress,
      final Timer timer,
      final QuicTokenHandler tokenHandler) {
    super(configuration.getVersion(), peerAddress, localConnectionId, packetSender);

    this.stateMachine = new ServerStateMachine(this);

    this.streamManager = new DefaultStreamManager(this, streamListener);

    final Ticker ticker = Ticker.systemTicker();

    final PacketBufferManager packetBuffer =
        new PacketBufferManager(
            new AckDelay(configuration.getAckDelayExponent(), ticker), this, timer, ticker);

    this.tlsManager =
        new ServerTLSManager(
            localConnectionId, configuration.toTransportParameters(), privateKey, certificates);

    final LoggingHandler logger = new LoggingHandler(false);

    final TerminationManager terminationManager =
        new TerminationManager(this, timer, configuration.getIdleTimeout(), TimeUnit.SECONDS);

    this.pipeline =
        new Pipeline(
            List.of(
                logger,
                new ServerRetryHandler(tokenHandler),
                tlsManager,
                packetBuffer,
                streamManager,
                flowControlHandler,
                terminationManager),
            List.of(flowControlHandler, packetBuffer, logger));
  }

  @Override
  protected Pipeline getPipeline() {
    return pipeline;
  }

  public void onPacket(final Packet packet) {
    // with incorrect conn ID
    stateMachine.handlePacket(packet);

    pipeline.onPacket(this, packet);
  }

  @Override
  public AEAD getAEAD(final EncryptionLevel level) {
    return tlsManager.getAEAD(level);
  }

  public Stream openStream() {
    return streamManager.openStream(false, true);
  }
}
