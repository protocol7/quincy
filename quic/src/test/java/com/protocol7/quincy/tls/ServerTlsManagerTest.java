package com.protocol7.quincy.tls;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.netty.QuicBuilder;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.frames.AckFrame;
import com.protocol7.quincy.protocol.frames.AckRange;
import com.protocol7.quincy.protocol.frames.CryptoFrame;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.frames.HandshakeDoneFrame;
import com.protocol7.quincy.protocol.packets.HandshakePacket;
import com.protocol7.quincy.protocol.packets.InitialPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.protocol.packets.ShortPacket;
import com.protocol7.quincy.tls.ClientTlsSession.CertificateInvalidException;
import com.protocol7.quincy.tls.aead.InitialAEAD;
import com.protocol7.quincy.tls.extensions.TransportParameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ServerTlsManagerTest {

  private final ConnectionId connectionId = ConnectionId.random();
  private final TransportParameters tps = new QuicBuilder().configuration().toTransportParameters();
  private final ServerTLSManager manager =
      new ServerTLSManager(
          connectionId,
          tps,
          KeyUtil.getPrivateKey("src/test/resources/server.der"),
          KeyUtil.getCertsFromCrt("src/test/resources/server.crt"));

  private final ClientTlsSession clientTlsSession =
      new ClientTlsSession(
          InitialAEAD.create(connectionId.asBytes(), true), tps, new NoopCertificateValidator());

  @Test
  public void handshake() throws CertificateInvalidException {
    // start handshake
    final byte[] ch = clientTlsSession.startHandshake(connectionId.asBytes());

    final PipelineContext ctx = mock(PipelineContext.class);

    when(ctx.getState()).thenReturn(State.Started);
    final Packet chPacket = ip(ch);
    manager.onReceivePacket(chPacket, ctx);

    // receive server hello
    final ArgumentCaptor<CryptoFrame> cfCaptor = ArgumentCaptor.forClass(CryptoFrame.class);
    verify(ctx, times(2)).send(any(EncryptionLevel.class), cfCaptor.capture());
    verify(ctx).setState(State.BeforeHandshake);
    verify(ctx).next(chPacket);

    clientTlsSession.handleServerHello(cfCaptor.getAllValues().get(0).getCryptoData());
    final ClientTlsSession.HandshakeResult hr =
        clientTlsSession.handleHandshake(cfCaptor.getAllValues().get(1).getCryptoData()).get();

    // receive fin, should send handshake done
    when(ctx.getState()).thenReturn(State.BeforeHandshake);
    final Packet finPacket = hp(hr.getFin());
    final ShortPacket donePacket = sp(HandshakeDoneFrame.INSTANCE);
    when(ctx.send(eq(EncryptionLevel.OneRtt), any(Frame.class))).thenReturn(donePacket);

    manager.onReceivePacket(finPacket, ctx);

    verify(ctx, times(2))
        .send(any(EncryptionLevel.class), any(CryptoFrame.class)); // no more interactions
    verify(ctx).setState(State.BeforeDone);
    verify(ctx).next(finPacket);

    // receive ack of handshake done
    when(ctx.getState()).thenReturn(State.BeforeDone);
    final ShortPacket ack =
        sp(
            new AckFrame(
                123, new AckRange(donePacket.getPacketNumber(), donePacket.getPacketNumber())));
    manager.onReceivePacket(ack, ctx);
    verify(ctx).setState(State.Done);
    verify(ctx).next(finPacket);

    // and we're done
  }

  private InitialPacket ip(final byte[] b) {
    return InitialPacket.create(
        empty(),
        empty(),
        PacketNumber.MIN,
        Version.DRAFT_29,
        of("some token".getBytes()),
        new CryptoFrame(0, b));
  }

  private HandshakePacket hp(final byte[] b) {
    return HandshakePacket.create(
        empty(), empty(), PacketNumber.MIN, Version.DRAFT_29, new CryptoFrame(0, b));
  }

  private ShortPacket sp(final Frame... frames) {
    return ShortPacket.create(false, empty(), PacketNumber.MIN, frames);
  }
}
