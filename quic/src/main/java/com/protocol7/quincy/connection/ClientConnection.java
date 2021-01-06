package com.protocol7.quincy.connection;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.flowcontrol.FlowControlHandler;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.streams.StreamListener;
import com.protocol7.quincy.tls.CertificateValidator;
import com.protocol7.quincy.tls.ClientTlsManager;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.Optional;
import org.slf4j.MDC;

public class ClientConnection extends AbstractConnection {

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
    super(
        configuration.getVersion(),
        peerAddress,
        sourceConnectionId,
        new ClientTlsManager(
            sourceConnectionId,
            configuration.getApplicationProtocols(),
            configuration.toTransportParameters(),
            certificateValidator),
        new ClientStateMachine(),
        packetSender,
        streamListener,
        true,
        flowControlHandler,
        configuration,
        timer,
        Optional.empty());

    setRemoteConnectionId(initialRemoteConnectionId);
  }

  public void handshake(final Promise promise) {
    MDC.put("actor", "client");
    tlsManager.handshake(getState(), this, stateMachine::setState, promise);
  }
}
