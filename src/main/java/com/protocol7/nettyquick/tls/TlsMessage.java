package com.protocol7.nettyquick.tls;

import io.netty.buffer.ByteBuf;

public abstract class TlsMessage {

    protected static int read24(ByteBuf bb) {
        byte[] b = new byte[3];
        bb.readBytes(b);
        return (b[0] & 0xFF) << 16 | (b[1] & 0xFF) << 8 | (b[2] & 0xFF);
    }

    protected static void write24(ByteBuf bb, int value) {
        bb.writeByte((value >> 16) & 0xFF);
        bb.writeByte((value >> 8)  & 0xFF);
        bb.writeByte(value & 0xFF);
    }

    protected static void write24(ByteBuf bb, int value, int position) {
        bb.setByte(position, (value >> 16) & 0xFF);
        bb.setByte(position + 1, (value >> 8)  & 0xFF);
        bb.setByte(position + 2, value & 0xFF);
    }
}
