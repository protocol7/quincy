package com.protocol7.quincy.tls.messages;

import static com.protocol7.quincy.tls.messages.MessageType.FINISHED;

import com.protocol7.quincy.Writeable;
import com.protocol7.quincy.tls.VerifyData;
import com.protocol7.quincy.utils.Bytes;
import io.netty.buffer.ByteBuf;

public class Finished implements Message, Writeable {

  private static final MessageType TYPE = FINISHED;

  public static Finished createClientFinished(
      final byte[] clientHandshakeTrafficSecret, final byte[] finHash) {
    final byte[] verifyData = VerifyData.create(clientHandshakeTrafficSecret, finHash);

    return new Finished(verifyData);
  }

  public static Finished parse(final ByteBuf bb) {
    // server handshake finished
    final int finType = bb.readByte();
    if (finType != TYPE.getType()) {
      throw new IllegalArgumentException("Invalid fin type: " + finType);
    }

    final int finLen = Bytes.read24(bb);

    final byte[] verifyData = new byte[finLen];
    bb.readBytes(verifyData);

    return new Finished(verifyData);
  }

  private final byte[] verificationData;

  public Finished(final byte[] verificationData) {
    this.verificationData = verificationData;
  }

  public byte[] getVerificationData() {
    return verificationData;
  }

  public void write(final ByteBuf bb) {
    // server handshake finished
    bb.writeByte(TYPE.getType());
    Bytes.write24(bb, verificationData.length);
    bb.writeBytes(verificationData);
  }

  @Override
  public MessageType getType() {
    return TYPE;
  }
}
