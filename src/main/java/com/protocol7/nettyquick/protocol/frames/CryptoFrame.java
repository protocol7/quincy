package com.protocol7.nettyquick.protocol.frames;

import com.protocol7.nettyquick.protocol.Varint;
import io.netty.buffer.ByteBuf;

public class CryptoFrame extends Frame {

    public static CryptoFrame parse(ByteBuf bb) {
        bb.readByte(); // TODO validate

        Varint offset = Varint.read(bb);
        Varint length = Varint.read(bb);
        byte[] cryptoData = new byte[(int) length.getValue()];
        bb.readBytes(cryptoData);
        return new CryptoFrame(offset, cryptoData);
    }

    private final Varint offset;
    private final byte[] cryptoData;

    public CryptoFrame(int offset, byte[] cryptoData) {
        this(new Varint(offset), cryptoData);
    }

    public CryptoFrame(Varint offset, byte[] cryptoData) {
        super(FrameType.CRYPTO);
        this.offset = offset;
        this.cryptoData = cryptoData;
    }

    public Varint getOffset() {
        return offset;
    }

    public byte[] getCryptoData() {
        return cryptoData;
    }

    @Override
    public void write(ByteBuf bb) {
        bb.writeByte(getType().getType());
        offset.write(bb);
        new Varint(cryptoData.length).write(bb);
        bb.writeBytes(cryptoData);
    }
}
