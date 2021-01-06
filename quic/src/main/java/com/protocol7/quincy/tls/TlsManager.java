package com.protocol7.quincy.tls;

import com.protocol7.quincy.FrameSender;
import com.protocol7.quincy.InboundHandler;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.tls.aead.AEAD;
import io.netty.util.concurrent.Promise;
import java.util.function.Consumer;

public interface TlsManager extends InboundHandler {

  boolean available(final EncryptionLevel encLevel);

  AEAD getAEAD(final EncryptionLevel level);

  void resetTlsSession(final ConnectionId connectionId);

  void handshake(
      final State state,
      final FrameSender sender,
      final Consumer<State> stateSetter,
      final Promise<Void> promise);
}
