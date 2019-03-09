package com.protocol7.nettyquic.tls.aead;

public class Labels {

  public static final String CLIENT_HANDSHAKE_TRAFFIC_SECRET = "c hs traffic";
  public static final String SERVER_HANDSHAKE_TRAFFIC_SECRET = "s hs traffic";

  public static final String CLIENT_APPLICATION_TRAFFIC_SECRET = "c ap traffic";
  public static final String SERVER_APPLICATION_TRAFFIC_SECRET = "s ap traffic";

  public static final String CLIENT_INITIAL = "client in";
  public static final String SERVER_INITIAL = "server in";

  public static final String DERIVED = "derived";

  public static final String KEY = "quic key";
  public static final String IV = "quic iv";
  public static final String HP_KEY = "quic hp";

  public static final String FINISHED = "finished";

  private Labels() {}
}
