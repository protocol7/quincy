package com.protocol7.nettyquic.tls.extensions;

import com.protocol7.nettyquic.utils.Bytes;
import com.protocol7.nettyquic.utils.Hex;
import io.netty.buffer.ByteBuf;

public class RawExtension implements Extension {

  public static RawExtension parse(ExtensionType type, ByteBuf bb) {
    byte[] b = Bytes.peekToArray(bb);
    return new RawExtension(type, b);
  }

  private final ExtensionType type;
  private final byte[] data;

  public RawExtension(ExtensionType type, byte[] data) {
    this.type = type;
    this.data = data;
  }

  @Override
  public ExtensionType getType() {
    return type;
  }

  public byte[] getData() {
    return data;
  }

  @Override
  public void write(ByteBuf bb, boolean isClient) {
    bb.writeBytes(data);
  }

  @Override
  public String toString() {
    return "RawExtension{" + "type=" + type + ", data=" + Hex.hex(data) + '}';
  }
}
