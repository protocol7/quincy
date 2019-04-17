package com.protocol7.quincy;

import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.tls.extensions.TransportParameters;

public class Configuration {

  public static class Builder {
    private Version version = Version.DRAFT_18;
    private int initialMaxStreamDataBidiLocal = 32768;
    private int initialMaxData = 49152;
    private int initialMaxBidiStreams = 100;
    private int idleTimeout = 30;
    private int maxPacketSize = 1452;
    private int ackDelayExponent = 3;
    private int initialMaxUniStreams = 100;
    private boolean disableMigration = true;
    private int initialMaxStreamDataBidiRemote = 32768;
    private int initialMaxStreamDataUni = 32768;
    private int maxAckDelay = 100; // TODO verify

    public Builder withVersion(final Version version) {
      this.version = version;
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

    public Configuration build() {
      return new Configuration(
          version,
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
    }
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Configuration defaults() {
    return new Builder().build();
  }

  private final Version version;
  private final int initialMaxStreamDataBidiLocal;
  private final int initialMaxData;
  private final int initialMaxBidiStreams;
  private final int idleTimeout;
  private final int maxPacketSize;
  private final int ackDelayExponent;
  private final int initialMaxUniStreams;
  private final boolean disableMigration;
  private final int initialMaxStreamDataBidiRemote;
  private final int initialMaxStreamDataUni;
  private final int maxAckDelay;

  private Configuration(
      final Version version,
      final int initialMaxStreamDataBidiLocal,
      final int initialMaxData,
      final int initialMaxBidiStreams,
      final int idleTimeout,
      final int maxPacketSize,
      final int ackDelayExponent,
      final int initialMaxUniStreams,
      final boolean disableMigration,
      final int initialMaxStreamDataBidiRemote,
      final int initialMaxStreamDataUni,
      final int maxAckDelay) {
    this.version = version;
    this.initialMaxStreamDataBidiLocal = initialMaxStreamDataBidiLocal;
    this.initialMaxData = initialMaxData;
    this.initialMaxBidiStreams = initialMaxBidiStreams;
    this.idleTimeout = idleTimeout;
    this.maxPacketSize = maxPacketSize;
    this.ackDelayExponent = ackDelayExponent;
    this.initialMaxUniStreams = initialMaxUniStreams;
    this.disableMigration = disableMigration;
    this.initialMaxStreamDataBidiRemote = initialMaxStreamDataBidiRemote;
    this.initialMaxStreamDataUni = initialMaxStreamDataUni;
    this.maxAckDelay = maxAckDelay;
  }

  public Version getVersion() {
    return version;
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

  public TransportParameters toTransportParameters() {
    return TransportParameters.newBuilder(version.asBytes())
        .withInitialMaxStreamDataBidiLocal(initialMaxStreamDataBidiLocal)
        .withInitialMaxData(initialMaxData)
        .withInitialMaxBidiStreams(initialMaxBidiStreams)
        .withIdleTimeout(idleTimeout)
        .withMaxPacketSize(maxPacketSize)
        .withInitialMaxUniStreams(initialMaxUniStreams)
        .withDisableMigration(disableMigration)
        .withInitialMaxStreamDataBidiRemote(initialMaxStreamDataBidiRemote)
        .withInitialMaxStreamDataUni(initialMaxStreamDataUni)
        .build();
  }
}
