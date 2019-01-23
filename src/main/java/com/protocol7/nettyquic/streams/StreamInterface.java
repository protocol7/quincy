package com.protocol7.nettyquic.streams;

public interface StreamInterface {

    void write(final byte[] b, boolean finish);

    void reset(int applicationErrorCode);

    void canWrite();

    void canReset();
}
