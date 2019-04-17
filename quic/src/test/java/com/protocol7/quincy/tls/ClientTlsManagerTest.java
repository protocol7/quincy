package com.protocol7.quincy.tls;

import static java.util.Optional.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.FrameSender;
import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.frames.CryptoFrame;
import com.protocol7.quincy.protocol.frames.PaddingFrame;
import com.protocol7.quincy.protocol.packets.HandshakePacket;
import com.protocol7.quincy.protocol.packets.InitialPacket;
import com.protocol7.quincy.tls.ServerTlsSession.ServerHelloAndHandshake;
import com.protocol7.quincy.tls.aead.InitialAEAD;
import com.protocol7.quincy.tls.extensions.TransportParameters;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ClientTlsManagerTest {

  private ConnectionId connectionId = ConnectionId.random();
  private TransportParameters tps = Configuration.defaults().toTransportParameters();
  private ClientTlsManager manager = new ClientTlsManager(connectionId, tps);

  private ServerTlsSession serverTlsSession =
      new ServerTlsSession(
          InitialAEAD.create(connectionId.asBytes(), false),
          tps,
          KeyUtil.getCertsFromCrt("src/test/resources/server.crt"),
          KeyUtil.getPrivateKey("src/test/resources/server.der"));

  @Test
  public void handshake() {
    // start handshake
    PipelineContext ctx = mock(PipelineContext.class);
    FrameSender sender = mock(FrameSender.class);
    Consumer<State> stateSetter = mock(Consumer.class);
    manager.handshake(State.Started, sender, stateSetter);

    ArgumentCaptor<CryptoFrame> chFrame = ArgumentCaptor.forClass(CryptoFrame.class);

    verify(sender).send(chFrame.capture(), any(PaddingFrame.class));
    verify(stateSetter).accept(State.BeforeHello);

    byte[] ch = chFrame.getValue().getCryptoData();

    ServerHelloAndHandshake shah = serverTlsSession.handleClientHello(ch);

    // receive server hello
    InitialPacket shPacket = ip(shah.getServerHello());
    when(ctx.getState()).thenReturn(State.BeforeHello);
    manager.onReceivePacket(shPacket, ctx);

    verify(ctx, never()).send(any(CryptoFrame.class));
    verify(ctx).setState(State.BeforeHandshake);
    verify(ctx).next(shPacket);

    // receive server handshake
    HandshakePacket handshakePacket = hp(shah.getServerHandshake());
    when(ctx.getState()).thenReturn(State.BeforeHandshake);
    manager.onReceivePacket(handshakePacket, ctx);

    verify(ctx).send(any(CryptoFrame.class));
    verify(ctx).setState(State.Ready);
    verify(ctx).next(handshakePacket);

    // and we're done
  }

  private InitialPacket ip(final byte[] b) {
    return InitialPacket.create(
        empty(), empty(), PacketNumber.MIN, Version.DRAFT_18, empty(), new CryptoFrame(0, b));
  }

  private HandshakePacket hp(final byte[] b) {
    return HandshakePacket.create(
        empty(), empty(), PacketNumber.MIN, Version.DRAFT_18, new CryptoFrame(0, b));
  }
}
