package com.protocol7.nettyquick.tls;

import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.tls.aead.HandshakeAEAD;
import com.protocol7.nettyquick.tls.aead.OneRttAEAD;
import com.protocol7.nettyquick.tls.extensions.ExtensionType;
import com.protocol7.nettyquick.tls.extensions.KeyShare;
import com.protocol7.nettyquick.tls.extensions.SupportedVersions;
import com.protocol7.nettyquick.tls.extensions.TransportParameters;
import com.protocol7.nettyquick.tls.messages.ClientFinished;
import com.protocol7.nettyquick.tls.messages.ClientHello;
import com.protocol7.nettyquick.tls.messages.ServerHandshake.EncryptedExtensions;
import com.protocol7.nettyquick.tls.messages.ServerHandshake.ServerCertificate;
import com.protocol7.nettyquick.tls.messages.ServerHandshake.ServerCertificateVerify;
import com.protocol7.nettyquick.tls.messages.ServerHandshake.ServerHandshakeFinished;
import com.protocol7.nettyquick.tls.messages.ServerHello;
import com.protocol7.nettyquick.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.security.PublicKey;
import java.util.Optional;

public class ClientTlsSession {

    private KeyExchange kek;

    private ByteBuf handshakeBuffer;
    private byte[] clientHello;
    private byte[] serverHello;
    private byte[] handshakeSecret;

    public ClientTlsSession() {
        reset();
    }

    public void reset() {
        kek = KeyExchange.generate(Group.X25519);
        handshakeBuffer = Unpooled.buffer();
        clientHello = null;
        serverHello = null;
        handshakeSecret = null;
    }

    public byte[] start() {
        if (clientHello != null) {
            throw new IllegalStateException("Already started");
        }

        ClientHello ch = ClientHello.defaults(kek, TransportParameters.defaults());
        ByteBuf bb = Unpooled.buffer();
        ch.write(bb);

        clientHello = Bytes.drainToArray(bb);
        return clientHello;
    }

    public AEAD handleServerHello(byte[] msg) {
        if (clientHello == null) {
            throw new IllegalStateException("Not started");
        }

        serverHello = msg;

        ByteBuf bb = Unpooled.wrappedBuffer(msg);
        ServerHello hello = ServerHello.parse(bb);

        SupportedVersions version = (SupportedVersions) hello.geExtension(ExtensionType.supported_versions).orElseThrow(IllegalArgumentException::new);
        if (!version.equals(SupportedVersions.TLS13)) {
            throw new IllegalArgumentException("Illegal version");
        }

        KeyShare keyShareExtension = (KeyShare) hello.geExtension(ExtensionType.key_share).orElseThrow(IllegalArgumentException::new);
        byte[] peerPublicKey = keyShareExtension.getKey(Group.X25519).get();
        byte[] sharedSecret = kek.generateSharedSecret(peerPublicKey);

        byte[] helloHash = Hash.sha256(clientHello, serverHello);

        handshakeSecret = HKDFUtil.calculateHandshakeSecret(sharedSecret);

        return HandshakeAEAD.create(handshakeSecret, helloHash, true, true);
    }

    public synchronized Optional<HandshakeResult> handleHandshake(byte[] msg) {
        if (clientHello == null || serverHello == null) {
            throw new IllegalStateException("Got handshake in unexpected state");
        }

        handshakeBuffer.writeBytes(msg);

        handshakeBuffer.markReaderIndex();
        try {
            int pos = handshakeBuffer.readerIndex();
            EncryptedExtensions ee = EncryptedExtensions.parse(handshakeBuffer);
            ServerCertificate sc = ServerCertificate.parse(handshakeBuffer);

            byte[] scvBytes = new byte[handshakeBuffer.readerIndex() - pos];
            handshakeBuffer.resetReaderIndex();
            handshakeBuffer.readBytes(scvBytes);

            ServerCertificateVerify scv = ServerCertificateVerify.parse(handshakeBuffer);

            validateServerCertificateVerify(sc, scv, scvBytes);

            byte[] finBytes = new byte[handshakeBuffer.readerIndex() - pos];
            handshakeBuffer.resetReaderIndex();
            handshakeBuffer.readBytes(finBytes);


            ServerHandshakeFinished fin = ServerHandshakeFinished.parse(handshakeBuffer);

            byte[] helloHash = Hash.sha256(clientHello, serverHello);
            validateServerFinish(fin, helloHash, finBytes);

            // TODO verify certificate


            handshakeBuffer.resetReaderIndex();

            byte[] hs = Bytes.drainToArray(handshakeBuffer);
            handshakeBuffer = Unpooled.buffer();


            byte[] handshakeHash = Hash.sha256(clientHello, serverHello, hs);

            AEAD aead = OneRttAEAD.create(handshakeSecret, handshakeHash, true, true);

            // TODO dedup
            byte[] clientHandshakeTrafficSecret = HKDFUtil.expandLabel(handshakeSecret, "tls13 ","c hs traffic", helloHash, 32);

            ClientFinished clientFinished = ClientFinished.create(clientHandshakeTrafficSecret, handshakeHash, false);

            ByteBuf finBB = Unpooled.buffer();
            clientFinished.write(finBB);
            byte[] b = Bytes.drainToArray(finBB);

            return Optional.of(new HandshakeResult(b, aead));
        } catch (IndexOutOfBoundsException e) {
            // wait for more data
            System.out.println("Need more data, waiting...");
            handshakeBuffer.resetReaderIndex();

            return Optional.empty();
        }
    }

    private void validateServerFinish(ServerHandshakeFinished fin, byte[] helloHash, byte[] finBytes) {
        // verify server fin
        byte[] finishedHash = Hash.sha256(clientHello, serverHello, finBytes);

        byte[] serverHandshakeTrafficSecret = HKDFUtil.expandLabel(handshakeSecret, "tls13 ","s hs traffic", helloHash, 32);

        boolean valid = VerifyData.verify(
                fin.getVerificationData(),
                serverHandshakeTrafficSecret,
                finishedHash,
                false);
        if (!valid) {
            throw new RuntimeException("Server verification data not valid");
        }
    }

    private void validateServerCertificateVerify(ServerCertificate sc, ServerCertificateVerify scv, byte[] handshakeData) {
        byte[] toVerify = Hash.sha256(clientHello, serverHello, handshakeData);

        byte[] serverSig = scv.getSignature();

        PublicKey serverKey = sc.getAsCertificiates().get(0).getPublicKey();

        boolean valid = CertificateVerify.verify(serverSig, toVerify, serverKey, false);
        if (!valid) {
            throw new RuntimeException("Invalid server certificate verify");
        }
    }

    public static class HandshakeResult {
        private final byte[] fin;
        private final AEAD oneRttAead;

        public HandshakeResult(byte[] fin, AEAD oneRttAead) {
            this.fin = fin;
            this.oneRttAead = oneRttAead;
        }

        public byte[] getFin() {
            return fin;
        }

        public AEAD getOneRttAead() {
            return oneRttAead;
        }
    }
}
