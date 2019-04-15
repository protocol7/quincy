package com.protocol7.nettyquic.tls;

import com.protocol7.nettyquic.FrameSender;
import com.protocol7.nettyquic.InboundHandler;
import com.protocol7.nettyquic.PipelineContext;
import com.protocol7.nettyquic.connection.State;
import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.frames.CryptoFrame;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.frames.PaddingFrame;
import com.protocol7.nettyquic.protocol.packets.HandshakePacket;
import com.protocol7.nettyquic.protocol.packets.InitialPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;
import com.protocol7.nettyquic.protocol.packets.RetryPacket;
import com.protocol7.nettyquic.tls.aead.AEAD;
import com.protocol7.nettyquic.tls.aead.InitialAEAD;
import com.protocol7.nettyquic.tls.extensions.TransportParameters;
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

  public ClientTlsManager(
      final ConnectionId connectionId, final TransportParameters transportParameters) {
    this.transportParameters = transportParameters;

    resetTlsSession(connectionId);
  }

  public void resetTlsSession(ConnectionId connectionId) {
    this.tlsSession =
        new ClientTlsSession(InitialAEAD.create(connectionId.asBytes(), true), transportParameters);
  }

  public Future<Void> handshake(State state, FrameSender sender, Consumer<State> stateSetter) {
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
        InitialPacket ip = (InitialPacket) packet;

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
        handleHandshake((HandshakePacket) packet, ctx);
      } else {
        throw new IllegalStateException(
            "Got packet in an unexpected state: " + state + " - " + packet);
      }
    }

    ctx.next(packet);
  }

  private void handleHandshake(final HandshakePacket packet, PipelineContext ctx) {
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

  private void sendInitialPacket(FrameSender frameSender) {
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
