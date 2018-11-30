package com.protocol7.nettyquick;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.protocol7.nettyquick.client.ClientConnection;
import com.protocol7.nettyquick.client.ClientState;
import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.connection.PacketSender;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.server.ServerConnection;
import com.protocol7.nettyquick.server.ServerState;
import com.protocol7.nettyquick.streams.Stream;
import com.protocol7.nettyquick.streams.StreamListener;
import com.protocol7.nettyquick.tls.KeyUtil;
import com.protocol7.nettyquick.tls.aead.AEAD;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.SucceededFuture;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ClientServerTest {

  private static final byte[] PING = "ping".getBytes();
  private static final byte[] PONG = "pong".getBytes();

  private ClientConnection clientConnection;
  private ServerConnection serverConnection;

  private final ConnectionId destConnectionId = ConnectionId.random();
  private final ConnectionId srcConnectionId = ConnectionId.random();
  private final ForwardingPacketSender clientSender = new ForwardingPacketSender();
  private final ForwardingPacketSender serverSender = new ForwardingPacketSender();

  private @Mock InetSocketAddress serverAddress;
  private @Mock InetSocketAddress clientAddress;
  private @Mock StreamListener clientListener;
  private @Mock StreamListener serverListener;

  public static class ForwardingPacketSender implements PacketSender {

    private final DefaultEventExecutor executor = new DefaultEventExecutor();

    private Connection peer;

    public void setPeer(Connection peer) {
      this.peer = peer;
    }

    @Override
    public Future<Void> send(Packet packet, InetSocketAddress address, AEAD aead) {
      executor.execute(() -> peer.onPacket(packet));

      return new SucceededFuture(executor, null);
    }

    @Override
    public Future<Void> destroy() {
      return new SucceededFuture(executor, null);
    }
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    clientConnection =
        new ClientConnection(serverAddress, destConnectionId, clientListener, clientSender);

    List<byte[]> certificates = KeyUtil.getCertsFromCrt("src/test/resources/server.crt");
    PrivateKey privateKey = KeyUtil.getPrivateKeyFromPem("src/test/resources/server.key");

    serverConnection =
        new ServerConnection(
            clientAddress, srcConnectionId, serverListener, serverSender, certificates, privateKey);

    clientSender.setPeer(serverConnection);
    serverSender.setPeer(clientConnection);
  }

  @Test
  public void handshake() {
    clientConnection.handshake().awaitUninterruptibly();

    sleep();

    assertEquals(ClientState.Ready, clientConnection.getState());
    assertEquals(ServerState.Ready, serverConnection.getState());
  }

  @Test
  public void pingPong() {
    handshake();

    // respond pong to any ping
    doAnswer(
            invocation -> {
              Stream stream = invocation.getArgument(0);
              stream.write(PONG, true);
              return null;
            })
        .when(serverListener)
        .onData(any(), eq(PING));

    // send ping
    Stream stream = clientConnection.openStream();
    stream.write(PING, true);

    sleep();

    // verify we got pong
    verify(clientListener).onData(any(), eq(PONG));
  }

  @Test
  public void clientCloses() {
    handshake();

    clientConnection.close();

    sleep();

    assertEquals(ClientState.Closed, clientConnection.getState());
    assertEquals(ServerState.Closed, serverConnection.getState());
  }

  @Test
  public void serverCloses() {
    handshake();

    serverConnection.close();

    sleep();

    assertEquals(ClientState.Closed, clientConnection.getState());
    assertEquals(ServerState.Closed, serverConnection.getState());
  }

  private void sleep() {
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
