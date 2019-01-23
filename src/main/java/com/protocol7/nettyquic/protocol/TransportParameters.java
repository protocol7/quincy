package com.protocol7.nettyquic.protocol;

public interface TransportParameters {

    int getInitialMaxStreamDataBidiLocal();

    int getInitialMaxData();

    int getInitialMaxBidiStreams();

    int getIdleTimeout();

    int getMaxPacketSize();

    byte[] getStatelessResetToken();

    int getAckDelayExponent();

    int getInitialMaxUniStreams();

    boolean isDisableMigration();

    int getInitialMaxStreamDataBidiRemote();

    int getInitialMaxStreamDataUni();

    int getMaxAckDelay();

    byte[] getOriginalConnectionId();

}
