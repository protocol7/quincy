package com.protocol7.quincy.protocol.frames;

import com.protocol7.quincy.Varint;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

/*
   0                   1                   2                   3
   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  |           Error Code (16)     |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  |                         Frame Type (i)                      ...
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  |                    Reason Phrase Length (i)                 ...
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  |                        Reason Phrase (*)                    ...
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
*/
public class ConnectionCloseFrame extends Frame {

  public static ConnectionCloseFrame parse(final ByteBuf bb) {
    final byte type = bb.readByte();
    if (type != 0x1c) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    final int errorCode = bb.readShort();
    final FrameType frameType = FrameType.fromByte(Varint.readAsByte(bb));

    final int reasonPhraseLength = Varint.readAsInt(bb);

    final byte[] reasonPhraseBytes = new byte[reasonPhraseLength];
    bb.readBytes(reasonPhraseBytes);

    return new ConnectionCloseFrame(
        errorCode, frameType, new String(reasonPhraseBytes, StandardCharsets.UTF_8));
  }

  private final int errorCode;
  private final FrameType frameType;
  private final String reasonPhrase;

  public ConnectionCloseFrame(
      final int errorCode, final FrameType frameType, final String reasonPhrase) {
    super(FrameType.CONNECTION_CLOSE);
    this.errorCode = errorCode;
    this.frameType = frameType;
    this.reasonPhrase = reasonPhrase;
  }

  public int getErrorCode() {
    return errorCode;
  }

  public FrameType getFrameType() {
    return frameType;
  }

  public String getReasonPhrase() {
    return reasonPhrase;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(0x1c);

    bb.writeShort(errorCode);
    Varint.write(frameType.getType(), bb);

    byte[] reasonPhraseBytes = reasonPhrase.getBytes(StandardCharsets.UTF_8);

    Varint.write(reasonPhraseBytes.length, bb);
    bb.writeBytes(reasonPhraseBytes);
  }

  @Override
  public String toString() {
    return "ConnectionCloseFrame{"
        + ", errorCode="
        + errorCode
        + ", frameType="
        + frameType
        + ", reasonPhrase='"
        + reasonPhrase
        + '\''
        + '}';
  }
}
