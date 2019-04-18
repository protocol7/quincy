package com.protocol7.quincy.tls.extensions;

import static com.protocol7.quincy.tls.extensions.TransportParameterType.ACK_DELAY_EXPONENT;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.DISABLE_MIGRATION;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.IDLE_TIMEOUT;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.INITIAL_MAX_BIDI_STREAMS;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.INITIAL_MAX_DATA;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.INITIAL_MAX_STREAM_DATA_BIDI_LOCAL;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.INITIAL_MAX_STREAM_DATA_BIDI_REMOTE;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.INITIAL_MAX_STREAM_DATA_UNI;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.INITIAL_MAX_UNI_STREAMS;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.MAX_ACK_DELAY;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.MAX_PACKET_SIZE;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.ORIGINAL_CONNECTION_ID;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.STATELESS_RESET_TOKEN;

import com.google.common.collect.ImmutableList;
import com.protocol7.quincy.Varint;
import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TransportParameters implements Extension {

  public static class Builder {
    private final byte[] version;
    private List<byte[]> supportedVersions = Collections.emptyList();
    private int initialMaxStreamDataBidiLocal = -1;
    private int initialMaxData = -1;
    private int initialMaxBidiStreams = -1;
    private int idleTimeout = -1;
    // TODO PREFERRED_ADDRESS(4),
    private int maxPacketSize = -1;
    private byte[] statelessResetToken = new byte[0];
    private int ackDelayExponent = -1;
    private int initialMaxUniStreams = -1;
    private boolean disableMigration = false;
    private int initialMaxStreamDataBidiRemote = -1;
    private int initialMaxStreamDataUni = -1;
    private int maxAckDelay = -1;
    private byte[] originalConnectionId = new byte[0];

    public Builder(final byte[] version) {
      this.version = version;
    }

    public Builder withSupportedVersions(final List<byte[]> supportedVersions) {
      this.supportedVersions = supportedVersions;
      return this;
    }

    public Builder withInitialMaxStreamDataBidiLocal(final int initialMaxStreamDataBidiLocal) {
      this.initialMaxStreamDataBidiLocal = initialMaxStreamDataBidiLocal;
      return this;
    }

    public Builder withInitialMaxData(final int initialMaxData) {
      this.initialMaxData = initialMaxData;
      return this;
    }

    public Builder withInitialMaxBidiStreams(final int initialMaxBidiStreams) {
      this.initialMaxBidiStreams = initialMaxBidiStreams;
      return this;
    }

    public Builder withIdleTimeout(final int idleTimeout) {
      this.idleTimeout = idleTimeout;
      return this;
    }

    public Builder withMaxPacketSize(final int maxPacketSize) {
      this.maxPacketSize = maxPacketSize;
      return this;
    }

    public Builder withStatelessResetToken(final byte[] statelessResetToken) {
      this.statelessResetToken = statelessResetToken;
      return this;
    }

    public Builder withAckDelayExponent(final int ackDelayExponent) {
      this.ackDelayExponent = ackDelayExponent;
      return this;
    }

    public Builder withInitialMaxUniStreams(final int initialMaxUniStreams) {
      this.initialMaxUniStreams = initialMaxUniStreams;
      return this;
    }

    public Builder withDisableMigration(final boolean disableMigration) {
      this.disableMigration = disableMigration;
      return this;
    }

    public Builder withInitialMaxStreamDataBidiRemote(final int initialMaxStreamDataBidiRemote) {
      this.initialMaxStreamDataBidiRemote = initialMaxStreamDataBidiRemote;
      return this;
    }

    public Builder withInitialMaxStreamDataUni(final int initialMaxStreamDataUni) {
      this.initialMaxStreamDataUni = initialMaxStreamDataUni;
      return this;
    }

    public Builder withMaxAckDelay(final int maxAckDelay) {
      this.maxAckDelay = maxAckDelay;
      return this;
    }

    public Builder withOriginalConnectionId(final byte[] originalConnectionId) {
      this.originalConnectionId = originalConnectionId;
      return this;
    }

    public TransportParameters build() {
      return new TransportParameters(
          version,
          supportedVersions,
          initialMaxStreamDataBidiLocal,
          initialMaxData,
          initialMaxBidiStreams,
          idleTimeout,
          maxPacketSize,
          statelessResetToken,
          ackDelayExponent,
          initialMaxUniStreams,
          disableMigration,
          initialMaxStreamDataBidiRemote,
          initialMaxStreamDataUni,
          maxAckDelay,
          originalConnectionId);
    }
  }

  public static Builder newBuilder(final byte[] version) {
    return new Builder(version);
  }

  public static TransportParameters parse(final ByteBuf bb, final boolean isClient) {
    final byte[] version = new byte[4];
    bb.readBytes(version);
    final ImmutableList.Builder<byte[]> supportedVersions = ImmutableList.builder();

    if (isClient) {
      final int supportVerLen = bb.readByte() / 4;

      for (int i = 0; i < supportVerLen; i++) {
        final byte[] b = new byte[4];
        bb.readBytes(b);
        supportedVersions.add(b);
      }
    }

    final int bufLen = bb.readShort();
    final ByteBuf tpBB = bb.readBytes(bufLen);

    try {
      final Builder builder = new Builder(version);
      builder.withSupportedVersions(supportedVersions.build());

      while (tpBB.isReadable()) {
        final byte[] type = new byte[2];
        tpBB.readBytes(type);

        final int len = tpBB.readShort();

        final byte[] data = new byte[len];
        tpBB.readBytes(data);

        final TransportParameterType tp = TransportParameterType.fromValue(type);

        switch (tp) {
          case INITIAL_MAX_STREAM_DATA_BIDI_LOCAL:
            builder.withInitialMaxStreamDataBidiLocal(dataToInt(data));
            break;
          case INITIAL_MAX_DATA:
            builder.withInitialMaxData(dataToInt(data));
            break;
          case INITIAL_MAX_BIDI_STREAMS:
            builder.withInitialMaxBidiStreams(dataToShort(data));
            break;
          case IDLE_TIMEOUT:
            builder.withIdleTimeout(dataToShort(data));
            break;
          case MAX_PACKET_SIZE:
            builder.withMaxPacketSize(dataToShort(data));
            break;
          case STATELESS_RESET_TOKEN:
            builder.withStatelessResetToken(data);
            break;
          case ACK_DELAY_EXPONENT:
            builder.withAckDelayExponent(dataToByte(data));
            break;
          case INITIAL_MAX_UNI_STREAMS:
            builder.withInitialMaxUniStreams(dataToShort(data));
            break;
          case DISABLE_MIGRATION:
            builder.withDisableMigration(true);
            break;
          case INITIAL_MAX_STREAM_DATA_BIDI_REMOTE:
            builder.withInitialMaxStreamDataBidiRemote(dataToInt(data));
            break;
          case INITIAL_MAX_STREAM_DATA_UNI:
            builder.withInitialMaxStreamDataUni(dataToInt(data));
            break;
          case MAX_ACK_DELAY:
            builder.withMaxAckDelay(dataToByte(data));
            break;
          case ORIGINAL_CONNECTION_ID:
            builder.withOriginalConnectionId(data);
            break;
        }
      }
      return builder.build();
    } finally {
      tpBB.release();
    }
  }

  private static int dataToInt(final byte[] data) {
    return Varint.readAsInt(data);
  }

  private static int dataToShort(final byte[] data) {
    return Varint.readAsInt(data);
  }

  private static int dataToByte(final byte[] data) {
    return Varint.readAsInt(data);
  }

  private final byte[] version;
  private final List<byte[]> supportedVersions;
  private final int initialMaxStreamDataBidiLocal;
  private final int initialMaxData;
  private final int initialMaxBidiStreams;
  private final int idleTimeout;
  // TODO PREFERRED_ADDRESS(4),
  private final int maxPacketSize;
  private final byte[] statelessResetToken;
  private final int ackDelayExponent;
  private final int initialMaxUniStreams;
  private final boolean disableMigration;
  private final int initialMaxStreamDataBidiRemote;
  private final int initialMaxStreamDataUni;
  private final int maxAckDelay;
  private final byte[] originalConnectionId;

  private TransportParameters(
      final byte[] version,
      final List<byte[]> supportedVersions,
      final int initialMaxStreamDataBidiLocal,
      final int initialMaxData,
      final int initialMaxBidiStreams,
      final int idleTimeout,
      final int maxPacketSize,
      final byte[] statelessResetToken,
      final int ackDelayExponent,
      final int initialMaxUniStreams,
      final boolean disableMigration,
      final int initialMaxStreamDataBidiRemote,
      final int initialMaxStreamDataUni,
      final int maxAckDelay,
      final byte[] originalConnectionId) {
    this.version = version;
    this.supportedVersions = supportedVersions;
    this.initialMaxStreamDataBidiLocal = initialMaxStreamDataBidiLocal;
    this.initialMaxData = initialMaxData;
    this.initialMaxBidiStreams = initialMaxBidiStreams;
    this.idleTimeout = idleTimeout;
    this.maxPacketSize = maxPacketSize;
    this.statelessResetToken = statelessResetToken;
    this.ackDelayExponent = ackDelayExponent;
    this.initialMaxUniStreams = initialMaxUniStreams;
    this.disableMigration = disableMigration;
    this.initialMaxStreamDataBidiRemote = initialMaxStreamDataBidiRemote;
    this.initialMaxStreamDataUni = initialMaxStreamDataUni;
    this.maxAckDelay = maxAckDelay;
    this.originalConnectionId = originalConnectionId;
  }

  @Override
  public ExtensionType getType() {
    return ExtensionType.QUIC;
  }

  public byte[] getVersion() {
    return version;
  }

  public List<byte[]> getSupportedVersions() {
    return supportedVersions;
  }

  public int getInitialMaxStreamDataBidiLocal() {
    return initialMaxStreamDataBidiLocal;
  }

  public int getInitialMaxData() {
    return initialMaxData;
  }

  public int getInitialMaxBidiStreams() {
    return initialMaxBidiStreams;
  }

  public int getIdleTimeout() {
    return idleTimeout;
  }

  public int getMaxPacketSize() {
    return maxPacketSize;
  }

  public byte[] getStatelessResetToken() {
    return statelessResetToken;
  }

  public int getAckDelayExponent() {
    return ackDelayExponent;
  }

  public int getInitialMaxUniStreams() {
    return initialMaxUniStreams;
  }

  public boolean isDisableMigration() {
    return disableMigration;
  }

  public int getInitialMaxStreamDataBidiRemote() {
    return initialMaxStreamDataBidiRemote;
  }

  public int getInitialMaxStreamDataUni() {
    return initialMaxStreamDataUni;
  }

  public int getMaxAckDelay() {
    return maxAckDelay;
  }

  public byte[] getOriginalConnectionId() {
    return originalConnectionId;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final TransportParameters that = (TransportParameters) o;
    return initialMaxStreamDataBidiLocal == that.initialMaxStreamDataBidiLocal
        && initialMaxData == that.initialMaxData
        && initialMaxBidiStreams == that.initialMaxBidiStreams
        && idleTimeout == that.idleTimeout
        && maxPacketSize == that.maxPacketSize
        && ackDelayExponent == that.ackDelayExponent
        && initialMaxUniStreams == that.initialMaxUniStreams
        && disableMigration == that.disableMigration
        && initialMaxStreamDataBidiRemote == that.initialMaxStreamDataBidiRemote
        && initialMaxStreamDataUni == that.initialMaxStreamDataUni
        && maxAckDelay == that.maxAckDelay
        && Arrays.equals(version, that.version)
        && Objects.equals(supportedVersions, that.supportedVersions)
        && Arrays.equals(statelessResetToken, that.statelessResetToken)
        && Arrays.equals(originalConnectionId, that.originalConnectionId);
  }

  @Override
  public int hashCode() {
    int result =
        Objects.hash(
            supportedVersions,
            initialMaxStreamDataBidiLocal,
            initialMaxData,
            initialMaxBidiStreams,
            idleTimeout,
            maxPacketSize,
            ackDelayExponent,
            initialMaxUniStreams,
            disableMigration,
            initialMaxStreamDataBidiRemote,
            initialMaxStreamDataUni,
            maxAckDelay);
    result = 31 * result + Arrays.hashCode(version);
    result = 31 * result + Arrays.hashCode(statelessResetToken);
    result = 31 * result + Arrays.hashCode(originalConnectionId);
    return result;
  }

  @Override
  public String toString() {
    return "TransportParameters{"
        + "version="
        + Hex.hex(version)
        + "suppoerdVersions="
        + supportedVersions
        + "initialMaxStreamDataBidiLocal="
        + initialMaxStreamDataBidiLocal
        + ", initialMaxData="
        + initialMaxData
        + ", initialMaxBidiStreams="
        + initialMaxBidiStreams
        + ", idleTimeout="
        + idleTimeout
        + ", maxPacketSize="
        + maxPacketSize
        + ", statelessResetToken="
        + Arrays.toString(statelessResetToken)
        + ", ackDelayExponent="
        + ackDelayExponent
        + ", initialMaxUniStreams="
        + initialMaxUniStreams
        + ", disableMigration="
        + disableMigration
        + ", initialMaxStreamDataBidiRemote="
        + initialMaxStreamDataBidiRemote
        + ", initialMaxStreamDataUni="
        + initialMaxStreamDataUni
        + ", maxAckDelay="
        + maxAckDelay
        + ", originalConnectionId="
        + Arrays.toString(originalConnectionId)
        + '}';
  }

  public void write(final ByteBuf bb, final boolean isClient) {
    bb.writeBytes(version);

    if (!isClient) {
      bb.writeByte(supportedVersions.size() * 4);
      for (final byte[] supportedVersion : supportedVersions) {
        bb.writeBytes(supportedVersion);
      }
    } else {
      if (!supportedVersions.isEmpty()) {
        throw new IllegalStateException("Supported version can not be set for clients");
      }
    }

    final int lenPos = bb.writerIndex();
    bb.writeShort(0);

    if (initialMaxStreamDataBidiLocal > -1) {
      bb.writeBytes(INITIAL_MAX_STREAM_DATA_BIDI_LOCAL.asBytes());
      writeVarint(bb, initialMaxStreamDataBidiLocal);
    }
    if (initialMaxData > -1) {
      bb.writeBytes(INITIAL_MAX_DATA.asBytes());
      writeVarint(bb, initialMaxData);
    }
    if (initialMaxBidiStreams > -1) {
      bb.writeBytes(INITIAL_MAX_BIDI_STREAMS.asBytes());
      writeVarint(bb, initialMaxBidiStreams);
    }
    if (idleTimeout > -1) {
      bb.writeBytes(IDLE_TIMEOUT.asBytes());
      writeVarint(bb, idleTimeout);
    }
    if (maxPacketSize > -1) {
      bb.writeBytes(MAX_PACKET_SIZE.asBytes());
      writeVarint(bb, maxPacketSize);
    }
    if (statelessResetToken.length > 0) {
      bb.writeBytes(STATELESS_RESET_TOKEN.asBytes());
      bb.writeShort(statelessResetToken.length);
      bb.writeBytes(statelessResetToken);
    }
    if (ackDelayExponent > -1) {
      bb.writeBytes(ACK_DELAY_EXPONENT.asBytes());
      writeVarint(bb, ackDelayExponent);
    }
    if (initialMaxUniStreams > -1) {
      bb.writeBytes(INITIAL_MAX_UNI_STREAMS.asBytes());
      writeVarint(bb, initialMaxUniStreams);
    }
    if (disableMigration) {
      bb.writeBytes(DISABLE_MIGRATION.asBytes());
      bb.writeShort(0);
    }
    if (initialMaxStreamDataBidiRemote > -1) {
      bb.writeBytes(INITIAL_MAX_STREAM_DATA_BIDI_REMOTE.asBytes());
      writeVarint(bb, initialMaxStreamDataBidiRemote);
    }
    if (initialMaxStreamDataUni > -1) {
      bb.writeBytes(INITIAL_MAX_STREAM_DATA_UNI.asBytes());
      writeVarint(bb, initialMaxStreamDataUni);
    }
    if (maxAckDelay > -1) {
      bb.writeBytes(MAX_ACK_DELAY.asBytes());
      writeVarint(bb, maxAckDelay);
    }
    if (originalConnectionId.length > 0) {
      bb.writeBytes(ORIGINAL_CONNECTION_ID.asBytes());
      bb.writeShort(originalConnectionId.length);
      bb.writeBytes(originalConnectionId);
    }

    bb.setShort(lenPos, bb.writerIndex() - lenPos - 2);
  }

  private void writeVarint(final ByteBuf bb, final int value) {
    final byte[] b = Varint.write(value);
    bb.writeShort(b.length);
    bb.writeBytes(b);
  }
}
