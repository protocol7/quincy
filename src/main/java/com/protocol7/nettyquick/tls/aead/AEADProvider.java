package com.protocol7.nettyquick.tls.aead;

import com.protocol7.nettyquick.EncryptionLevel;

public interface AEADProvider {

  AEAD get(EncryptionLevel level);
}
