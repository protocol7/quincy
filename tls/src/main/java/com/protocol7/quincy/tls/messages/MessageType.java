package com.protocol7.quincy.tls.messages;

public enum MessageType {
  CLIENT_HELLO((byte) 0x01),
  SERVER_HELLO((byte) 0x02),
  ENCRYPTED_EXTENSIONS((byte) 0x08),
  SERVER_CERTIFICATE((byte) 0x0b),
  SERVER_CERTIFICATE_VERIFY((byte) 0x0f),
  FINISHED((byte) 0x14);

  public static MessageType from(final byte type) {
    if (type == CLIENT_HELLO.type) {
      return CLIENT_HELLO;
    } else if (type == SERVER_HELLO.type) {
      return SERVER_HELLO;
    } else if (type == ENCRYPTED_EXTENSIONS.type) {
      return ENCRYPTED_EXTENSIONS;
    } else if (type == SERVER_CERTIFICATE.type) {
      return SERVER_CERTIFICATE;
    } else if (type == SERVER_CERTIFICATE_VERIFY.type) {
      return SERVER_CERTIFICATE_VERIFY;
    } else if (type == FINISHED.type) {
      return FINISHED;
    } else {
      throw new IllegalArgumentException("Unknown TLS message type: " + type);
    }
  }

  private final byte type;

  MessageType(final byte type) {
    this.type = type;
  }

  public byte getType() {
    return type;
  }
}
