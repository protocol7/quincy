package com.protocol7.nettyquick.tls.extensions;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;
import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.Objects;

import static com.protocol7.nettyquick.tls.extensions.TransportParameterType.*;

public class TransportParameters implements Extension {

    public static class Builder {
        private int initialMaxStreamDataBidiLocal = -1;
        private int initialMaxData = -1;
        private int initialMaxBidiStreams = -1;
        private int idleTimeout = -1;
        // TODO preferred_address(4),
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
            return new TransportParameters(initialMaxStreamDataBidiLocal,
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
        bb.readBytes(new byte[6]); // TODO figure out

        Builder builder = new Builder();

        while (bb.isReadable()) {
            byte[] type = new byte[2];
            bb.readBytes(type);
            int len = bb.readShort();
            byte[] data = new byte[len];
            bb.readBytes(data);

            TransportParameterType tp = TransportParameterType.fromValue(type);

            switch (tp) {
                case initial_max_stream_data_bidi_local:
                    builder.withInitialMaxStreamDataBidiLocal(dataToInt(data));
                    break;
                case initial_max_data:
                    builder.withInitialMaxData(dataToInt(data));
                    break;
                case initial_max_bidi_streams:
                    builder.withInitialMaxBidiStreams(dataToShort(data));
                    break;
                case idle_timeout:
                    builder.withIdleTimeout(dataToShort(data));
                    break;
                case max_packet_size:
                    builder.withMaxPacketSize(dataToShort(data));
                    break;
                case stateless_reset_token:
                    builder.withStatelessResetToken(data);
                    break;
                case ack_delay_exponent:
                    builder.withAckDelayExponent(dataToByte(data));
                    break;
                case initial_max_uni_streams:
                    builder.withInitialMaxUniStreams(dataToShort(data));
                    break;
                case disable_migration:
                    builder.withDisableMigration(true);
                    break;
                case initial_max_stream_data_bidi_remote:
                    builder.withInitialMaxStreamDataBidiRemote(dataToInt(data));
                    break;
                case initial_max_stream_data_uni:
                    builder.withInitialMaxStreamDataUni(dataToInt(data));
                    break;
                case max_ack_delay:
                    builder.withMaxAckDelay(dataToByte(data));
                    break;
                case original_connection_id:
                    builder.withOriginalConnectionId(data);
                    break;
            }
        }
        return builder.build();
    }

    private static int dataToInt(byte[] data) {
        return Ints.fromByteArray(data);
    }

    private static int dataToShort(byte[] data) {
        return Shorts.fromByteArray(data);
    }

    private static int dataToByte(byte[] data) {
        Preconditions.checkArgument(data.length == 1);
        return data[0] & 0xFF;
    }

    private static byte[] intToData(int i) {
        return Ints.toByteArray(i);
    }

    private static byte[] shortToData(int i) {
        return Shorts.toByteArray((short)i);
    }

    private final int initialMaxStreamDataBidiLocal;
    private final int initialMaxData;
    private final int initialMaxBidiStreams;
    private final int idleTimeout;
    // TODO preferred_address(4),
    private final int maxPacketSize;
    private final byte[] statelessResetToken;
    private final int ackDelayExponent;
    private final int initialMaxUniStreams;
    private final boolean disableMigration;
    private final int initialMaxStreamDataBidiRemote;
    private final int initialMaxStreamDataUni;
    private final int maxAckDelay;
    private final byte[] originalConnectionId;

    private TransportParameters(int initialMaxStreamDataBidiLocal, int initialMaxData, int initialMaxBidiStreams, int idleTimeout, int maxPacketSize, byte[] statelessResetToken, int ackDelayExponent, int initialMaxUniStreams, boolean disableMigration, int initialMaxStreamDataBidiRemote, int initialMaxStreamDataUni, int maxAckDelay, byte[] originalConnectionId) {
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
        return initialMaxStreamDataBidiLocal == that.initialMaxStreamDataBidiLocal &&
                initialMaxData == that.initialMaxData &&
                initialMaxBidiStreams == that.initialMaxBidiStreams &&
                idleTimeout == that.idleTimeout &&
                maxPacketSize == that.maxPacketSize &&
                ackDelayExponent == that.ackDelayExponent &&
                initialMaxUniStreams == that.initialMaxUniStreams &&
                disableMigration == that.disableMigration &&
                initialMaxStreamDataBidiRemote == that.initialMaxStreamDataBidiRemote &&
                initialMaxStreamDataUni == that.initialMaxStreamDataUni &&
                maxAckDelay == that.maxAckDelay &&
                Arrays.equals(statelessResetToken, that.statelessResetToken) &&
                Arrays.equals(originalConnectionId, that.originalConnectionId);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(initialMaxStreamDataBidiLocal, initialMaxData, initialMaxBidiStreams, idleTimeout, maxPacketSize, ackDelayExponent, initialMaxUniStreams, disableMigration, initialMaxStreamDataBidiRemote, initialMaxStreamDataUni, maxAckDelay);
        result = 31 * result + Arrays.hashCode(statelessResetToken);
        result = 31 * result + Arrays.hashCode(originalConnectionId);
        return result;
    }

