package com.protocol7.nettyquick.protocol.frames;

import com.google.common.base.Charsets;
import com.protocol7.nettyquick.protocol.Varint;
import io.netty.buffer.ByteBuf;

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

  public static ConnectionCloseFrame parse(ByteBuf bb) {
    byte type = bb.readByte();
    if (type != 0x02) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    int errorCode = bb.readShort();
    int frameType = Varint.readAsInt(bb);

    int reasonPhraseLength = Varint.readAsInt(bb);

    byte[] reasonPhraseBytes = new byte[reasonPhraseLength];
    bb.readBytes(reasonPhraseBytes);

    return new ConnectionCloseFrame(
        errorCode, frameType, new String(reasonPhraseBytes, Charsets.UTF_8));
  }

  private final int errorCode;
  private final int frameType;
  private final String reasonPhrase;

  public ConnectionCloseFrame(int errorCode, int frameType, String reasonPhrase) {
    super(FrameType.CONNECTION_CLOSE);
    this.errorCode = errorCode;
    this.frameType = frameType;
    this.reasonPhrase = reasonPhrase;
  }

  public int getErrorCode() {
    return errorCode;
  }

  public int getFrameType() {
    return frameType;
  }

  public String getReasonPhrase() {
    return reasonPhrase;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(getType().getType());

    bb.writeShort(errorCode);
    Varint.write(frameType, bb);

    byte[] reasonPhraseBytes = reasonPhrase.getBytes(Charsets.UTF_8);

    Varint.write(reasonPhraseBytes.length, bb);
    bb.writeBytes(reasonPhraseBytes);
  }
}
