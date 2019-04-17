package com.protocol7.quincy.protocol.frames;

import com.protocol7.quincy.Varint;
import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;

public class CryptoFrame extends Frame {

  public static CryptoFrame parse(final ByteBuf bb) {
    final byte type = bb.readByte();
    if (type != FrameType.CRYPTO.getType()) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    final long offset = Varint.readAsLong(bb);
    final int length = Varint.readAsInt(bb);
    final byte[] cryptoData = new byte[length];
    bb.readBytes(cryptoData);
    return new CryptoFrame(offset, cryptoData);
  }

  private final long offset;
  private final byte[] cryptoData;

  public CryptoFrame(final long offset, final byte[] cryptoData) {
    super(FrameType.CRYPTO);
    this.offset = offset;
    this.cryptoData = cryptoData;
  }

  public long getOffset() {
    return offset;
  }

  public byte[] getCryptoData() {
    return cryptoData;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(getType().getType());
    Varint.write(offset, bb);
    Varint.write(cryptoData.length, bb);
    bb.writeBytes(cryptoData);
  }

  @Override
  public String toString() {
    return "CryptoFrame{" + "offset=" + offset + ", cryptoData=" + Hex.hex(cryptoData) + '}';
  }
}
