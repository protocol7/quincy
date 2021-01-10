package com.protocol7.quincy.connection;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.FrameSender;
import com.protocol7.quincy.flowcontrol.FlowControlHandler;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.TransportError;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.frames.FrameType;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.streams.Stream;
import com.protocol7.quincy.streams.StreamHandler;
import com.protocol7.quincy.tls.CertificateValidator;
import com.protocol7.quincy.tls.ClientTlsManager;
import com.protocol7.quincy.tls.EncryptionLevel;
import com.protocol7.quincy.tls.ServerTlsManager;
import com.protocol7.quincy.tls.aead.AEAD;
import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.List;

public interface Connection extends FrameSender {

  static ConnectionBootstrap newBootstrap(final Channel channel) {
    return new ConnectionBootstrap(channel);
  }

  static Connection forServer(
      final Configuration configuration,
      final ConnectionId localConnectionId,
      final ConnectionId remoteConnectionId,
      final ConnectionId originalRemoteConnectionId,
      final StreamHandler streamListener,
      final PacketSender packetSender,
      final List<byte[]> certificates,
      final PrivateKey privateKey,
      final FlowControlHandler flowControlHandler,
      final InetSocketAddress peerAddress,
      final Timer timer) {
    return new DefaultConnection(
        false,
        configuration,
        remoteConnectionId,
        localConnectionId,
        peerAddress,
        new ServerStateMachine(),
        packetSender,
        streamListener,
        new ServerTlsManager(
            localConnectionId,
            originalRemoteConnectionId,
            configuration.getApplicationProtocols(),
            configuration.toTransportParameters(),
            privateKey,
            certificates),
        flowControlHandler,
        timer);
  }

  static Connection forClient(
      final Configuration configuration,
      final ConnectionId initialRemoteConnectionId,
      final ConnectionId localConnectionId,
      final StreamHandler streamHandler,
      final PacketSender packetSender,
      final FlowControlHandler flowControlHandler,
      final InetSocketAddress peerAddress,
      final CertificateValidator certificateValidator,
      final Timer timer) {
    return new DefaultConnection(
        true,
        configuration,
        initialRemoteConnectionId,
        localConnectionId,
        peerAddress,
        new ClientStateMachine(),
        packetSender,
        streamHandler,
        new ClientTlsManager(
            localConnectionId,
            configuration.getApplicationProtocols(),
            configuration.toTransportParameters(),
            certificateValidator),
        flowControlHandler,
        timer);
  }

  void handshake(final Promise promise);

  Packet sendPacket(Packet p);

  void setRemoteConnectionId(final ConnectionId remoteConnectionId);

  Version getVersion();

  AEAD getAEAD(EncryptionLevel level);

  Future<Void> close(TransportError error, FrameType frameType, String msg);

  Future<Void> close();

  InetSocketAddress getPeerAddress();

  Stream openStream();

  State getState();

  void onPacket(Packet packet);

  void setState(State state);

  void closeByPeer();

  void reset(ConnectionId sourceConnectionId);

  void setToken(byte[] retryToken);

  interface Listener {
    void action();
  }

  void addCloseListener(Listener listener);
}
