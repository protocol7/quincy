package com.protocol7.quincy;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.protocol7.quincy.connection.AbstractConnection;
import com.protocol7.quincy.connection.ClientConnection;
import com.protocol7.quincy.connection.Connection;
import com.protocol7.quincy.connection.PacketSender;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.flowcontrol.DefaultFlowControlHandler;
import com.protocol7.quincy.flowcontrol.FlowControlHandler;
import com.protocol7.quincy.netty.QuicBuilder;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.streams.DefaultStream;
import com.protocol7.quincy.streams.Stream;
import com.protocol7.quincy.streams.StreamHandler;
import com.protocol7.quincy.tls.KeyUtil;
import com.protocol7.quincy.tls.NoopCertificateValidator;
import com.protocol7.quincy.tls.aead.AEAD;
import io.netty.util.Timer;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.SucceededFuture;
import java.security.PrivateKey;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ClientServerConnectionTest {

  private static final byte[] PING = "ping".getBytes();
  private static final byte[] PONG = "pong".getBytes();

  private ClientConnection clientConnection;
  private Connection serverConnection;

  private final ConnectionId destConnectionId = ConnectionId.random();
  private final ConnectionId srcConnectionId = ConnectionId.random();
  private final ForwardingPacketSender clientSender = new ForwardingPacketSender();
  private final ForwardingPacketSender serverSender = new ForwardingPacketSender();

  private @Mock StreamHandler clientListener;
  private @Mock StreamHandler serverListener;
  private @Mock Timer scheduler;
  private final FlowControlHandler flowControlHandler = new DefaultFlowControlHandler(1000, 1000);

  public static class ForwardingPacketSender implements PacketSender {

    private final DefaultEventExecutor executor = new DefaultEventExecutor();

    private Connection peer;

    public void setPeer(final Connection peer) {
      this.peer = peer;
    }

    @Override
    public Future<Void> send(final Packet packet, final AEAD aead) {
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
    clientConnection =
        new ClientConnection(
            new QuicBuilder().withApplicationProtocols("http/0.9").configuration(),
            destConnectionId,
            srcConnectionId,
            clientListener,
            clientSender,
            flowControlHandler,
            TestUtil.getTestAddress(),
            new NoopCertificateValidator(),
            scheduler);

    final List<byte[]> certificates = KeyUtil.getCertsFromCrt("src/test/resources/server.crt");
    final PrivateKey privateKey = KeyUtil.getPrivateKey("src/test/resources/server.der");

    serverConnection =
        AbstractConnection.forServer(
            new QuicBuilder().withApplicationProtocols("http/0.9").configuration(),
            srcConnectionId,
            destConnectionId,
            destConnectionId,
            serverListener,
            serverSender,
            certificates,
            privateKey,
            flowControlHandler,
            TestUtil.getTestAddress(),
            scheduler);

    clientSender.setPeer(serverConnection);
    serverSender.setPeer(clientConnection);
  }

  @Test
  public void handshake() {
    final DefaultPromise<Void> handshakeFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);

    clientConnection.handshake(handshakeFuture);
    handshakeFuture.awaitUninterruptibly();

    sleep();

    assertEquals(State.Done, clientConnection.getState());
    assertEquals(State.Done, serverConnection.getState());
  }

  @Test
  public void pingPong() {
    handshake();

    // respond pong to any ping
    doAnswer(
            invocation -> {
              DefaultStream stream = invocation.getArgument(0);
              stream.write(PONG, true);
              return null;
            })
        .when(serverListener)
        .onData(any(), eq(PING), eq(true));

    // send ping
    final Stream stream = clientConnection.openStream();
    stream.write(PING, true);

    sleep();

    // verify we got pong
    verify(clientListener).onData(any(), eq(PONG), eq(true));
  }

  @Test
  public void sirStreamAlot() {
    handshake();

    // This is a somewhat contrived test, but it send a bunch of messages from a client to a server
    // and verifies that they all arrive in order. The amount of data sent is meant to require flow
    // control.

    final Stream stream = clientConnection.openStream();

    for (int i = 0; i < 100; i++) {
      stream.write(b(i), i == 99);
    }

    // wait until all messages have arrived
    ArgumentCaptor<byte[]> captor = null;
    for (int i = 0; i < 10; i++) {
      captor = ArgumentCaptor.forClass(byte[].class);
      verify(serverListener, atLeast(0))
          .onData(any(Stream.class), captor.capture(), any(Boolean.class));

      if (captor.getAllValues().size() >= 100) {
        break;
      }
      sleep();
    }

    if (captor != null) {
      assertEquals(100, captor.getAllValues().size());

      for (int i = 0; i < 100; i++) {
        final byte[] value = captor.getAllValues().get(i);
        assertArrayEquals(b(i), value);
      }
    } else {
      fail("Timed out");
    }
  }

  private byte[] b(final int i) {
    final byte[] b = new byte[1000];
    b[0] = (byte) i;
    return b;
  }

  @Test
  public void clientCloses() {
    handshake();

    clientConnection.close();

    sleep();

    assertEquals(State.Closed, clientConnection.getState());
    assertEquals(State.Closed, serverConnection.getState());
  }

  @Test
  public void serverCloses() {
    handshake();

    serverConnection.close();

    sleep();

    assertEquals(State.Closed, clientConnection.getState());
    assertEquals(State.Closed, serverConnection.getState());
  }

  private void sleep() {
    try {
      Thread.sleep(200);
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
