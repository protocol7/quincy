package com.protocol7.quincy.tls;

import static java.util.Objects.requireNonNull;

import com.protocol7.quincy.FrameSender;
import com.protocol7.quincy.InboundHandler;
import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.TransportError;
import com.protocol7.quincy.protocol.frames.CryptoFrame;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.frames.FrameType;
import com.protocol7.quincy.protocol.frames.HandshakeDoneFrame;
import com.protocol7.quincy.protocol.frames.PaddingFrame;
import com.protocol7.quincy.protocol.packets.HandshakePacket;
import com.protocol7.quincy.protocol.packets.InitialPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.protocol.packets.RetryPacket;
import com.protocol7.quincy.protocol.packets.ShortPacket;
import com.protocol7.quincy.tls.ClientTlsSession.CertificateInvalidException;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.InitialAEAD;
import com.protocol7.quincy.tls.extensions.TransportParameters;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.Promise;
import java.util.Optional;
import java.util.function.Consumer;

public class ClientTlsManager implements InboundHandler {

  private ClientTlsSession tlsSession;
  private Promise promise;
  private final byte[] applicationProtocols;
  private final TransportParameters transportParameters;
  private final CertificateValidator certificateValidator;
  private final ConnectionId initialConnectionId;

  public ClientTlsManager(
      final ConnectionId initialConnectionId,
      final byte[] applicationProtocols,
      final TransportParameters transportParameters,
      final CertificateValidator certificateValidator) {
    this.initialConnectionId = requireNonNull(initialConnectionId);
    this.applicationProtocols = requireNonNull(applicationProtocols);
    this.transportParameters = requireNonNull(transportParameters);
    this.certificateValidator = requireNonNull(certificateValidator);

    resetTlsSession(initialConnectionId);
  }

  public void resetTlsSession(final ConnectionId connectionId) {
    this.tlsSession =
        new ClientTlsSession(
            InitialAEAD.create(connectionId.asBytes(), true),
            applicationProtocols,
            transportParameters,
            certificateValidator);
  }

  public void handshake(
      final State state,
      final FrameSender sender,
      final Consumer<State> stateSetter,
      final Promise<Void> promise) {
    this.promise = promise;

    // send initial packet
    if (state == State.Started) {
      sendInitialPacket(sender);
      stateSetter.accept(State.BeforeHello);
    } else {
      throw new IllegalStateException("Can't handshake in state " + state);
    }
  }

  @Override
  public void onReceivePacket(final Packet packet, final PipelineContext ctx) {
    final State state = ctx.getState();

    if (state == State.BeforeHello) {
      if (packet instanceof InitialPacket) {
        final InitialPacket ip = (InitialPacket) packet;

        for (final Frame frame : ip.getPayload().getFrames()) {
          if (frame instanceof CryptoFrame) {
            final CryptoFrame cf = (CryptoFrame) frame;

            final ByteBuf cryptoData = Unpooled.wrappedBuffer(cf.getCryptoData());
            try {
              tlsSession.handleServerHello(cryptoData);
            } finally {
              cryptoData.release();
            }
            ctx.setState(State.BeforeHandshake);
          }
        }
      } else if (packet instanceof RetryPacket) {
        // reset the TLS session
        resetTlsSession(packet.getSourceConnectionId());

        // restart handshake
        sendInitialPacket(ctx);
      } else {
        throw new IllegalStateException(
            "Got packet in an unexpected state: " + state + " - " + packet);
      }
    } else if (state == State.BeforeHandshake) {
      if (packet instanceof HandshakePacket) {
        try {
          handleHandshake((HandshakePacket) packet, ctx);
        } catch (final CertificateInvalidException e) {
          ctx.closeConnection(TransportError.PROTOCOL_VIOLATION, FrameType.CRYPTO, "");
          return;
        }
      } else {
        throw new IllegalStateException(
            "Got packet in an unexpected state: " + state + " - " + packet);
      }
    } else if (state == State.BeforeDone) {
      if (packet instanceof ShortPacket) {
        final ShortPacket sp = (ShortPacket) packet;

        for (final Frame frame : sp.getPayload().getFrames()) {
          if (frame instanceof HandshakeDoneFrame) {
            tlsSession.unsetHandshakeAead();

            ctx.setState(State.Done);
            promise.setSuccess(null);
          }
        }
      }
    }

    ctx.next(packet);
  }

  private void handleHandshake(final HandshakePacket packet, final PipelineContext ctx)
      throws CertificateInvalidException {
    for (final Frame frame : packet.getPayload().getFrames()) {
      if (frame instanceof CryptoFrame) {
        final CryptoFrame cf = (CryptoFrame) frame;

        final Optional<byte[]> clientFin = tlsSession.handleHandshake(cf.getCryptoData());

        if (clientFin.isPresent()) {
          tlsSession.unsetInitialAead();

          ctx.send(EncryptionLevel.Handshake, new CryptoFrame(0, clientFin.get()));

          ctx.setState(State.BeforeDone);
        }
      }
    }
  }

  private void sendInitialPacket(final FrameSender frameSender) {
    int len = 1200;

    final CryptoFrame clientHello =
        new CryptoFrame(0, tlsSession.startHandshake(initialConnectionId.asBytes()));
    len -= clientHello.calculateLength();
    frameSender.send(EncryptionLevel.Initial, clientHello, new PaddingFrame(len));
  }

  public boolean available(final EncryptionLevel encLevel) {
    return tlsSession.available(encLevel);
  }

  public AEAD getAEAD(final EncryptionLevel level) {
    return tlsSession.getAEAD(level);
  }
}
