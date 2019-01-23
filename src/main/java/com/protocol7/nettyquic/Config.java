package com.protocol7.nettyquic;

public class Config {


    public static final int INITIAL_MAX_DATA = 49152;

    // max stream data
    public static final int INITIAL_MAX_STREAM_DATA_BIDI_LOCAL = 32768;
    public static final int INITIAL_MAX_STREAM_DATA_BIDI_REMOTE = 32768;

    // max streams
    public static final int INITIAL_MAX_STREAM_DATA_UNI = 32768;
    public static final int INITIAL_MAX_BIDI_STREAMS = 100;
    public static final int INITIAL_MAX_UNI_STREAMS = 100;

    public static final int MAX_STREAM_UPDATE_DELTA = 100;
    public static final double MAX_STREAM_UPDATE_THRESHOLD = 0.25;



    // timeout
    public static final int IDLE_TIMEOUT = 30;

    // packet size
    public static final int MAX_PACKET_SIZE = 1452;

    // migration
    public static final boolean DISABLE_MIGRATION = true;


}
