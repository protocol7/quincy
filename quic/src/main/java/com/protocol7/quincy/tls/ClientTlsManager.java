package com.protocol7.quincy.tls;

import com.protocol7.quincy.FrameSender;
import com.protocol7.quincy.InboundHandler;
import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.TransportError;
import com.protocol7.quincy.protocol.frames.CryptoFrame;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.frames.FrameType;
import com.protocol7.quincy.protocol.frames.PaddingFrame;
import com.protocol7.quincy.protocol.packets.HandshakePacket;
import com.protocol7.quincy.protocol.packets.InitialPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.protocol.packets.RetryPacket;
import com.protocol7.quincy.tls.ClientTlsSession.CertificateInvalidException;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.InitialAEAD;
import com.protocol7.quincy.tls.extensions.TransportParameters;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.util.Optional;
import java.util.function.Consumer;

public class ClientTlsManager implements InboundHandler {

  private ClientTlsSession tlsSession;
  private final DefaultPromise<Void> handshakeFuture =
      new DefaultPromise(GlobalEventExecutor.INSTANCE); // TODO use what event executor?
  private final TransportParameters transportParameters;
  private final CertificateValidator certificateValidator;

  public ClientTlsManager(
      final ConnectionId connectionId,
      final TransportParameters transportParameters,
      final CertificateValidator certificateValidator) {
    this.transportParameters = transportParameters;
    this.certificateValidator = certificateValidator;

    resetTlsSession(connectionId);
  }

  public void resetTlsSession(final ConnectionId connectionId) {
    this.tlsSession =
        new ClientTlsSession(
            InitialAEAD.create(connectionId.asBytes(), true),
            transportParameters,
            certificateValidator);
  }

  public Future<Void> handshake(
      final State state, final FrameSender sender, final Consumer<State> stateSetter) {
    // send initial packet
    if (state == State.Started) {
      sendInitialPacket(sender);
      stateSetter.accept(State.BeforeHello);
    } else {
      throw new IllegalStateException("Can't handshake in state " + state);
    }
    return handshakeFuture;
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

            final AEAD handshakeAead = tlsSession.handleServerHello(cf.getCryptoData());
            tlsSession.setHandshakeAead(handshakeAead);
            ctx.setState(State.BeforeHandshake);
          }
        }
      } else if (packet instanceof RetryPacket) {
        // reset the TLS session
        resetTlsSession(packet.getSourceConnectionId().get());

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
    }

    ctx.next(packet);
  }

  private void handleHandshake(final HandshakePacket packet, final PipelineContext ctx)
      throws CertificateInvalidException {
    for (final Frame frame : packet.getPayload().getFrames()) {
      if (frame instanceof CryptoFrame) {
        final CryptoFrame cf = (CryptoFrame) frame;

        final Optional<ClientTlsSession.HandshakeResult> result =
            tlsSession.handleHandshake(cf.getCryptoData());

        if (result.isPresent()) {
          tlsSession.unsetInitialAead();

          ctx.send(new CryptoFrame(0, result.get().getFin()));

          tlsSession.setOneRttAead(result.get().getOneRttAead());

          tlsSession.unsetHandshakeAead();
          ctx.setState(State.Ready);
          handshakeFuture.setSuccess(null);
        }
      }
    }
  }

  private void sendInitialPacket(final FrameSender frameSender) {
    int len = 1200;

    final CryptoFrame clientHello = new CryptoFrame(0, tlsSession.startHandshake());
    len -= clientHello.calculateLength();

    frameSender.send(clientHello, new PaddingFrame(len));
  }

  public boolean available(final EncryptionLevel encLevel) {
    return tlsSession.available(encLevel);
  }

  public AEAD getAEAD(final EncryptionLevel level) {
    return tlsSession.getAEAD(level);
  }
}
