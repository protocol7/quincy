package com.protocol7.quincy.tls.messages;

import static com.protocol7.quincy.tls.messages.MessageType.ENCRYPTED_EXTENSIONS;

import com.protocol7.quincy.Writeable;
import com.protocol7.quincy.tls.extensions.Extension;
import com.protocol7.quincy.utils.Bytes;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;

public class EncryptedExtensions implements Message, Writeable {

  private static final MessageType TYPE = ENCRYPTED_EXTENSIONS;

  public static EncryptedExtensions parse(final ByteBuf bb, final boolean isClient) {
    // EE
    final int eeType = bb.readByte();
    if (eeType != TYPE.getType()) {
      throw new IllegalArgumentException("Invalid EE type: " + eeType);
    }

    final int eeMsgLen = Bytes.read24(bb);
    final int extLen = bb.readShort();

    final ByteBuf ext = bb.readBytes(extLen);
    try {
      final List<Extension> extensions = Extension.parseAll(ext, isClient);

      return new EncryptedExtensions(extensions);
    } finally {
      ext.release();
    }
  }

  private final List<Extension> extensions;

  public EncryptedExtensions(final List<Extension> extensions) {
    this.extensions = extensions;
  }

  public EncryptedExtensions(final Extension... extensions) {
    this.extensions = Arrays.asList(extensions);
  }

  public List<Extension> getExtensions() {
    return extensions;
  }

  public void write(final ByteBuf bb) {
    // EE
    bb.writeByte(TYPE.getType());
    final int eeMsgLenPos = bb.writerIndex();
    Bytes.write24(bb, 0);

    final int extLenPos = bb.writerIndex();
    bb.writeShort(0);

    Extension.writeAll(extensions, bb, false);

    Bytes.set24(bb, eeMsgLenPos, bb.writerIndex() - eeMsgLenPos - 3);
    bb.setShort(extLenPos, bb.writerIndex() - extLenPos - 2);
  }

  @Override
  public MessageType getType() {
    return TYPE;
  }
}
