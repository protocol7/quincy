package com.protocol7.nettyquick.tls;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.protocol7.nettyquick.tls.extensions.ExtensionType;
import com.protocol7.nettyquick.tls.extensions.KeyShare;
import com.protocol7.nettyquick.tls.extensions.TransportParameters;
import com.protocol7.nettyquick.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Optional;

public class ClientTlsEngine {

    private static final HashFunction SHA256 = Hashing.sha256();

    enum State {
        Start,
        WaitingForServerHello
    }

    private State state;
    private KeyExchangeKeys kek;
    private ByteBuf handshakeBuffer;

    private byte[] clientHello;
    private byte[] serverHello;
    private byte[] handshakeSecret;

    public ClientTlsEngine() {
        reset();
    }

    public void reset() {
        state = State.Start;
        kek = KeyExchangeKeys.generate(Group.X25519);
        handshakeBuffer = Unpooled.buffer();
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
            serverHello = msg;

            ByteBuf bb = Unpooled.wrappedBuffer(msg);
            ServerHello hello = ServerHello.parse(bb);

            // TODO handle errors
            KeyShare keyShareExtension = (KeyShare) hello.geExtension(ExtensionType.key_share).get();
            byte[] peerPublicKey = keyShareExtension.getKey(Group.X25519).get();
            byte[] sharedSecret = kek.generateSharedSecret(peerPublicKey);

            byte[] helloHash = SHA256.hashBytes(Bytes.concat(clientHello, serverHello)).asBytes();

            handshakeSecret = AEADUtil.calculateHandshakeSecret(sharedSecret);

            return HandshakeAEAD.create(handshakeSecret, helloHash, true, true);
        } else {
            throw new IllegalStateException("Got server hello in unexpected state");
        }
    }

    public Optional<AEAD> handleHandshake(byte[] msg) {
        handshakeBuffer.writeBytes(msg);

        handshakeBuffer.markReaderIndex();
        try {
            ServerHandshake handshake = ServerHandshake.parse(handshakeBuffer);

            // TODo verify handshake

            handshakeBuffer.resetReaderIndex();

            byte[] hs = Bytes.asArray(handshakeBuffer);

            byte[] handshakeHash = SHA256.hashBytes(Bytes.concat(clientHello, serverHello, hs)).asBytes();

            AEAD aead = OneRttAEAD.create(handshakeSecret, handshakeHash, true, true);

            handshakeBuffer = Unpooled.buffer();

            return Optional.of(aead);
        } catch (IndexOutOfBoundsException e) {
            // wait for more data
            System.out.println("Need more data, waiting...");
            handshakeBuffer.resetReaderIndex();

            return Optional.empty();
        }
    }
}
