package com.protocol7.nettyquick.tls;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.protocol7.nettyquick.tls.extensions.ExtensionType;
import com.protocol7.nettyquick.tls.extensions.KeyShare;
import com.protocol7.nettyquick.tls.extensions.SupportedVersions;
import com.protocol7.nettyquick.tls.extensions.TransportParameters;
import com.protocol7.nettyquick.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Optional;

public class ServerTlsSession {

    private static final HashFunction SHA256 = Hashing.sha256();

    private KeyExchange kek;

    private byte[] clientHello;
    private byte[] serverHello;
    private byte[] handshakeSecret;

    public ServerTlsSession() {
        reset();
    }

    public void reset() {
        kek = KeyExchange.generate(Group.X25519);
        clientHello = null;
        serverHello = null;
        handshakeSecret = null;
    }

    public AEAD handleClientHello(byte[] msg) {
        clientHello = msg;

        ClientHello hello = ClientHello.parse(msg);

        SupportedVersions version = (SupportedVersions) hello.geExtension(ExtensionType.supported_versions).orElseThrow(IllegalArgumentException::new);
        if (!version.equals(SupportedVersions.TLS13)) {
            throw new IllegalArgumentException("Illegal version");
        }

        KeyShare keyShareExtension = (KeyShare) hello.geExtension(ExtensionType.key_share).orElseThrow(IllegalArgumentException::new);

        byte[] peerPublicKey = keyShareExtension.getKey(Group.X25519).get();
        byte[] sharedSecret = kek.generateSharedSecret(peerPublicKey);

        byte[] helloHash = SHA256.hashBytes(Bytes.concat(clientHello, serverHello)).asBytes();

        handshakeSecret = HKDFUtil.calculateHandshakeSecret(sharedSecret);

        return HandshakeAEAD.create(handshakeSecret, helloHash, true, true);
    }

    public synchronized void handleClientFinished(byte[] msg) {
        if (clientHello == null || serverHello == null) {
            throw new IllegalStateException("Got handshake in unexpected state");
        }

        ByteBuf bb = Unpooled.wrappedBuffer(msg);
        ClientFinished fin = ClientFinished.parse(bb);

        // TODO verify handshake
    }
}
