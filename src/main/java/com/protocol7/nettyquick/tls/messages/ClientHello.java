package com.protocol7.nettyquick.tls.messages;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.protocol7.nettyquick.Writeable;
import com.protocol7.nettyquick.tls.CipherSuite;
import com.protocol7.nettyquick.tls.Group;
import com.protocol7.nettyquick.tls.KeyExchange;
import com.protocol7.nettyquick.tls.extensions.*;
import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Hex;
import com.protocol7.nettyquick.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ClientHello implements Writeable {

    public static ClientHello defaults(KeyExchange ke, TransportParameters tps) {
        byte[] clientRandom = Rnd.rndBytes(32);
        byte[] sessionId = new byte[0];
        List<CipherSuite> cipherSuites = CipherSuite.SUPPORTED;
        List<Extension> extensions = ImmutableList.of(
                KeyShare.of(ke.getGroup(), ke.getPublicKey()),
                new SupportedGroups(Group.X25519),
                SupportedVersions.TLS13,
                tps
        );

        return new ClientHello(clientRandom, sessionId, cipherSuites, extensions);
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

        List<CipherSuite> cipherSuites = CipherSuite.parseKnown(bb);

        byte[] compression = new byte[2];
        bb.readBytes(compression);
        if (!Arrays.equals(compression, new byte[]{1, 0})) {
            throw new IllegalArgumentException("Compression must be disabled: " + Hex.hex(compression));
        }

        int extensionsLen = bb.readShort();
        List<Extension> ext = Extension.parseAll(bb.readBytes(extensionsLen), true);

        return new ClientHello(
                clientRandom,
                sessionId,
                cipherSuites,
                ext);
    }

    private final byte[] clientRandom;
    private final byte[] sessionId;
    private final List<CipherSuite> cipherSuites;
    private final List<Extension> extensions;

    public ClientHello(byte[] clientRandom, byte[] sessionId, List<CipherSuite> cipherSuites, List<Extension> extensions) {
        Preconditions.checkArgument(clientRandom.length == 32);
        this.clientRandom = clientRandom;
        this.sessionId = Preconditions.checkNotNull(sessionId);
        this.cipherSuites = Preconditions.checkNotNull(cipherSuites);
        this.extensions = extensions;
    }

    public byte[] getClientRandom() {
        return clientRandom;
    }

    public byte[] getSessionId() {
        return sessionId;
    }

    public List<CipherSuite> getCipherSuites() {
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

    public ClientHello addExtension(Extension extension) {
        List<Extension> newExtensionMap = Lists.newArrayList(extensions);
        newExtensionMap.add(extension);

        return new ClientHello(clientRandom, sessionId, cipherSuites, newExtensionMap);
    }

    public void write(ByteBuf bb) {
        bb.writeByte(0x01);

        // payload length
        int lenPos = bb.writerIndex();
        Bytes.write24(bb, 0); // placeholder

        // version
        bb.writeByte(0x03);
        bb.writeByte(0x03);

        bb.writeBytes(clientRandom);

        bb.writeByte(sessionId.length);
        bb.writeBytes(sessionId);

        CipherSuite.writeAll(bb, cipherSuites);

        // compression
        bb.writeByte(0x01);
        bb.writeByte(0x00);

        int extLenPos = bb.writerIndex();
        bb.writeShort(0);
        Extension.writeAll(extensions, bb, true);

        bb.setShort(extLenPos, bb.writerIndex() - extLenPos - 2);

        Bytes.set24(bb, lenPos, bb.writerIndex() - lenPos - 3);
    }

    @Override
    public String toString() {
        return "ClientHello{" +
                "clientRandom=" + Hex.hex(clientRandom) +
                ", sessionId=" + Hex.hex(sessionId) +
                ", cipherSuites=" + cipherSuites +
                ", extensions=" + extensions +
                '}';
    }
}
