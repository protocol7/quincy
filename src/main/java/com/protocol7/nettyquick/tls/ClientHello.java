package com.protocol7.nettyquick.tls;

import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Arrays;

public class ClientHello {

    public static byte[] extend(byte[] ch, TransportParameters tps) {
        ClientHello hello = parse(ch);

        ByteBuf bb = Unpooled.buffer();
        hello.writeWithTransportParameters(bb, tps);

        byte[] b = new byte[bb.writerIndex()];
        bb.readBytes(b);

        return b;
    }

    public static ClientHello parse(byte[] ch) {
        ByteBuf bb = Unpooled.wrappedBuffer(ch);

        byte handshakeType = bb.readByte();

        if (handshakeType != 0x01) {
            throw new IllegalArgumentException("Invalid handshake type");
        }

        byte[] b = new byte[3];
        bb.readBytes(b);
        int payloadLength = (b[0] & 0xFF) << 16 | (b[1] & 0xFF) << 8 | (b[2] & 0xFF);
        if (payloadLength != bb.readableBytes()) {
            throw new IllegalArgumentException("Buffer incorrect length: actual " + payloadLength + ", expected " + bb.readableBytes());
        }

        byte[] clientVersion = new byte[2];
        bb.readBytes(clientVersion);
        if (!Arrays.equals(clientVersion, new byte[]{3, 3})) {
            throw new IllegalArgumentException("Invalid client version: " + Hex.hex(clientVersion));
        }

        byte[] clientRandom = new byte[32];
        bb.readBytes(clientRandom); // client random

        int sessionIdLen = bb.readByte();
        byte[] sessionId = new byte[sessionIdLen];
        bb.readBytes(sessionId); // session ID

        int cipherSuiteLen = bb.readShort();
        byte[] cipherSuites = new byte[cipherSuiteLen];
        bb.readBytes(cipherSuites); // cipher suites

        byte[] compression = new byte[2];
        bb.readBytes(compression);
        if (!Arrays.equals(compression, new byte[]{1, 0})) {
            throw new IllegalArgumentException("Compression must be disabled: " + Hex.hex(compression));
        }

        int extensionsLen = bb.readShort();
        byte[] extensions = new byte[extensionsLen];
        bb.readBytes(extensions); // extensions

        return new ClientHello(
                clientRandom,
                sessionId,
                cipherSuites,
                extensions);
    }

    private final byte[] clientRandom;
    private final byte[] sessionId;
    private final byte[] cipherSuites;
    private final byte[] extensions;

    public ClientHello(byte[] clientRandom, byte[] sessionId, byte[] cipherSuites, byte[] extensions) {
        this.clientRandom = clientRandom;
        this.sessionId = sessionId;
        this.cipherSuites = cipherSuites;
        this.extensions = extensions;
    }

    public byte[] getClientRandom() {
        return clientRandom;
    }

    public byte[] getSessionId() {
        return sessionId;
    }

    public byte[] getCipherSuites() {
        return cipherSuites;
    }

    public byte[] getExtensions() {
        return extensions;
    }

    public void write(ByteBuf bb) {
        writeWithTransportParameters(bb, null);
    }

    public void writeWithTransportParameters(ByteBuf bb, TransportParameters tps) {
        bb.writeByte(0x01);

        int tpsLen = 0;
        if (tps != null) {
            tpsLen = 2 + 2 + tps.calculateLength();
        }

        // payload length
        int len = 2 + clientRandom.length + 1 + sessionId.length + 2 + cipherSuites.length + 2 + 2 + extensions.length + tpsLen;
        bb.writeByte((len >> 16) & 0xFF);
        bb.writeByte((len >> 8)  & 0xFF);
        bb.writeByte(len & 0xFF);

        // version
        bb.writeByte(0x03);
        bb.writeByte(0x03);

        bb.writeBytes(clientRandom);

        bb.writeByte(sessionId.length);
        bb.writeBytes(sessionId);

        bb.writeShort(cipherSuites.length);
        bb.writeBytes(cipherSuites);

        // compression
        bb.writeByte(0x01);
        bb.writeByte(0x00);

        bb.writeShort(extensions.length + tpsLen);
        bb.writeBytes(extensions);

        if (tps != null) {
            ByteBuf tpsBB = Unpooled.buffer();
            tps.write(tpsBB);
            byte[] x = new byte[tpsBB.writerIndex()];
            tpsBB.readBytes(x);

            bb.writeShort(4085);
            bb.writeShort(x.length);
            bb.writeBytes(x);
        }
    }

    @Override
    public String toString() {
        return "ClientHello{" +
                "clientRandom=" + Hex.hex(clientRandom) +
                ", sessionId=" + Hex.hex(sessionId) +
                ", cipherSuites=" + Hex.hex(cipherSuites) +
                ", extensions=" + Hex.hex(extensions) +
                '}';
    }
}
