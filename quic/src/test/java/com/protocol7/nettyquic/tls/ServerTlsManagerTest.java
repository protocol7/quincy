package com.protocol7.nettyquic.tls;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.protocol7.nettyquic.Configuration;
import com.protocol7.nettyquic.PipelineContext;
import com.protocol7.nettyquic.connection.State;
import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.Version;
import com.protocol7.nettyquic.protocol.frames.CryptoFrame;
import com.protocol7.nettyquic.protocol.packets.HandshakePacket;
import com.protocol7.nettyquic.protocol.packets.InitialPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;
import com.protocol7.nettyquic.tls.aead.InitialAEAD;
import com.protocol7.nettyquic.tls.extensions.TransportParameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ServerTlsManagerTest {

  private ConnectionId connectionId = ConnectionId.random();
  private TransportParameters tps = Configuration.defaults().toTransportParameters();
  private ServerTLSManager manager =
      new ServerTLSManager(
          connectionId,
          tps,
          KeyUtil.getPrivateKey("src/test/resources/server.der"),
          KeyUtil.getCertsFromCrt("src/test/resources/server.crt"));

  private ClientTlsSession clientTlsSession =
      new ClientTlsSession(InitialAEAD.create(connectionId.asBytes(), true), tps);

  @Test
  public void handshake() {
    // start handshake
    byte[] ch = clientTlsSession.startHandshake();

    PipelineContext ctx = mock(PipelineContext.class);

    when(ctx.getState()).thenReturn(State.Started);
    Packet chPacket = ip(ch);
    manager.onReceivePacket(chPacket, ctx);

    // receive server hello
    ArgumentCaptor<CryptoFrame> cfCaptor = ArgumentCaptor.forClass(CryptoFrame.class);
    verify(ctx, times(2)).send(cfCaptor.capture());
    verify(ctx).setState(State.BeforeReady);
    verify(ctx).next(chPacket);

    clientTlsSession.handleServerHello(cfCaptor.getAllValues().get(0).getCryptoData());
    ClientTlsSession.HandshakeResult hr =
        clientTlsSession.handleHandshake(cfCaptor.getAllValues().get(1).getCryptoData()).get();

    when(ctx.getState()).thenReturn(State.BeforeReady);
    Packet finPacket = hp(hr.getFin());
    manager.onReceivePacket(finPacket, ctx);

    verify(ctx, times(2)).send(any(CryptoFrame.class)); // no more interactions
    verify(ctx).setState(State.Ready);
    verify(ctx).next(finPacket);

    // and we're done
  }

  private InitialPacket ip(final byte[] b) {
    return InitialPacket.create(
        empty(),
        empty(),
        PacketNumber.MIN,
        Version.DRAFT_18,
        of("some token".getBytes()),
        new CryptoFrame(0, b));
  }

  private HandshakePacket hp(final byte[] b) {
    return HandshakePacket.create(
        empty(), empty(), PacketNumber.MIN, Version.DRAFT_18, new CryptoFrame(0, b));
  }
}
