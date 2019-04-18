package com.protocol7.quincy.tls.messages;

import com.protocol7.quincy.Writeable;
import com.protocol7.quincy.tls.VerifyData;
import com.protocol7.quincy.utils.Bytes;
import io.netty.buffer.ByteBuf;

public class ClientFinished implements Writeable {

  public static ClientFinished create(
      final byte[] clientHandshakeTrafficSecret, final byte[] finHash) {
    final byte[] verifyData = VerifyData.create(clientHandshakeTrafficSecret, finHash);

    return new ClientFinished(verifyData);
  }

  public static ClientFinished parse(final ByteBuf bb) {
    final int type = bb.readByte();
    if (type != 0x14) {
      throw new IllegalArgumentException("Invalid type: " + type);
    }

    final int len = Bytes.read24(bb);
    final byte[] verifyData = new byte[len];
    bb.readBytes(verifyData);

    return new ClientFinished(verifyData);
  }

  private final byte[] verificationData;

  public ClientFinished(final byte[] verificationData) {
    this.verificationData = verificationData;
  }

  public byte[] getVerificationData() {
    return verificationData;
  }

  public void write(final ByteBuf bb) {
    bb.writeByte(0x14);

    Bytes.write24(bb, verificationData.length);
    bb.writeBytes(verificationData);
  }
}
