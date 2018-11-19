package com.protocol7.nettyquick.tls;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.tls.aead.HandshakeAEAD;
import com.protocol7.nettyquick.tls.aead.OneRttAEAD;
import com.protocol7.nettyquick.tls.extensions.ExtensionType;
import com.protocol7.nettyquick.tls.extensions.KeyShare;
import com.protocol7.nettyquick.tls.extensions.SupportedVersions;
import com.protocol7.nettyquick.tls.extensions.TransportParameters;
import com.protocol7.nettyquick.tls.messages.ClientFinished;
import com.protocol7.nettyquick.tls.messages.ClientHello;
import com.protocol7.nettyquick.tls.messages.ServerHandshake;
import com.protocol7.nettyquick.tls.messages.ServerHello;
import com.protocol7.nettyquick.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.security.PrivateKey;
import java.util.List;

import static com.protocol7.nettyquick.utils.Bytes.peekToArray;

public class ServerTlsSession {

    private static final HashFunction SHA256 = Hashing.sha256();

    private KeyExchange kek;

    private final PrivateKey privateKey;
    private final List<byte[]> certificates;
    private byte[] clientHello;
    private byte[] serverHello;
    private byte[] handshake;

    public ServerTlsSession(List<byte[]> certificates, PrivateKey privateKey) {
        this.privateKey = privateKey;
        this.certificates = certificates;
        reset();
    }

    public void reset() {
        kek = KeyExchange.generate(Group.X25519);
        clientHello = null;
        serverHello = null;
        handshake = null;
    }

    public ServerHelloAndHandshake handleClientHello(byte[] msg) {
        clientHello = msg;

        ClientHello ch = ClientHello.parse(msg);

        SupportedVersions version = (SupportedVersions) ch.geExtension(ExtensionType.supported_versions).orElseThrow(IllegalArgumentException::new);
        if (!version.equals(SupportedVersions.TLS13)) {
            throw new IllegalArgumentException("Illegal version");
        }

        KeyShare keyShareExtension = (KeyShare) ch.geExtension(ExtensionType.key_share).orElseThrow(IllegalArgumentException::new);

        ServerHello sh = ServerHello.defaults(kek, TransportParameters.defaults());

        ByteBuf shBB = Unpooled.buffer();
        sh.write(shBB);
        serverHello = Bytes.drainToArray(shBB);

        byte[] helloHash = SHA256.hashBytes(Bytes.concat(clientHello, serverHello)).asBytes();

        ByteBuf handshakeBB = Unpooled.buffer();

        ServerHandshake.EncryptedExtensions ee = ServerHandshake.EncryptedExtensions.defaults();
        ee.write(handshakeBB);

        ServerHandshake.ServerCertificate sc = new ServerHandshake.ServerCertificate(new byte[0], certificates);
        sc.write(handshakeBB);

        byte[] toVerify = peekToArray(handshakeBB);

        byte[] verificationSig = CertificateVerify.sign(Bytes.concat(clientHello, serverHello, toVerify), privateKey, false);

        ServerHandshake.ServerCertificateVerify scv = new ServerHandshake.ServerCertificateVerify(2052, verificationSig);
        scv.write(handshakeBB);

        byte[] peerPublicKey = keyShareExtension.getKey(Group.X25519).get();
        byte[] sharedSecret = kek.generateSharedSecret(peerPublicKey);
        byte[] handshakeSecret = HKDFUtil.calculateHandshakeSecret(sharedSecret);
        byte[] serverHandshakeTrafficSecret = HKDFUtil.expandLabel(handshakeSecret, "tls13 ","s hs traffic", helloHash, 32);
        // finished_key = HKDF-Expand-Label(
        //    key = server_handshake_traffic_secret,
        //    label = "finished",
        //    context = "",
        //    len = 32)
        byte[] finishedKey = HKDFUtil.expandLabel(serverHandshakeTrafficSecret, "tls13 ", "finished", new byte[0], 32);

        // finished_hash = SHA256(Client Hello ... Server Cert Verify)
        byte[] finishedHash = SHA256.hashBytes(Bytes.concat(clientHello, serverHello, peekToArray(handshakeBB))).asBytes();

        // verify_data = HMAC-SHA256(
        //	  key = finished_key,
        //	  msg = finished_hash)
        byte[] verifyData = Hashing.hmacSha256(finishedKey).hashBytes(finishedHash).asBytes();

        ServerHandshake.ServerHandshakeFinished fin = new ServerHandshake.ServerHandshakeFinished(verifyData);
        fin.write(handshakeBB);

        handshake = Bytes.drainToArray(handshakeBB);

        byte[] handshakeHash = SHA256.hashBytes(Bytes.concat(clientHello, serverHello, handshake)).asBytes();

        AEAD handshakeAEAD = HandshakeAEAD.create(handshakeSecret, helloHash, true, true);
        AEAD oneRttAEAD = OneRttAEAD.create(handshakeSecret, handshakeHash, true, false);

        return new ServerHelloAndHandshake(serverHello, handshake, handshakeAEAD, oneRttAEAD);
    }

    public static class ServerHelloAndHandshake {

        private final byte[] serverHello;
        private final byte[] serverHandshake;

        private final AEAD handshakeAEAD;
        private final AEAD oneRttAEAD;

        public ServerHelloAndHandshake(byte[] serverHello, byte[] serverHandshake, AEAD handshakeAEAD, AEAD oneRttAEAD) {
            this.serverHello = serverHello;
            this.serverHandshake = serverHandshake;
            this.handshakeAEAD = handshakeAEAD;
            this.oneRttAEAD = oneRttAEAD;
        }

        public byte[] getServerHello() {
            return serverHello;
        }

        public byte[] getServerHandshake() {
            return serverHandshake;
        }

        public AEAD getHandshakeAEAD() {
            return handshakeAEAD;
        }

        public AEAD getOneRttAEAD() {
            return oneRttAEAD;
        }
    }


    public synchronized void handleClientFinished(byte[] msg) {
        if (clientHello == null || serverHello == null || handshake == null) {
            throw new IllegalStateException("Got handshake in unexpected state");
        }

        ByteBuf bb = Unpooled.wrappedBuffer(msg);
        ClientFinished fin = ClientFinished.parse(bb);

        // TODO verify handshake
    }
}
