package com.protocol7.quincy.tls;

import static com.protocol7.quincy.utils.Bytes.peekToArray;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.AEADs;
import com.protocol7.quincy.tls.aead.HandshakeAEAD;
import com.protocol7.quincy.tls.aead.OneRttAEAD;
import com.protocol7.quincy.tls.extensions.ExtensionType;
import com.protocol7.quincy.tls.extensions.KeyShare;
import com.protocol7.quincy.tls.extensions.SupportedVersion;
import com.protocol7.quincy.tls.extensions.SupportedVersions;
import com.protocol7.quincy.tls.extensions.TransportParameters;
import com.protocol7.quincy.tls.messages.ClientFinished;
import com.protocol7.quincy.tls.messages.ClientHello;
import com.protocol7.quincy.tls.messages.ServerHandshake.EncryptedExtensions;
import com.protocol7.quincy.tls.messages.ServerHandshake.ServerCertificate;
import com.protocol7.quincy.tls.messages.ServerHandshake.ServerCertificateVerify;
import com.protocol7.quincy.tls.messages.ServerHandshake.ServerHandshakeFinished;
import com.protocol7.quincy.tls.messages.ServerHello;
import com.protocol7.quincy.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.security.PrivateKey;
import java.util.List;

public class ServerTlsSession {

  private final TransportParameters transportParameters;

  private final AEADs aeads;
  private final KeyExchange kek;

  private final PrivateKey privateKey;
  private final List<byte[]> certificates;
  private byte[] clientHello;
  private byte[] serverHello;
  private byte[] handshake;
  private byte[] handshakeSecret;

  public ServerTlsSession(
      final AEAD initialAEAD,
      final TransportParameters transportParameters,
      List<byte[]> certificates,
      PrivateKey privateKey) {
    this.transportParameters = transportParameters;
    Preconditions.checkArgument(!certificates.isEmpty());

    aeads = new AEADs(initialAEAD);
    this.privateKey = privateKey;
    this.certificates = requireNonNull(certificates);
    this.kek = KeyExchange.generate(Group.X25519);
  }

  public ServerHelloAndHandshake handleClientHello(byte[] msg) {
    clientHello = msg;

    ClientHello ch = ClientHello.parse(msg, false);

    System.out.println(ch);

    // verify expected extensions
    SupportedVersions versions =
        (SupportedVersions)
            ch.getExtension(ExtensionType.SUPPORTED_VERSIONS)
                .orElseThrow(IllegalArgumentException::new);
    if (!versions.getVersions().contains(SupportedVersion.TLS13)) {
      throw new IllegalArgumentException("Illegal version");
    }

    KeyShare keyShareExtension =
        (KeyShare)
            ch.getExtension(ExtensionType.KEY_SHARE).orElseThrow(IllegalArgumentException::new);

    // create ServerHello
    serverHello = Bytes.write(ServerHello.defaults(kek, transportParameters));

    ByteBuf handshakeBB = Unpooled.buffer();

    // TODO decide on what parameters to send where
    EncryptedExtensions ee = EncryptedExtensions.defaults(transportParameters);
    ee.write(handshakeBB);

    ServerCertificate sc = new ServerCertificate(new byte[0], certificates);
    sc.write(handshakeBB);

    // create server cert verification
    byte[] toVerify = peekToArray(handshakeBB);

    byte[] verificationSig =
        CertificateVerify.sign(Hash.sha256(clientHello, serverHello, toVerify), privateKey, false);

    ServerCertificateVerify scv = new ServerCertificateVerify(2052, verificationSig);
    scv.write(handshakeBB);

    // create server finished
    byte[] peerPublicKey = keyShareExtension.getKey(Group.X25519).get();
    byte[] sharedSecret = kek.generateSharedSecret(peerPublicKey);
    handshakeSecret = HKDF.calculateHandshakeSecret(sharedSecret);
    byte[] helloHash = Hash.sha256(clientHello, serverHello);

    // create handshake AEAD
    AEAD handshakeAEAD = HandshakeAEAD.create(handshakeSecret, helloHash, true);

    byte[] serverHandshakeTrafficSecret =
        HKDF.expandLabel(handshakeSecret, "s hs traffic", helloHash, 32);

    // finished_hash = SHA256(Client Hello ... Server Cert Verify)
    byte[] finishedHash = Hash.sha256(clientHello, serverHello, peekToArray(handshakeBB));

    byte[] verifyData = VerifyData.create(serverHandshakeTrafficSecret, finishedHash);

    ServerHandshakeFinished fin = new ServerHandshakeFinished(verifyData);
    fin.write(handshakeBB);

    // create 1-RTT AEAD
    handshake = Bytes.drainToArray(handshakeBB);

    byte[] handshakeHash = Hash.sha256(clientHello, serverHello, handshake);
    AEAD oneRttAEAD = OneRttAEAD.create(handshakeSecret, handshakeHash, false);

    return new ServerHelloAndHandshake(serverHello, handshake, handshakeAEAD, oneRttAEAD);
  }

  public synchronized void handleClientFinished(byte[] msg) {
    if (clientHello == null || serverHello == null || handshake == null) {
      throw new IllegalStateException("Got handshake in unexpected state");
    }

    ByteBuf bb = Unpooled.wrappedBuffer(msg);
    ClientFinished fin = ClientFinished.parse(bb);

    byte[] helloHash = Hash.sha256(clientHello, serverHello);

    byte[] clientHandshakeTrafficSecret =
        HKDF.expandLabel(handshakeSecret, "c hs traffic", helloHash, 32);

    byte[] handshakeHash = Hash.sha256(clientHello, serverHello, handshake);

    boolean valid =
        VerifyData.verify(
            fin.getVerificationData(), clientHandshakeTrafficSecret, handshakeHash, false);

    if (!valid) {
      throw new RuntimeException("Invalid client verification");
    }
  }

  public AEAD getAEAD(final EncryptionLevel level) {
    return aeads.get(level);
  }

  public void setHandshakeAead(final AEAD handshakeAEAD) {
    aeads.setHandshakeAead(handshakeAEAD);
  }

  public void setOneRttAead(final AEAD oneRttAEAD) {
    aeads.setOneRttAead(oneRttAEAD);
  }

  public boolean available(final EncryptionLevel level) {
    return aeads.available(level);
  }

  public void unsetInitialAead() {
    aeads.unsetInitialAead();
  }

  public void unsetHandshakeAead() {
    aeads.unsetHandshakeAead();
  }

  public static class ServerHelloAndHandshake {

    private final byte[] serverHello;
    private final byte[] serverHandshake;

    private final AEAD handshakeAEAD;
    private final AEAD oneRttAEAD;

    public ServerHelloAndHandshake(
        byte[] serverHello, byte[] serverHandshake, AEAD handshakeAEAD, AEAD oneRttAEAD) {
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
}
