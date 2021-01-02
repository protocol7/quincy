package com.protocol7.quincy.tls.messages;

import com.protocol7.quincy.Writeable;
import com.protocol7.quincy.utils.Bytes;
import io.netty.buffer.ByteBuf;

public class ServerHandshakeFinished implements Writeable {

  public static ServerHandshakeFinished parse(final ByteBuf bb) {
    // server handshake finished
    final int finType = bb.readByte();
    if (finType != 0x14) {
      throw new IllegalArgumentException("Invalid fin type: " + finType);
    }

    final int finLen = Bytes.read24(bb);

    final byte[] verifyData = new byte[finLen];
    bb.readBytes(verifyData);

    return new ServerHandshakeFinished(verifyData);
  }

  private final byte[] verificationData;

  public ServerHandshakeFinished(final byte[] verificationData) {
    this.verificationData = verificationData;
  }

  public byte[] getVerificationData() {
    return verificationData;
  }

  public void write(final ByteBuf bb) {
    // server handshake finished
    bb.writeByte(0x14);
    Bytes.write24(bb, verificationData.length);
    bb.writeBytes(verificationData);
  }
}
