package com.protocol7.nettyquic.protocol.frames;

import com.protocol7.nettyquic.Varint;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

public class ApplicationCloseFrame extends Frame {

  public static ApplicationCloseFrame parse(final ByteBuf bb) {
    final byte type = bb.readByte();
    if (type != 0x1d) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    final int errorCode = bb.readShort();

    final int reasonPhraseLength = Varint.readAsInt(bb);

    final byte[] reasonPhraseBytes = new byte[reasonPhraseLength];
    bb.readBytes(reasonPhraseBytes);

    return new ApplicationCloseFrame(
        errorCode, new String(reasonPhraseBytes, StandardCharsets.UTF_8));
  }

  private final int errorCode;
  private final String reasonPhrase;

  public ApplicationCloseFrame(final int errorCode, final String reasonPhrase) {
    super(FrameType.CONNECTION_CLOSE);
    this.errorCode = errorCode;
    this.reasonPhrase = reasonPhrase;
  }

  public int getErrorCode() {
    return errorCode;
  }

  public String getReasonPhrase() {
    return reasonPhrase;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(0x1d);

    bb.writeShort(errorCode);

    byte[] reasonPhraseBytes = reasonPhrase.getBytes(StandardCharsets.UTF_8);

    Varint.write(reasonPhraseBytes.length, bb);
    bb.writeBytes(reasonPhraseBytes);
  }

  @Override
  public String toString() {
    return "ConnectionCloseFrame{"
        + ", errorCode="
        + errorCode
        + ", reasonPhrase='"
        + reasonPhrase
        + '\''
        + '}';
  }
}
