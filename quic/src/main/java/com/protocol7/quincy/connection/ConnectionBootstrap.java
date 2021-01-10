package com.protocol7.quincy.connection;

import static java.util.Objects.requireNonNull;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.flowcontrol.DefaultFlowControlHandler;
import com.protocol7.quincy.netty.QuicClientHandler;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.streams.StreamHandler;
import com.protocol7.quincy.tls.CertificateValidator;
import com.protocol7.quincy.tls.NoopCertificateValidator;
import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;

public class ConnectionBootstrap {

  private final Channel channel;
  private StreamHandler streamHandler = (stream, data, finished) -> {};
  private CertificateValidator certificateValidator = NoopCertificateValidator.INSTANCE;

  protected ConnectionBootstrap(final Channel channel) {
    this.channel = requireNonNull(channel);
  }

  public ConnectionBootstrap withStreamHandler(final StreamHandler streamHandler) {
    this.streamHandler = requireNonNull(streamHandler);
    return this;
  }

  public ConnectionBootstrap withCertificateValidator(
      final CertificateValidator certificateValidator) {
    this.certificateValidator = requireNonNull(certificateValidator);
    return this;
  }

  public Future<Connection> connect() {
    final QuicClientHandler clientHandler = channel.pipeline().get(QuicClientHandler.class);

    if (clientHandler != null) {
      final Configuration configuration = clientHandler.getConfiguration();
      final InetSocketAddress peerAddress = (InetSocketAddress) channel.remoteAddress();
      final Timer timer = clientHandler.getTimer();

      final ClientConnection connection =
          new ClientConnection(
              configuration,
              ConnectionId.random(),
              ConnectionId.random(),
              streamHandler,
              new NettyPacketSender(channel, peerAddress),
              new DefaultFlowControlHandler(
                  configuration.getInitialMaxData(), configuration.getInitialMaxStreamDataUni()),
              peerAddress,
              certificateValidator,
              timer);

      final Promise<Void> connectFuture = channel.newPromise();
      final Promise<Connection> connectionFuture = new DefaultPromise<>(channel.eventLoop());

      clientHandler.putConnection(connection);

      connectFuture.addListener(
          future -> {
            if (future.isSuccess()) {
              connectionFuture.setSuccess(connection);
            } else {
              connectionFuture.setFailure(future.cause());
            }
          });

      connection.handshake(connectFuture);

      return connectionFuture;
    } else {
      throw new IllegalStateException("Channel missing QuicClientHandler");
    }
  }
}
