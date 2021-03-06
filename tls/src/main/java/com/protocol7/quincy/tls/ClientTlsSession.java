package com.protocol7.quincy.tls;

import static com.protocol7.quincy.tls.aead.Labels.CLIENT_HANDSHAKE_TRAFFIC_SECRET;

import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.AEADs;
import com.protocol7.quincy.tls.aead.HandshakeAEAD;
import com.protocol7.quincy.tls.aead.OneRttAEAD;
import com.protocol7.quincy.tls.extensions.ALPN;
import com.protocol7.quincy.tls.extensions.Extension;
import com.protocol7.quincy.tls.extensions.ExtensionType;
import com.protocol7.quincy.tls.extensions.KeyShare;
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
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientTlsSession {

  public static class CertificateInvalidException extends Exception {}

  private final Logger log = LoggerFactory.getLogger(ClientTlsSession.class);

  private final TransportParameters transportParametersDefaults;

  private final AEADs aeads;
  private final KeyExchange kek;
  private final CertificateValidator certificateValidator;
  private final List<String> applicationProtocols;

  private ReceivedDataBuffer handshakeBuffer = new ReceivedDataBuffer();
  private byte[] clientHello;
  private byte[] serverHello;
  private byte[] handshakeSecret;

  public ClientTlsSession(
      final AEAD initialAEAD,
      final List<String> applicationProtocols,
      final TransportParameters transportParametersDefaults,
      final CertificateValidator certificateValidator) {

    this.applicationProtocols = applicationProtocols;
    this.transportParametersDefaults = transportParametersDefaults;

    aeads = new AEADs(initialAEAD);
    this.certificateValidator = certificateValidator;
    kek = KeyExchange.generate(Group.X25519);
  }

  public byte[] startHandshake(final byte[] sourceConnectionId) {
    if (clientHello != null) {
      throw new IllegalStateException("Already started");
    }

    final List<Extension> extensions = new ArrayList<>();
    final TransportParameters transportParameters =
        TransportParameters.newBuilder(transportParametersDefaults)
            .withInitialSourceConnectionId(sourceConnectionId)
            .build();

    extensions.add(transportParameters);

    if (!applicationProtocols.isEmpty()) {
      extensions.add(new ALPN(applicationProtocols));
    }

    final ClientHello ch = ClientHello.defaults(kek, extensions);
    clientHello = Bytes.write(bb -> ch.write(bb, true));
    return clientHello;
  }

  public void handleServerHello(final ByteBuf msg) {
    if (clientHello == null) {
      throw new IllegalStateException("Not started");
    }

    serverHello = Bytes.peekToArray(msg);

    final ServerHello hello = ServerHello.parse(msg, true);

    final SupportedVersions version =
        (SupportedVersions)
            hello
                .geExtension(ExtensionType.SUPPORTED_VERSIONS)
                .orElseThrow(IllegalArgumentException::new);
    if (!version.equals(SupportedVersions.TLS13)) {
      throw new IllegalArgumentException("Illegal version");
    }

    final KeyShare keyShareExtension =
        (KeyShare)
            hello.geExtension(ExtensionType.KEY_SHARE).orElseThrow(IllegalArgumentException::new);
    final byte[] peerPublicKey = keyShareExtension.getKey(Group.X25519).get();
    final byte[] sharedSecret = kek.generateSharedSecret(peerPublicKey);

    final byte[] helloHash = Hash.sha256(clientHello, serverHello);

    handshakeSecret = HKDF.calculateHandshakeSecret(sharedSecret);

    aeads.setHandshakeAead(HandshakeAEAD.create(handshakeSecret, helloHash, true));
  }

  public synchronized Optional<byte[]> handleHandshake(final byte[] msg, final long offset)
      throws CertificateInvalidException {
    if (clientHello == null || serverHello == null) {
      throw new IllegalStateException("Got handshake in unexpected state");
    }

    // TODO handle out of order
    handshakeBuffer.onData(msg, offset);

    final Optional<ByteBuf> optBuf = handshakeBuffer.read();
    if (optBuf.isEmpty()) {
      return Optional.empty();
    }
    final ByteBuf buffer = optBuf.get();

    buffer.markReaderIndex();
    try {
      final int pos = buffer.readerIndex();
      EncryptedExtensions.parse(buffer, true);
      final ServerCertificate sc = ServerCertificate.parse(buffer);

      final byte[] scvBytes = new byte[buffer.readerIndex() - pos];
      buffer.resetReaderIndex();
      buffer.readBytes(scvBytes);

      final ServerCertificateVerify scv = ServerCertificateVerify.parse(buffer);

      validateServerCertificateVerify(sc, scv, scvBytes);

      final byte[] finBytes = new byte[buffer.readerIndex() - pos];
      buffer.resetReaderIndex();
      buffer.readBytes(finBytes);

      final Finished fin = Finished.parse(buffer);

      final byte[] helloHash = Hash.sha256(clientHello, serverHello);
      validateServerFinish(fin, helloHash, finBytes);

      if (!certificateValidator.validate(sc.getServerCertificates())) {
        throw new CertificateInvalidException();
      }

      buffer.resetReaderIndex();
      final byte[] hs = Bytes.peekToArray(buffer);
      handshakeBuffer = new ReceivedDataBuffer();

      final byte[] handshakeHash = Hash.sha256(clientHello, serverHello, hs);

      final AEAD oneRttAead = OneRttAEAD.create(handshakeSecret, handshakeHash, true);
      aeads.setOneRttAead(oneRttAead);

      // TODO dedup
      final byte[] clientHandshakeTrafficSecret =
          HKDF.expandLabel(handshakeSecret, CLIENT_HANDSHAKE_TRAFFIC_SECRET, helloHash, 32);

      final Finished clientFinished =
          Finished.createClientFinished(clientHandshakeTrafficSecret, handshakeHash);

      final byte[] clientFin = Bytes.write(clientFinished);

      return Optional.of(clientFin);
    } catch (final IndexOutOfBoundsException e) {
      // wait for more data

      return Optional.empty();
    } finally {
      buffer.release();
    }
  }

  private void validateServerFinish(
      final Finished fin, final byte[] helloHash, final byte[] finBytes) {
    // verify server fin
    final byte[] finishedHash = Hash.sha256(clientHello, serverHello, finBytes);

    final byte[] serverHandshakeTrafficSecret =
        HKDF.expandLabel(handshakeSecret, "s hs traffic", helloHash, 32);

    final boolean valid =
        VerifyData.verify(
            fin.getVerificationData(), serverHandshakeTrafficSecret, finishedHash, false);
    if (!valid) {
      throw new RuntimeException("Server verification data not valid");
    }
  }

  private void validateServerCertificateVerify(
      final ServerCertificate sc, final ServerCertificateVerify scv, final byte[] handshakeData) {
    final byte[] toVerify = Hash.sha256(clientHello, serverHello, handshakeData);

    final byte[] serverSig = scv.getSignature();

    final PublicKey serverKey = sc.getAsCertificiates().get(0).getPublicKey();

    final boolean valid = CertificateVerify.verify(serverSig, toVerify, serverKey, false);
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

  public void unsetInitialAead() {
    aeads.unsetInitialAead();
  }

  public void unsetHandshakeAead() {
    aeads.unsetHandshakeAead();
  }
}
