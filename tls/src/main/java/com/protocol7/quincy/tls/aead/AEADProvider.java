package com.protocol7.quincy.tls.aead;

import com.protocol7.quincy.tls.EncryptionLevel;

public interface AEADProvider {

  AEAD get(EncryptionLevel level);
}
