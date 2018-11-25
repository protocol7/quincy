package com.protocol7.nettyquick.protocol.frames;

import com.protocol7.nettyquick.protocol.Varint;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;

public class CryptoFrame extends Frame {

    public static CryptoFrame parse(ByteBuf bb) {
        bb.readByte(); // TODO validate

        long offset = Varint.readAsLong(bb);
        int length = Varint.readAsInt(bb);
        byte[] cryptoData = new byte[length];
        bb.readBytes(cryptoData);
        return new CryptoFrame(offset, cryptoData);
    }

    private final long offset;
    private final byte[] cryptoData;

    public CryptoFrame(long offset, byte[] cryptoData) {
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
    public void write(ByteBuf bb) {
        bb.writeByte(getType().getType());
        Varint.write(offset, bb);
        Varint.write(cryptoData.length, bb);
        bb.writeBytes(cryptoData);
    }

    @Override
    public String toString() {
        return "CryptoFrame{" +
                "offset=" + offset +
                ", cryptoData=" + Hex.hex(cryptoData) +
                '}';
    }
}
