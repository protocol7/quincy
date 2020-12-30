package com.protocol7.quincy.tls;

import com.protocol7.quincy.InboundHandler;
import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.frames.AckFrame;
import com.protocol7.quincy.protocol.frames.CryptoFrame;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.frames.HandshakeDoneFrame;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.protocol.packets.HandshakePacket;
import com.protocol7.quincy.protocol.packets.InitialPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.reliability.AckUtil;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.InitialAEAD;
import com.protocol7.quincy.tls.extensions.TransportParameters;
import java.security.PrivateKey;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ServerTLSManager implements InboundHandler {

  private final ServerTlsSession tlsSession;
  private final AtomicLong donePacketNumber = new AtomicLong(-1);

  public ServerTLSManager(
      final ConnectionId connectionId,
      final TransportParameters transportParameters,
      final PrivateKey privateKey,
      final List<byte[]> certificates) {
    this.tlsSession =
        new ServerTlsSession(
            InitialAEAD.create(connectionId.asBytes(), false),
            transportParameters,
            certificates,
            privateKey);
  }

  @Override
  public void onReceivePacket(final Packet packet, final PipelineContext ctx) {

    // TODO check version
    final State state = ctx.getState();
    if (state == State.Started) {
      if (packet instanceof InitialPacket) {
        final InitialPacket initialPacket = (InitialPacket) packet;

        final CryptoFrame cf = (CryptoFrame) initialPacket.getPayload().getFrames().get(0);

        final ServerTlsSession.ServerHelloAndHandshake shah =
            tlsSession.handleClientHello(cf.getCryptoData());

        // sent as initial packet
        // TODO ack?
        ctx.send(EncryptionLevel.Initial, new CryptoFrame(0, shah.getServerHello()));

        tlsSession.setHandshakeAead(shah.getHandshakeAEAD());

        // sent as handshake packet
        ctx.send(EncryptionLevel.Handshake, new CryptoFrame(0, shah.getServerHandshake()));

        tlsSession.setOneRttAead(shah.getOneRttAEAD());

        ctx.setState(State.BeforeHandshake);
      } else {
        throw new IllegalStateException("Unexpected packet in BeforeInitial: " + packet);
      }
    } else if (state == State.BeforeHandshake && packet instanceof HandshakePacket) {
      final HandshakePacket fp = (HandshakePacket) packet;
      final CryptoFrame cryptoFrame = (CryptoFrame) fp.getPayload().getFrames().get(0);
      tlsSession.handleClientFinished(cryptoFrame.getCryptoData());

      final FullPacket donePacket = ctx.send(EncryptionLevel.OneRtt, HandshakeDoneFrame.INSTANCE);
      donePacketNumber.set(donePacket.getPacketNumber());

      ctx.setState(State.BeforeDone);
    } else if (state == State.BeforeDone && packet instanceof FullPacket) {
      final FullPacket fp = (FullPacket) packet;

      // check if package contains an ack for the donePacketNumber
      boolean acked = false;
      for (final Frame frame : fp.getPayload().getFrames()) {
        if (frame instanceof AckFrame) {
          if (AckUtil.contains((AckFrame) frame, donePacketNumber.get())) {
            acked = true;
            break;
          }
        }
      }

      if (acked) {
        tlsSession.unsetInitialAead();
        tlsSession.unsetHandshakeAead();

        ctx.setState(State.Done);
      }
    }

    ctx.next(packet);
  }

  public AEAD getAEAD(final EncryptionLevel level) {
    return tlsSession.getAEAD(level);
  }

  public boolean available(final EncryptionLevel level) {
    return tlsSession.available(level);
  }
}
