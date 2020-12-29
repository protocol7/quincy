package com.protocol7.quincy.tls.extensions;

import static com.protocol7.quincy.tls.extensions.TransportParameterType.ACK_DELAY_EXPONENT;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.ACTIVE_CONNECTION_ID_LIMIT;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.DISABLE_ACTIVE_MIGRATION;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.INITIAL_MAX_DATA;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.INITIAL_MAX_STREAMS_BIDI;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.INITIAL_MAX_STREAMS_UNI;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.INITIAL_MAX_STREAM_DATA_BIDI_LOCAL;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.INITIAL_MAX_STREAM_DATA_BIDI_REMOTE;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.INITIAL_MAX_STREAM_DATA_UNI;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.INITIAL_SOURCE_CONNECTION_ID;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.MAX_ACK_DELAY;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.MAX_IDLE_TIMEOUT;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.MAX_UDP_PACKET_SIZE;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.ORIGINAL_DESTINATION_CONNECTION_ID;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.RETRY_SOURCE_CONNECTION_ID;
import static com.protocol7.quincy.tls.extensions.TransportParameterType.STATELESS_RESET_TOKEN;

import com.protocol7.quincy.Varint;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Objects;

public class TransportParameters implements Extension {

  public static class Builder {
    private int initialMaxStreamDataBidiLocal = -1;
    private int initialMaxData = -1;
    private int initialMaxStreamsBidi = -1;
    private int idleTimeout = -1;
    // TODO PREFERRED_ADDRESS(4),
    private int maxUDPPacketSize = -1;
    private byte[] statelessResetToken = new byte[0];
    private int ackDelayExponent = -1;
    private int initialMaxStreamsUni = -1;
    private boolean disableActiveMigration = false;
    private int initialMaxStreamDataBidiRemote = -1;
    private int initialMaxStreamDataUni = -1;
    private int maxAckDelay = -1;
    private byte[] originalDestinationConnectionId = new byte[0];
    private int activeConnectionIdLimit = -1;
    private byte[] initialSourceConnectionId = new byte[0];
    private byte[] retrySourceConnectionId = new byte[0];

    public Builder() {}

    public Builder(final TransportParameters transportParameters) {
      this.initialMaxStreamDataBidiLocal = transportParameters.initialMaxStreamDataBidiLocal;
      this.initialMaxData = transportParameters.initialMaxData;
      this.initialMaxStreamsBidi = transportParameters.initialMaxStreamsBidi;
      this.idleTimeout = transportParameters.idleTimeout;
      // TODO PREFERRED_ADDRESS(4),
      this.maxUDPPacketSize = transportParameters.maxUDPPacketSize;
      this.statelessResetToken = transportParameters.statelessResetToken;
      this.ackDelayExponent = transportParameters.ackDelayExponent;
      this.initialMaxStreamsUni = transportParameters.initialMaxStreamsUni;
      this.disableActiveMigration = transportParameters.disableActiveMigration;
      this.initialMaxStreamDataBidiRemote = transportParameters.initialMaxStreamDataBidiRemote;
      this.initialMaxStreamDataUni = transportParameters.initialMaxStreamDataUni;
      this.maxAckDelay = transportParameters.maxAckDelay;
      this.originalDestinationConnectionId = transportParameters.originalDestinationConnectionId;
      this.activeConnectionIdLimit = transportParameters.activeConnectionIdLimit;
      this.initialSourceConnectionId = transportParameters.initialSourceConnectionId;
      this.retrySourceConnectionId = transportParameters.retrySourceConnectionId;
    }

    public Builder withInitialMaxStreamDataBidiLocal(final int initialMaxStreamDataBidiLocal) {
      this.initialMaxStreamDataBidiLocal = initialMaxStreamDataBidiLocal;
      return this;
    }

    public Builder withInitialMaxData(final int initialMaxData) {
      this.initialMaxData = initialMaxData;
      return this;
    }

    public Builder withInitialMaxStreamsBidi(final int initialMaxStreamsBidi) {
      this.initialMaxStreamsBidi = initialMaxStreamsBidi;
      return this;
    }

    public Builder withMaxIdleTimeout(final int idleTimeout) {
      this.idleTimeout = idleTimeout;
      return this;
    }

