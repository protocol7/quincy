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
   |                    Reason Phrase Length (i)                 ...
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                        Reason Phrase (*)                    ...
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ */
public class ApplicationCloseFrame extends Frame {

  public static ApplicationCloseFrame parse(ByteBuf bb) {
    byte type = bb.readByte();
    if (type != FrameType.APPLICATION_CLOSE.getType()) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    int errorCode = bb.readShort();

    int reasonPhraseLength = Varint.read(bb).intValue();

    byte[] reasonPhraseBytes = new byte[reasonPhraseLength];
    bb.readBytes(reasonPhraseBytes);

    return new ApplicationCloseFrame(errorCode, new String(reasonPhraseBytes, Charsets.UTF_8));
  }

  private final int errorCode;
  private final String reasonPhrase;

  public ApplicationCloseFrame(int errorCode, String reasonPhrase) {
    super(FrameType.APPLICATION_CLOSE);
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
    bb.writeByte(getType().getType());

    bb.writeShort(errorCode);

    byte[] reasonPhraseBytes = reasonPhrase.getBytes(Charsets.UTF_8);

    Varint.write(reasonPhraseBytes.length, bb);
    bb.writeBytes(reasonPhraseBytes);
  }
}
