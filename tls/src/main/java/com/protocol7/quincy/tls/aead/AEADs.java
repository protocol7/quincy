package com.protocol7.quincy.tls.aead;

import static java.util.Objects.requireNonNull;

import com.protocol7.quincy.tls.EncryptionLevel;
import java.util.concurrent.atomic.AtomicReference;

public class AEADs {

  private final AtomicReference<AEAD> initialAead;
  private final AtomicReference<AEAD> handshakeAead = new AtomicReference<>();
  private final AtomicReference<AEAD> oneRttAead = new AtomicReference<>();

  public AEADs(final AEAD initialAead) {
    this.initialAead = new AtomicReference<>(requireNonNull(initialAead));
  }

  public boolean available(final EncryptionLevel level) {
    if (level == EncryptionLevel.Initial) {
      return initialAead.get() != null;
    } else if (level == EncryptionLevel.Handshake) {
      return handshakeAead.get() != null;
    } else {
      return oneRttAead.get() != null;
    }
  }

  public AEAD get(final EncryptionLevel level) {
    requireNonNull(level);

    if (level == EncryptionLevel.Initial) {
      final AEAD aead = initialAead.get();
      if (aead == null) {
        throw new IllegalStateException("Initial AEAD not set");
      }

      return aead;
    } else if (level == EncryptionLevel.Handshake) {
      final AEAD aead = handshakeAead.get();
      if (aead == null) {
        throw new IllegalStateException("Handshake AEAD not set");
      }

      return aead;
    } else {
      final AEAD aead = oneRttAead.get();
      if (aead == null) {
        throw new IllegalStateException("1-RTT AEAD not set");
      }

      return aead;
    }
  }

  public void unsetInitialAead() {
    this.initialAead.set(null);
  }

  public void unsetHandshakeAead() {
    this.handshakeAead.set(null);
  }

  public void setHandshakeAead(final AEAD handshakeAead) {
    this.handshakeAead.set(requireNonNull(handshakeAead));
  }

  public void setOneRttAead(final AEAD oneRttAead) {
    this.oneRttAead.set(requireNonNull(oneRttAead));
  }
}
