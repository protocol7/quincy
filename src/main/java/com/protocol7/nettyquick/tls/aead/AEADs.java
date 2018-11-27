package com.protocol7.nettyquick.tls.aead;

import static com.google.common.base.Preconditions.checkNotNull;

import com.protocol7.nettyquick.EncryptionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AEADs {

  private final Logger log = LoggerFactory.getLogger(AEADs.class);

  private final AEAD initialAead;
  private AEAD handshakeAead;
  private AEAD oneRttAead;

  public AEADs(AEAD initialAead) {
    this.initialAead = checkNotNull(initialAead);
  }

  public AEAD get(EncryptionLevel level) {
    checkNotNull(level);

    if (level == EncryptionLevel.Initial) {
      log.debug("Using initial AEAD: {}", initialAead);
      return initialAead;
    } else if (level == EncryptionLevel.Handshake) {
      if (handshakeAead == null) {
        throw new IllegalStateException("Handshake AEAD not set");
      }

      log.debug("Using handshake AEAD: {}", handshakeAead);
      return handshakeAead;
    } else {
      if (oneRttAead == null) {
        throw new IllegalStateException("1-RTT AEAD not set");
      }

      log.debug("Using 1-RTT AEAD: {}", oneRttAead);
      return oneRttAead;
    }
  }

  public void setHandshakeAead(AEAD handshakeAead) {
    this.handshakeAead = checkNotNull(handshakeAead);
  }

  public void setOneRttAead(AEAD oneRttAead) {
    this.oneRttAead = checkNotNull(oneRttAead);
  }
}