    public Builder withMaxUDPPacketSize(final int maxUDPPacketSize) {
      this.maxUDPPacketSize = maxUDPPacketSize;
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

    public Builder withInitialMaxStreamsUni(final int initialMaxStreamsUni) {
      this.initialMaxStreamsUni = initialMaxStreamsUni;
      return this;
    }

    public Builder withDisableActiveMigration(final boolean disableActiveMigration) {
      this.disableActiveMigration = disableActiveMigration;
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

    public Builder withOriginalDestinationConnectionId(
        final byte[] originalDestinationConnectionId) {
      this.originalDestinationConnectionId = originalDestinationConnectionId;
      return this;
    }

    public Builder withActiveConnectionIdLimit(final int activeConnectionIdLimit) {
      this.activeConnectionIdLimit = activeConnectionIdLimit;
      return this;
    }

    public Builder withInitialSourceConnectionId(final byte[] initialSourceConnectionId) {
      this.initialSourceConnectionId = initialSourceConnectionId;
      return this;
    }

    public Builder withRetrySourceConnectionId(final byte[] retrySourceConnectionId) {
      this.retrySourceConnectionId = retrySourceConnectionId;
      return this;
    }

    public TransportParameters build() {
      return new TransportParameters(
          initialMaxStreamDataBidiLocal,
          initialMaxData,
          initialMaxStreamsBidi,
          idleTimeout,
          maxUDPPacketSize,
          statelessResetToken,
          ackDelayExponent,
          initialMaxStreamsUni,
          disableActiveMigration,
          initialMaxStreamDataBidiRemote,
          initialMaxStreamDataUni,
          maxAckDelay,
          originalDestinationConnectionId,
          activeConnectionIdLimit,
          initialSourceConnectionId,
          retrySourceConnectionId);
    }
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Builder newBuilder(final TransportParameters transportParameters) {
    return new Builder(transportParameters);
  }

  public static TransportParameters parse(final ByteBuf bb) {

    final Builder builder = new Builder();

    while (bb.isReadable()) {
      final int type = Varint.readAsInt(bb);
      final int len = Varint.readAsInt(bb);

      final byte[] data = new byte[len];
      bb.readBytes(data);

      final TransportParameterType tp = TransportParameterType.fromValue(type);

      switch (tp) {
        case INITIAL_MAX_STREAM_DATA_BIDI_LOCAL:
          builder.withInitialMaxStreamDataBidiLocal(dataToInt(data));
          break;
        case INITIAL_MAX_DATA:
          builder.withInitialMaxData(dataToInt(data));
          break;
        case INITIAL_MAX_STREAMS_BIDI:
          builder.withInitialMaxStreamsBidi(dataToShort(data));
          break;
        case MAX_IDLE_TIMEOUT:
          builder.withMaxIdleTimeout(dataToShort(data));
          break;
        case MAX_UDP_PACKET_SIZE:
          builder.withMaxUDPPacketSize(dataToShort(data));
          break;
        case STATELESS_RESET_TOKEN:
          builder.withStatelessResetToken(data);
          break;
        case ACK_DELAY_EXPONENT:
          builder.withAckDelayExponent(dataToByte(data));
          break;
        case INITIAL_MAX_STREAMS_UNI:
          builder.withInitialMaxStreamsUni(dataToShort(data));
          break;
        case DISABLE_ACTIVE_MIGRATION:
          builder.withDisableActiveMigration(true);
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
        case ORIGINAL_DESTINATION_CONNECTION_ID:
          builder.withOriginalDestinationConnectionId(data);
          break;
        case ACTIVE_CONNECTION_ID_LIMIT:
          builder.withActiveConnectionIdLimit(dataToInt(data));
          break;
        case INITIAL_SOURCE_CONNECTION_ID:
          builder.withInitialSourceConnectionId(data);
          break;
        case RETRY_SOURCE_CONNECTION_ID:
          builder.withRetrySourceConnectionId(data);
          break;
      }
    }
    return builder.build();
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

  private final int initialMaxStreamDataBidiLocal;
  private final int initialMaxData;
  private final int initialMaxStreamsBidi;
  private final int idleTimeout;
  // TODO PREFERRED_ADDRESS(4),
  private final int maxUDPPacketSize;
  private final byte[] statelessResetToken;
  private final int ackDelayExponent;
  private final int initialMaxStreamsUni;
  private final boolean disableActiveMigration;
  private final int initialMaxStreamDataBidiRemote;
  private final int initialMaxStreamDataUni;
  private final int maxAckDelay;
  private final byte[] originalDestinationConnectionId;
  private final int activeConnectionIdLimit;
  private final byte[] initialSourceConnectionId;
  private final byte[] retrySourceConnectionId;

  private TransportParameters(
      final int initialMaxStreamDataBidiLocal,
      final int initialMaxData,
      final int initialMaxStreamsBidi,
      final int idleTimeout,
      final int maxUDPPacketSize,
      final byte[] statelessResetToken,
      final int ackDelayExponent,
      final int initialMaxStreamsUni,
      final boolean disableActiveMigration,
      final int initialMaxStreamDataBidiRemote,
      final int initialMaxStreamDataUni,
      final int maxAckDelay,
      final byte[] originalDestinationConnectionId,
      final int activeConnectionIdLimit,
      final byte[] initialSourceConnectionId,
      final byte[] retrySourceConnectionId) {
    this.initialMaxStreamDataBidiLocal = initialMaxStreamDataBidiLocal;
    this.initialMaxData = initialMaxData;
    this.initialMaxStreamsBidi = initialMaxStreamsBidi;
    this.idleTimeout = idleTimeout;
    this.maxUDPPacketSize = maxUDPPacketSize;
    this.statelessResetToken = statelessResetToken;
    this.ackDelayExponent = ackDelayExponent;
    this.initialMaxStreamsUni = initialMaxStreamsUni;
    this.disableActiveMigration = disableActiveMigration;
    this.initialMaxStreamDataBidiRemote = initialMaxStreamDataBidiRemote;
    this.initialMaxStreamDataUni = initialMaxStreamDataUni;
    this.maxAckDelay = maxAckDelay;
    this.originalDestinationConnectionId = originalDestinationConnectionId;
    this.activeConnectionIdLimit = activeConnectionIdLimit;
    this.initialSourceConnectionId = initialSourceConnectionId;
    this.retrySourceConnectionId = retrySourceConnectionId;
  }

  @Override
  public ExtensionType getType() {
    return ExtensionType.QUIC;
  }

  public int getInitialMaxStreamDataBidiLocal() {
    return initialMaxStreamDataBidiLocal;
  }

  public int getInitialMaxData() {
    return initialMaxData;
  }

  public int getInitialMaxStreamsBidi() {
    return initialMaxStreamsBidi;
  }

  public int getIdleTimeout() {
    return idleTimeout;
  }

  public int getMaxUDPPacketSize() {
    return maxUDPPacketSize;
  }

  public byte[] getStatelessResetToken() {
    return statelessResetToken;
  }

  public int getAckDelayExponent() {
    return ackDelayExponent;
  }

  public int getInitialMaxStreamsUni() {
    return initialMaxStreamsUni;
  }

  public boolean isDisableActiveMigration() {
    return disableActiveMigration;
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

  public byte[] getOriginalDestinationConnectionId() {
    return originalDestinationConnectionId;
  }

  public int getActiveConnectionIdLimit() {
    return activeConnectionIdLimit;
  }

  public byte[] getInitialSourceConnectionId() {
    return initialSourceConnectionId;
  }

  public byte[] getRetrySourceConnectionId() {
    return retrySourceConnectionId;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final TransportParameters that = (TransportParameters) o;
    return initialMaxStreamDataBidiLocal == that.initialMaxStreamDataBidiLocal
        && initialMaxData == that.initialMaxData
        && initialMaxStreamsBidi == that.initialMaxStreamsBidi
        && idleTimeout == that.idleTimeout
        && maxUDPPacketSize == that.maxUDPPacketSize
        && ackDelayExponent == that.ackDelayExponent
        && initialMaxStreamsUni == that.initialMaxStreamsUni
        && disableActiveMigration == that.disableActiveMigration
        && initialMaxStreamDataBidiRemote == that.initialMaxStreamDataBidiRemote
        && initialMaxStreamDataUni == that.initialMaxStreamDataUni
        && maxAckDelay == that.maxAckDelay
        && Arrays.equals(statelessResetToken, that.statelessResetToken)
        && Arrays.equals(originalDestinationConnectionId, that.originalDestinationConnectionId)
        && activeConnectionIdLimit == that.activeConnectionIdLimit
        && Arrays.equals(initialSourceConnectionId, that.initialSourceConnectionId)
        && Arrays.equals(retrySourceConnectionId, that.retrySourceConnectionId);
  }

  @Override
  public int hashCode() {
    int result =
        Objects.hash(
            initialMaxStreamDataBidiLocal,
            initialMaxData,
            initialMaxStreamsBidi,
            idleTimeout,
            maxUDPPacketSize,
            ackDelayExponent,
            initialMaxStreamsUni,
            disableActiveMigration,
            initialMaxStreamDataBidiRemote,
            initialMaxStreamDataUni,
            maxAckDelay,
            activeConnectionIdLimit);
    result = 31 * result + Arrays.hashCode(statelessResetToken);
    result = 31 * result + Arrays.hashCode(originalDestinationConnectionId);
    result = 31 * result + Arrays.hashCode(initialSourceConnectionId);
    result = 31 * result + Arrays.hashCode(retrySourceConnectionId);
    return result;
  }

  @Override
  public String toString() {
    return "TransportParameters{"
        + "initialMaxStreamDataBidiLocal="
        + initialMaxStreamDataBidiLocal
        + ", initialMaxData="
        + initialMaxData
        + ", initialMaxBidiStreams="
        + initialMaxStreamsBidi
        + ", idleTimeout="
        + idleTimeout
        + ", maxPacketSize="
        + maxUDPPacketSize
        + ", statelessResetToken="
        + Arrays.toString(statelessResetToken)
        + ", ackDelayExponent="
        + ackDelayExponent
        + ", initialMaxUniStreams="
        + initialMaxStreamsUni
        + ", disableMigration="
        + disableActiveMigration
        + ", initialMaxStreamDataBidiRemote="
        + initialMaxStreamDataBidiRemote
        + ", initialMaxStreamDataUni="
        + initialMaxStreamDataUni
        + ", maxAckDelay="
        + maxAckDelay
        + ", originalConnectionId="
        + Arrays.toString(originalDestinationConnectionId)
        + ", activeConnectionIdLimit="
        + activeConnectionIdLimit
        + ", initialSourceConnectionId="
        + Arrays.toString(initialSourceConnectionId)
        + ", retrySourceConnectionId="
        + Arrays.toString(retrySourceConnectionId)
        + '}';
  }

  public void write(final ByteBuf bb, final boolean isClient) {

    if (initialMaxStreamDataBidiLocal > -1) {
      writeVarint(INITIAL_MAX_STREAM_DATA_BIDI_LOCAL, bb, initialMaxStreamDataBidiLocal);
    }
    if (initialMaxData > -1) {
      writeVarint(INITIAL_MAX_DATA, bb, initialMaxData);
    }
    if (initialMaxStreamsBidi > -1) {
      writeVarint(INITIAL_MAX_STREAMS_BIDI, bb, initialMaxStreamsBidi);
    }
    if (idleTimeout > -1) {
      writeVarint(MAX_IDLE_TIMEOUT, bb, idleTimeout);
    }
    if (maxUDPPacketSize > -1) {
      writeVarint(MAX_UDP_PACKET_SIZE, bb, maxUDPPacketSize);
    }
    if (statelessResetToken.length > 0) {
      writeBytes(STATELESS_RESET_TOKEN, bb, statelessResetToken);
    }
    if (ackDelayExponent > -1) {
      writeVarint(ACK_DELAY_EXPONENT, bb, ackDelayExponent);
    }
    if (initialMaxStreamsUni > -1) {
      writeVarint(INITIAL_MAX_STREAMS_UNI, bb, initialMaxStreamsUni);
    }
    if (disableActiveMigration) {
      writeVarint(DISABLE_ACTIVE_MIGRATION, bb, 0);
    }
    if (initialMaxStreamDataBidiRemote > -1) {
      writeVarint(INITIAL_MAX_STREAM_DATA_BIDI_REMOTE, bb, initialMaxStreamDataBidiRemote);
    }
    if (initialMaxStreamDataUni > -1) {
      writeVarint(INITIAL_MAX_STREAM_DATA_UNI, bb, initialMaxStreamDataUni);
    }
    if (maxAckDelay > -1) {
      writeVarint(MAX_ACK_DELAY, bb, maxAckDelay);
    }
    if (originalDestinationConnectionId.length > 0) {
      writeBytes(ORIGINAL_DESTINATION_CONNECTION_ID, bb, originalDestinationConnectionId);
    }
    if (activeConnectionIdLimit > -1) {
      writeVarint(ACTIVE_CONNECTION_ID_LIMIT, bb, activeConnectionIdLimit);
    }
    if (initialSourceConnectionId.length > 0) {
      writeBytes(INITIAL_SOURCE_CONNECTION_ID, bb, initialSourceConnectionId);
    }
    if (retrySourceConnectionId.length > 0) {
      writeBytes(RETRY_SOURCE_CONNECTION_ID, bb, retrySourceConnectionId);
    }
  }

  private void writeBytes(final TransportParameterType type, final ByteBuf bb, final byte[] b) {
    Varint.write(type.getValue(), bb);
    Varint.write(b.length, bb);
    bb.writeBytes(b);
  }

  private void writeVarint(final TransportParameterType type, final ByteBuf bb, final int value) {
    Varint.write(type.getValue(), bb);
    final byte[] b = Varint.write(value);
    Varint.write(b.length, bb);
    bb.writeBytes(b);
  }
}
