package com.protocol7.quincy.connection;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.flowcontrol.FlowControlHandler;
import com.protocol7.quincy.netty2.api.QuicTokenHandler;
import com.protocol7.quincy.protocol.*;
import com.protocol7.quincy.streams.StreamListener;
import com.protocol7.quincy.tls.ServerTlsManager;
import io.netty.util.Timer;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;

public class ServerConnection extends AbstractConnection {

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
    super(
        configuration.getVersion(),
        peerAddress,
        localConnectionId,
        new ServerTlsManager(
            localConnectionId, configuration.toTransportParameters(), privateKey, certificates),
        packetSender,
        streamListener,
        false,
        flowControlHandler,
        configuration,
        timer,
        Optional.of(tokenHandler));

    this.stateMachine = new ServerStateMachine(this);
  }
}
