package com.protocol7.nettyquic.tls.aead;

import com.protocol7.nettyquic.tls.EncryptionLevel;

public interface AEADProvider {

  AEAD get(EncryptionLevel level);
}
