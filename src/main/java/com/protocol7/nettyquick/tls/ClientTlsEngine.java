package com.protocol7.nettyquick.tls;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.protocol7.nettyquick.tls.extensions.ExtensionType;
import com.protocol7.nettyquick.tls.extensions.KeyShare;
import com.protocol7.nettyquick.tls.extensions.TransportParameters;
import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ClientTlsEngine {

    enum State {
        Start,
        WaitingForServerHello
    }

    private State state = State.Start;
    private KeyExchangeKeys kek;

    private byte[] clientHello;

    public ClientTlsEngine() {
        reset();
    }

    public void reset() {
        state = State.Start;
        kek = KeyExchangeKeys.generate(Group.X25519);
    }

    public byte[] start() {
        if (state != State.Start) {
            throw new IllegalStateException("Already started");
        }

        ClientHello ch = ClientHello.defaults(kek, TransportParameters.defaults());
        ByteBuf bb = Unpooled.buffer();
        ch.write(bb);

        state = State.WaitingForServerHello;
        clientHello = Bytes.asArray(bb);
        return clientHello;
    }

    public AEAD handleServerHello(byte[] msg) {
        if (state == State.WaitingForServerHello) {
            ByteBuf bb = Unpooled.wrappedBuffer(msg);
            ServerHello serverHello = ServerHello.parse(bb);

            // TODO handle errors
            KeyShare keyShareExtension = (KeyShare) serverHello.geExtension(ExtensionType.key_share).get();
            byte[] peerPublicKey = keyShareExtension.getKey(Group.X25519).get();
            byte[] sharedSecret = kek.generateSharedSecret(peerPublicKey);

            HashFunction sha256 = Hashing.sha256();
            byte[] helloHash = sha256.hashBytes(Bytes.concat(clientHello, msg)).asBytes();

            return HandshakeAEAD.create(sharedSecret, helloHash, true, true);
        } else {
            throw new IllegalStateException("Got server hello in unexpected state");
        }
    }
}