    @Override
    public String toString() {
        return "TransportParameters{" +
                "initialMaxStreamDataBidiLocal=" + initialMaxStreamDataBidiLocal +
                ", initialMaxData=" + initialMaxData +
                ", initialMaxBidiStreams=" + initialMaxBidiStreams +
                ", idleTimeout=" + idleTimeout +
                ", maxPacketSize=" + maxPacketSize +
                ", statelessResetToken=" + Arrays.toString(statelessResetToken) +
                ", ackDelayExponent=" + ackDelayExponent +
                ", initialMaxUniStreams=" + initialMaxUniStreams +
                ", disableMigration=" + disableMigration +
                ", initialMaxStreamDataBidiRemote=" + initialMaxStreamDataBidiRemote +
                ", initialMaxStreamDataUni=" + initialMaxStreamDataUni +
                ", maxAckDelay=" + maxAckDelay +
                ", originalConnectionId=" + Arrays.toString(originalConnectionId) +
                '}';
    }

    public void write(ByteBuf bb, boolean isClient) {
        bb.writeBytes(new byte[] {00, 00, 00, 00, 00, 0x3c}); // TODO figure out

        if (initialMaxStreamDataBidiLocal > -1) {
            bb.writeBytes(initial_max_stream_data_bidi_local.asBytes());
            bb.writeShort(4);
            bb.writeBytes(intToData(initialMaxStreamDataBidiLocal));
        }
        if (initialMaxData > -1) {
            bb.writeBytes(initial_max_data.asBytes());
            bb.writeShort(4);
            bb.writeBytes(intToData(initialMaxData));
        }
        if (initialMaxBidiStreams > -1) {
            bb.writeBytes(initial_max_bidi_streams.asBytes());
            bb.writeShort(2);
            bb.writeBytes(shortToData(initialMaxBidiStreams));
        }
        if (idleTimeout > -1) {
            bb.writeBytes(idle_timeout.asBytes());
            bb.writeShort(2);
            bb.writeBytes(shortToData(idleTimeout));
        }
        if (maxPacketSize > -1) {
            bb.writeBytes(max_packet_size.asBytes());
            bb.writeShort(2);
            bb.writeBytes(shortToData(maxPacketSize));
        }
        if (statelessResetToken.length > 0) {
            bb.writeBytes(stateless_reset_token.asBytes());
            bb.writeShort(statelessResetToken.length);
            bb.writeBytes(statelessResetToken);
        }
        if (ackDelayExponent > -1) {
            bb.writeBytes(ack_delay_exponent.asBytes());
            bb.writeShort(1);
            bb.writeByte(ackDelayExponent);
        }
        if (initialMaxUniStreams > -1) {
            bb.writeBytes(initial_max_uni_streams.asBytes());
            bb.writeShort(2);
            bb.writeBytes(shortToData(initialMaxUniStreams));
        }
        if (disableMigration) {
            bb.writeBytes(disable_migration.asBytes());
            bb.writeShort(0);
        }
        if (initialMaxStreamDataBidiRemote > -1) {
            bb.writeBytes(initial_max_stream_data_bidi_remote.asBytes());
            bb.writeShort(4);
            bb.writeBytes(intToData(initialMaxStreamDataBidiRemote));
        }
        if (initialMaxStreamDataUni > -1) {
            bb.writeBytes(initial_max_stream_data_uni.asBytes());
            bb.writeShort(4);
            bb.writeBytes(intToData(initialMaxStreamDataUni));
        }
        if (maxAckDelay > -1) {
            bb.writeBytes(max_ack_delay.asBytes());
            bb.writeShort(1);
            bb.writeByte(maxAckDelay);
        }
        if (originalConnectionId.length > 0) {
            bb.writeBytes(original_connection_id.asBytes());
            bb.writeShort(originalConnectionId.length);
            bb.writeBytes(originalConnectionId);
        }
    }
}
