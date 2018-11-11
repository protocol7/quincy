package com.protocol7.nettyquick.tls;

import com.protocol7.nettyquick.tls.extensions.Extension;
import com.protocol7.nettyquick.tls.extensions.ExtensionType;
import io.netty.buffer.ByteBuf;

import java.util.List;
import java.util.Optional;

import static com.protocol7.nettyquick.utils.Hex.hex;

public class ServerHello extends TlsMessage {

    public static ServerHello parse(ByteBuf bb) {
        int messageType = bb.readByte(); // server hello
        if (messageType != 0x02) {
            throw new IllegalArgumentException("Not a server hello");
        }

        read24(bb); // payloadLength

        bb.readShort(); // version

        byte[] serverRandom = new byte[32];
        bb.readBytes(serverRandom); // server random

        int sessionIdLen = bb.readByte();
        byte[] sessionId = new byte[sessionIdLen];

        byte[] cipherSuites = new byte[2];
        bb.readBytes(cipherSuites); // cipher suite

        bb.readByte(); // compressionMethod

        int extensionLen = bb.readShort();
        List<Extension> extensions = Extension.parseAll(bb.readBytes(extensionLen), false);

        return new ServerHello(serverRandom, sessionId, cipherSuites, extensions);
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
