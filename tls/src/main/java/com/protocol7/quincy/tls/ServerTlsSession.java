package com.protocol7.quincy.tls;

import static com.protocol7.quincy.utils.Bytes.peekToArray;
import static io.netty.util.internal.ObjectUtil.checkNonEmpty;
import static java.util.Objects.requireNonNull;

import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.AEADs;
import com.protocol7.quincy.tls.aead.HandshakeAEAD;
import com.protocol7.quincy.tls.aead.OneRttAEAD;
import com.protocol7.quincy.tls.extensions.ALPN;
import com.protocol7.quincy.tls.extensions.Extension;
import com.protocol7.quincy.tls.extensions.ExtensionType;
import com.protocol7.quincy.tls.extensions.KeyShare;
import com.protocol7.quincy.tls.extensions.SupportedGroups;
import com.protocol7.quincy.tls.extensions.SupportedVersion;
import com.protocol7.quincy.tls.extensions.SupportedVersions;
import com.protocol7.quincy.tls.extensions.TransportParameters;
import com.protocol7.quincy.tls.messages.ClientHello;
import com.protocol7.quincy.tls.messages.EncryptedExtensions;
import com.protocol7.quincy.tls.messages.Finished;
import com.protocol7.quincy.tls.messages.ServerCertificate;
import com.protocol7.quincy.tls.messages.ServerCertificateVerify;
import com.protocol7.quincy.tls.messages.ServerHello;
import com.protocol7.quincy.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;

public class ServerTlsSession {

  private final TransportParameters defaultTransportParameters;

  private final AEADs aeads;
  private final KeyExchange kek;

  private final List<String> applicationProtocols;

  private final PrivateKey privateKey;
  private final List<byte[]> certificates;
  private byte[] clientHello;
  private byte[] serverHello;
  private byte[] handshake;
  private byte[] handshakeSecret;

  public ServerTlsSession(
      final AEAD initialAEAD,
      final List<String> applicationProtocols,
      final TransportParameters defaultTransportParameters,
      final List<byte[]> certificates,
      final PrivateKey privateKey) {
    this.defaultTransportParameters = requireNonNull(defaultTransportParameters);
    this.aeads = new AEADs(requireNonNull(initialAEAD));
    this.applicationProtocols = applicationProtocols;
    this.privateKey = requireNonNull(privateKey);
    this.certificates = checkNonEmpty(certificates, "certificates");
    this.kek = KeyExchange.generate(Group.X25519);
  }

  public ServerHelloAndHandshake handleClientHello(final byte[] msg) {
    clientHello = msg;

    final ByteBuf bb = Unpooled.wrappedBuffer(msg);
    try {
      final ClientHello ch = ClientHello.parse(bb, false);

      // verify expected extensions
      final SupportedVersions versions =
          (SupportedVersions)
              ch.getExtension(ExtensionType.SUPPORTED_VERSIONS)
                  .orElseThrow(IllegalArgumentException::new);
      if (!versions.getVersions().contains(SupportedVersion.TLS13)) {
        throw new IllegalArgumentException("Illegal version");
      }

      final Optional<Extension> clientALPNOpt =
          ch.getExtension(ExtensionType.APPLICATION_LAYER_PROTOCOL_NEGOTIATION);

      String selectedApplicationProtocol = null;
      if (clientALPNOpt.isEmpty()) {
        throw new IllegalArgumentException("Missing ALPN");
      } else {
        final ALPN clientALPN = (ALPN) clientALPNOpt.get();

        for (final String applicationProtocol : applicationProtocols) {
          if (clientALPN.contains(applicationProtocol)) {
            selectedApplicationProtocol = applicationProtocol;
          }
        }

        if (selectedApplicationProtocol == null) {
          throw new IllegalArgumentException("Client ALPN not supported");
        }
      }

      final KeyShare keyShareExtension =
          (KeyShare)
              ch.getExtension(ExtensionType.KEY_SHARE).orElseThrow(IllegalArgumentException::new);

      // create ServerHello
      serverHello = Bytes.write(ServerHello.defaults(kek));

      final ByteBuf handshakeBB = Unpooled.buffer();

      final EncryptedExtensions ee =
          new EncryptedExtensions(
              new SupportedGroups(Group.X25519),
              new ALPN(selectedApplicationProtocol),
              defaultTransportParameters);
      ee.write(handshakeBB);

      final ServerCertificate sc = new ServerCertificate(new byte[0], certificates);
      sc.write(handshakeBB);

      // create server cert verification
      final byte[] toVerify = peekToArray(handshakeBB);

      final byte[] verificationSig =
          CertificateVerify.sign(
              Hash.sha256(clientHello, serverHello, toVerify), privateKey, false);

      final ServerCertificateVerify scv = new ServerCertificateVerify(2052, verificationSig);
      scv.write(handshakeBB);

      // create server finished
      final byte[] peerPublicKey = keyShareExtension.getKey(Group.X25519).get();
      final byte[] sharedSecret = kek.generateSharedSecret(peerPublicKey);
      handshakeSecret = HKDF.calculateHandshakeSecret(sharedSecret);
      final byte[] helloHash = Hash.sha256(clientHello, serverHello);

      // create handshake AEAD
      final AEAD handshakeAEAD = HandshakeAEAD.create(handshakeSecret, helloHash, false);
      aeads.setHandshakeAead(handshakeAEAD);

      final byte[] serverHandshakeTrafficSecret =
          HKDF.expandLabel(handshakeSecret, "s hs traffic", helloHash, 32);

      // finished_hash = SHA256(Client Hello ... Server Cert Verify)
      final byte[] finishedHash = Hash.sha256(clientHello, serverHello, peekToArray(handshakeBB));

      final byte[] verifyData = VerifyData.create(serverHandshakeTrafficSecret, finishedHash);

      final Finished fin = new Finished(verifyData);
      fin.write(handshakeBB);

      // create 1-RTT AEAD
      handshake = Bytes.drainToArray(handshakeBB);

      final byte[] handshakeHash = Hash.sha256(clientHello, serverHello, handshake);
      final AEAD oneRttAEAD = OneRttAEAD.create(handshakeSecret, handshakeHash, false);
      aeads.setOneRttAead(oneRttAEAD);

      return new ServerHelloAndHandshake(serverHello, handshake);
    } finally {

    }
  }

  public synchronized void handleClientFinished(final byte[] msg) {
    if (clientHello == null || serverHello == null || handshake == null) {
      throw new IllegalStateException("Got handshake in unexpected state");
    }

    final ByteBuf bb = Unpooled.wrappedBuffer(msg);
    final Finished fin = Finished.parse(bb);

    final byte[] helloHash = Hash.sha256(clientHello, serverHello);

    final byte[] clientHandshakeTrafficSecret =
        HKDF.expandLabel(handshakeSecret, "c hs traffic", helloHash, 32);

    final byte[] handshakeHash = Hash.sha256(clientHello, serverHello, handshake);

    final boolean valid =
        VerifyData.verify(
            fin.getVerificationData(), clientHandshakeTrafficSecret, handshakeHash, false);

    if (!valid) {
      throw new RuntimeException("Invalid client verification");
    }
  }

  public AEAD getAEAD(final EncryptionLevel level) {
    return aeads.get(level);
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

    public ServerHelloAndHandshake(final byte[] serverHello, final byte[] serverHandshake) {
      this.serverHello = serverHello;
      this.serverHandshake = serverHandshake;
    }

    public byte[] getServerHello() {
      return serverHello;
    }

    public byte[] getServerHandshake() {
      return serverHandshake;
    }
  }
}
