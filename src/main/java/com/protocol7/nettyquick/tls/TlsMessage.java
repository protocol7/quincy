package com.protocol7.nettyquick.tls;

import io.netty.buffer.ByteBuf;

public abstract class TlsMessage {

    protected static int read24(ByteBuf bb) {
        byte[] b = new byte[3];
        bb.readBytes(b);
        return (b[0] & 0xFF) << 16 | (b[1] & 0xFF) << 8 | (b[2] & 0xFF);
    }
}
