package com.protocol7.quincy.tls;

import static com.protocol7.quincy.tls.aead.Labels.CLIENT_HANDSHAKE_TRAFFIC_SECRET;

import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.AEADs;
import com.protocol7.quincy.tls.aead.HandshakeAEAD;
import com.protocol7.quincy.tls.aead.OneRttAEAD;
import com.protocol7.quincy.tls.extensions.ExtensionType;
import com.protocol7.quincy.tls.extensions.KeyShare;
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
import java.security.PublicKey;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientTlsSession {

  private final Logger log = LoggerFactory.getLogger(ClientTlsSession.class);

  private final TransportParameters transportParameters;

  private final AEADs aeads;
  private final KeyExchange kek;

  private ByteBuf handshakeBuffer;
  private byte[] clientHello;
  private byte[] serverHello;
  private byte[] handshakeSecret;

  public ClientTlsSession(final AEAD initialAEAD, final TransportParameters transportParameters) {
    this.transportParameters = transportParameters;

    aeads = new AEADs(initialAEAD);
    kek = KeyExchange.generate(Group.X25519);
    handshakeBuffer = Unpooled.buffer(); // replace with position keeping buffer
  }

  public byte[] startHandshake() {
    if (clientHello != null) {
      throw new IllegalStateException("Already started");
    }

    ClientHello ch = ClientHello.defaults(kek, transportParameters);
    clientHello = Bytes.write(bb -> ch.write(bb, true));
    return clientHello;
  }

  public AEAD handleServerHello(byte[] msg) {
    if (clientHello == null) {
      throw new IllegalStateException("Not started");
    }

    serverHello = msg;

    ByteBuf bb = Unpooled.wrappedBuffer(msg);
    ServerHello hello = ServerHello.parse(bb, true);

    SupportedVersions version =
        (SupportedVersions)
            hello
                .geExtension(ExtensionType.SUPPORTED_VERSIONS)
                .orElseThrow(IllegalArgumentException::new);
    if (!version.equals(SupportedVersions.TLS13)) {
      throw new IllegalArgumentException("Illegal version");
    }

    KeyShare keyShareExtension =
        (KeyShare)
            hello.geExtension(ExtensionType.KEY_SHARE).orElseThrow(IllegalArgumentException::new);
    byte[] peerPublicKey = keyShareExtension.getKey(Group.X25519).get();
    byte[] sharedSecret = kek.generateSharedSecret(peerPublicKey);

    byte[] helloHash = Hash.sha256(clientHello, serverHello);

    handshakeSecret = HKDF.calculateHandshakeSecret(sharedSecret);

    return HandshakeAEAD.create(handshakeSecret, helloHash, true);
  }

  public synchronized Optional<HandshakeResult> handleHandshake(byte[] msg) {
    if (clientHello == null || serverHello == null) {
      throw new IllegalStateException("Got handshake in unexpected state");
    }

    handshakeBuffer.writeBytes(msg);

    handshakeBuffer.markReaderIndex();
    try {
      int pos = handshakeBuffer.readerIndex();
      EncryptedExtensions ee = EncryptedExtensions.parse(handshakeBuffer, true);
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

      AEAD aead = OneRttAEAD.create(handshakeSecret, handshakeHash, true);

      // TODO dedup
      byte[] clientHandshakeTrafficSecret =
          HKDF.expandLabel(handshakeSecret, CLIENT_HANDSHAKE_TRAFFIC_SECRET, helloHash, 32);

      ClientFinished clientFinished =
          ClientFinished.create(clientHandshakeTrafficSecret, handshakeHash);

      byte[] b = Bytes.write(clientFinished);

      return Optional.of(new HandshakeResult(b, aead));
    } catch (IndexOutOfBoundsException e) {
      // wait for more data
      log.debug("Need more data, waiting...");
      handshakeBuffer.resetReaderIndex();

      return Optional.empty();
    }
  }

  private void validateServerFinish(
      ServerHandshakeFinished fin, byte[] helloHash, byte[] finBytes) {
    // verify server fin
    byte[] finishedHash = Hash.sha256(clientHello, serverHello, finBytes);

    byte[] serverHandshakeTrafficSecret =
        HKDF.expandLabel(handshakeSecret, "s hs traffic", helloHash, 32);

    boolean valid =
        VerifyData.verify(
            fin.getVerificationData(), serverHandshakeTrafficSecret, finishedHash, false);
    if (!valid) {
      throw new RuntimeException("Server verification data not valid");
    }
  }

  private void validateServerCertificateVerify(
      ServerCertificate sc, ServerCertificateVerify scv, byte[] handshakeData) {
    byte[] toVerify = Hash.sha256(clientHello, serverHello, handshakeData);

    byte[] serverSig = scv.getSignature();

    PublicKey serverKey = sc.getAsCertificiates().get(0).getPublicKey();

    boolean valid = CertificateVerify.verify(serverSig, toVerify, serverKey, false);
    if (!valid) {
      throw new RuntimeException("Invalid server certificate verify");
    }
  }

  public boolean available(final EncryptionLevel encLevel) {
    return aeads.available(encLevel);
  }

  public AEAD getAEAD(final EncryptionLevel level) {
    return aeads.get(level);
  }

  public void setHandshakeAead(final AEAD handshakeAead) {
    aeads.setHandshakeAead(handshakeAead);
  }

  public void setOneRttAead(final AEAD oneRttAead) {
    aeads.setOneRttAead(oneRttAead);
  }

  public void unsetInitialAead() {
    aeads.unsetInitialAead();
  }

  public void unsetHandshakeAead() {
    aeads.unsetHandshakeAead();
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
