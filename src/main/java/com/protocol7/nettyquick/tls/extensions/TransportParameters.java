package com.protocol7.nettyquick.tls.extensions;

import static com.protocol7.nettyquick.tls.extensions.TransportParameterType.*;

import com.protocol7.nettyquick.protocol.Varint;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Objects;

public class TransportParameters implements Extension {

  public static class Builder {
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

    public Builder withInitialMaxStreamDataBidiLocal(int initialMaxStreamDataBidiLocal) {
      this.initialMaxStreamDataBidiLocal = initialMaxStreamDataBidiLocal;
      return this;
    }

    public Builder withInitialMaxData(int initialMaxData) {
      this.initialMaxData = initialMaxData;
      return this;
    }

    public Builder withInitialMaxBidiStreams(int initialMaxBidiStreams) {
      this.initialMaxBidiStreams = initialMaxBidiStreams;
      return this;
    }

    public Builder withIdleTimeout(int idleTimeout) {
      this.idleTimeout = idleTimeout;
      return this;
    }

    public Builder withMaxPacketSize(int maxPacketSize) {
      this.maxPacketSize = maxPacketSize;
      return this;
    }

    public Builder withStatelessResetToken(byte[] statelessResetToken) {
      this.statelessResetToken = statelessResetToken;
      return this;
    }

    public Builder withAckDelayExponent(int ackDelayExponent) {
      this.ackDelayExponent = ackDelayExponent;
      return this;
    }

    public Builder withInitialMaxUniStreams(int initialMaxUniStreams) {
      this.initialMaxUniStreams = initialMaxUniStreams;
      return this;
    }

    public Builder withDisableMigration(boolean disableMigration) {
      this.disableMigration = disableMigration;
      return this;
    }

    public Builder withInitialMaxStreamDataBidiRemote(int initialMaxStreamDataBidiRemote) {
      this.initialMaxStreamDataBidiRemote = initialMaxStreamDataBidiRemote;
      return this;
    }

    public Builder withInitialMaxStreamDataUni(int initialMaxStreamDataUni) {
      this.initialMaxStreamDataUni = initialMaxStreamDataUni;
      return this;
    }

    public Builder withMaxAckDelay(int maxAckDelay) {
      this.maxAckDelay = maxAckDelay;
      return this;
    }

    public Builder withOriginalConnectionId(byte[] originalConnectionId) {
      this.originalConnectionId = originalConnectionId;
      return this;
    }

    public TransportParameters build() {
      return new TransportParameters(
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

  public static Builder newBuilder() {
    return new Builder();
  }

  public static TransportParameters defaults() {
    return new Builder()
        .withInitialMaxStreamDataBidiLocal(32768)
        .withInitialMaxData(49152)
        .withInitialMaxBidiStreams(100)
        .withIdleTimeout(30)
        .withMaxPacketSize(1452)
        .withInitialMaxUniStreams(100)
        .withDisableMigration(true)
        .withInitialMaxStreamDataBidiRemote(32768)
        .withInitialMaxStreamDataUni(32768)
        .build();
  }

  public static TransportParameters parse(ByteBuf bb) {
    byte[] version = new byte[4];
    bb.readBytes(version); // TODO validate version

    if (Arrays.equals(version, new byte[] {0, 0, 0, 0x65})) {
      int xLen = bb.readByte();
      byte[] x = new byte[xLen];
      bb.readBytes(x); // TODO figure out
    }

    int bufLen = bb.readShort();
    ByteBuf tpBB = bb.readBytes(bufLen);

    Builder builder = new Builder();

    while (tpBB.isReadable()) {
      byte[] type = new byte[2];
      tpBB.readBytes(type);

      int len = tpBB.readShort();

      byte[] data = new byte[len];
      tpBB.readBytes(data);

      TransportParameterType tp = TransportParameterType.fromValue(type);

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
  }

  private static int dataToInt(byte[] data) {
    return Varint.readAsInt(data);
  }

  private static int dataToShort(byte[] data) {
    return Varint.readAsInt(data);
  }

  private static int dataToByte(byte[] data) {
    return Varint.readAsInt(data);
  }

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
      int initialMaxStreamDataBidiLocal,
      int initialMaxData,
      int initialMaxBidiStreams,
      int idleTimeout,
      int maxPacketSize,
      byte[] statelessResetToken,
      int ackDelayExponent,
      int initialMaxUniStreams,
      boolean disableMigration,
      int initialMaxStreamDataBidiRemote,
      int initialMaxStreamDataUni,
      int maxAckDelay,
      byte[] originalConnectionId) {
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TransportParameters that = (TransportParameters) o;
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
        && Arrays.equals(statelessResetToken, that.statelessResetToken)
        && Arrays.equals(originalConnectionId, that.originalConnectionId);
  }

  @Override
  public int hashCode() {
    int result =
        Objects.hash(
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
    result = 31 * result + Arrays.hashCode(statelessResetToken);
    result = 31 * result + Arrays.hashCode(originalConnectionId);
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

  public void write(ByteBuf bb, boolean isClient) {

    bb.writeBytes(new byte[] {00, 00, 00, 00}); // version
    int lenPos = bb.writerIndex();
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

  private void writeVarint(ByteBuf bb, int value) {
    byte[] b = Varint.write(value);
    bb.writeShort(b.length);
    bb.writeBytes(b);
  }
}
