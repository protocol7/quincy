package com.protocol7.nettyquick.tls;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ServerHello extends TlsMessage {

    public static void parse(byte[] sh) {
        ByteBuf bb = Unpooled.wrappedBuffer(sh);

        int messageType = bb.readByte(); // server hello
        if (messageType != 0x02) {
            throw new IllegalArgumentException("Not a server hello");
        }
        int payloadLength = read24(bb);

        //assertEquals(bb.readableBytes(), payloadLength);

        bb.readShort(); // version

        byte[] b = new byte[32];
        bb.readBytes(b); // server random

        int sessionIdLen = bb.readByte();
        b = new byte[sessionIdLen];
        bb.readBytes(b); // session ID

        b = new byte[2];
        bb.readBytes(b); // cipher suite

        int compressionMethod = bb.readByte();

        int extensionLen = bb.readShort();
        Extension.parseAll(bb.readBytes(extensionLen));
    }

}
