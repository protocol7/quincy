package com.protocol7.nettyquick.tls.aead;

import com.protocol7.nettyquick.EncryptionLevel;
import com.protocol7.nettyquick.protocol.ConnectionId;
import java.util.Optional;

public interface AEADProvider {

  AEAD get(EncryptionLevel level);
}
