package com.protocol7.nettyquick.tls;

import com.protocol7.nettyquick.tls.extensions.Extension;
import com.protocol7.nettyquick.tls.extensions.ExtensionType;
import com.protocol7.nettyquick.utils.Bytes;
import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.protocol7.nettyquick.utils.Hex.hex;

public class ServerHello {

    private final static byte[] VERSION = new byte[]{0x03, 0x03};
    private final static byte[] CIPHER_SUITES = new byte[]{0x13, 0x01};

    public static ServerHello parse(ByteBuf bb) {
        int messageType = bb.readByte(); // server hello
        if (messageType != 0x02) {
            throw new IllegalArgumentException("Not a server hello");
        }

        Bytes.read24(bb); // payloadLength

        byte[] version = new byte[2];
        bb.readBytes(version);
        if (!Arrays.equals(version, VERSION)) {
            throw new IllegalArgumentException("Illegal version");
        }

        byte[] serverRandom = new byte[32];
        bb.readBytes(serverRandom); // server random

        int sessionIdLen = bb.readByte();
        byte[] sessionId = new byte[sessionIdLen];

        byte[] cipherSuites = new byte[2];
        bb.readBytes(cipherSuites); // cipher suite
        // TODO implement all know cipher suites
        if (!Arrays.equals(cipherSuites, CIPHER_SUITES)) {
            throw new IllegalArgumentException("Illegal cipher suite: " + hex(version));
        }

        bb.readByte(); // compressionMethod

        int extensionLen = bb.readShort();
        ByteBuf extBB = bb.readBytes(extensionLen);
        try {
            List<Extension> extensions = Extension.parseAll(extBB, false);
            return new ServerHello(serverRandom, sessionId, cipherSuites, extensions);
        } finally {
            extBB.release();
        }
    }

    public void write(ByteBuf bb) {
        bb.writeByte(0x02);

        int lenPosition = bb.writerIndex();
        // write placeholder
        bb.writeBytes(new byte[3]);
        bb.writeBytes(VERSION);

        bb.writeBytes(serverRandom);
        bb.writeByte(sessionId.length);
        bb.writeBytes(sessionId);
        bb.writeBytes(cipherSuites);
        bb.writeByte(0);

        int extPosition = bb.writerIndex();
        bb.writeShort(0); // placeholder

        Extension.writeAll(extensions, bb, false);
        bb.setShort(extPosition, bb.writerIndex() - extPosition - 2);


        // update length
        Bytes.write24(bb, bb.writerIndex() - lenPosition - 3, lenPosition);

    }

    private final byte[] serverRandom;
    private final byte[] sessionId;
    private final byte[] cipherSuites;
    private final List<Extension> extensions;

    public ServerHello(byte[] serverRandom, byte[] sessionId, byte[] cipherSuites, List<Extension> extensions) {
        this.serverRandom = serverRandom;
        this.sessionId = sessionId;
        this.cipherSuites = cipherSuites;
        this.extensions = extensions;
    }

    public byte[] getServerRandom() {
        return serverRandom;
    }

    public byte[] getSessionId() {
        return sessionId;
    }

    public byte[] getCipherSuites() {
        return cipherSuites;
    }

    public List<Extension> getExtensions() {
        return extensions;
    }

    public Optional<Extension> geExtension(ExtensionType type) {
        for (Extension ext : extensions) {
            if (ext.getType().equals(type)) {
                return Optional.of(ext);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "ServerHello{" +
                "serverRandom=" + hex(serverRandom) +
                ", sessionId=" + hex(sessionId) +
                ", cipherSuites=" + hex(cipherSuites) +
                ", extensions=" + extensions +
                '}';
    }
}
