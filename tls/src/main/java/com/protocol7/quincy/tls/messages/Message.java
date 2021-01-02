package com.protocol7.quincy.tls.messages;

import io.netty.buffer.ByteBuf;

public interface Message {

  static Message parse(final ByteBuf bb, final boolean isClient) {
    final MessageType type = MessageType.from(bb.getByte(bb.readerIndex()));

    if (type == MessageType.CLIENT_HELLO) {
      return ClientHello.parse(bb, isClient);
    } else if (type == MessageType.SERVER_HELLO) {
      return ServerHello.parse(bb, isClient);
    } else if (type == MessageType.ENCRYPTED_EXTENSIONS) {
      return EncryptedExtensions.parse(bb, isClient);
    } else if (type == MessageType.SERVER_CERTIFICATE) {
      return ServerCertificate.parse(bb);
    } else if (type == MessageType.SERVER_CERTIFICATE_VERIFY) {
      return ServerCertificateVerify.parse(bb);
    } else if (type == MessageType.FINISHED) {
      return Finished.parse(bb);
    } else {
      throw new IllegalArgumentException("Unknown TLS message type");
    }
  }

  MessageType getType();
}
