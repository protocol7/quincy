package com.protocol7.quincy.tls.messages;

import static com.protocol7.quincy.tls.messages.MessageType.SERVER_CERTIFICATE_VERIFY;

import com.protocol7.quincy.Writeable;
import com.protocol7.quincy.utils.Bytes;
import io.netty.buffer.ByteBuf;

public class ServerCertificateVerify implements Message, Writeable {

  private static final MessageType TYPE = SERVER_CERTIFICATE_VERIFY;

  public static ServerCertificateVerify parse(final ByteBuf bb) {
    // server cert verify
    final int serverCertVerifyType = bb.readByte();
    if (serverCertVerifyType != TYPE.getType()) {
      throw new IllegalArgumentException(
          "Invalid server cert verify type: " + serverCertVerifyType);
    }

    final int scvMsgLen = Bytes.read24(bb);

    final int signType = bb.readShort();
    final int signLen = bb.readShort();

    final byte[] sign = new byte[signLen];
    bb.readBytes(sign);

    return new ServerCertificateVerify(signType, sign);
  }

  private final int type;
  private final byte[] signature;

  public ServerCertificateVerify(final int type, final byte[] signature) {
    this.type = type;
    this.signature = signature;
  }

  public int getVerifyType() {
    return type;
  }

  public byte[] getSignature() {
    return signature;
  }

  public void write(final ByteBuf bb) {
    // server cert verify
    bb.writeByte(TYPE.getType());

    final int scvMsgLenPos = bb.writerIndex();
    Bytes.write24(bb, 0);

    bb.writeShort(type);
    bb.writeShort(signature.length);
    bb.writeBytes(signature);

    Bytes.set24(bb, scvMsgLenPos, bb.writerIndex() - scvMsgLenPos - 3);
  }

  @Override
  public MessageType getType() {
    return TYPE;
  }
}
