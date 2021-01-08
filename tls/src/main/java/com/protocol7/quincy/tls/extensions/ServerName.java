package com.protocol7.quincy.tls.extensions;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ServerName implements Extension {

  public static ServerName parse(final ByteBuf bb) {
    // TODO handle multiple names
    bb.readShort(); // first item length
    final byte nameType = bb.readByte();

    if (nameType != 0) {
      throw new IllegalArgumentException("Unknown name type " + nameType);
    }

    final int len = bb.readShort();
    final byte[] b = new byte[len];
    bb.readBytes(b);

    final String serverName = new String(b, StandardCharsets.US_ASCII);

    return new ServerName(serverName);
  }

  private final String serverName;

  public ServerName(final String serverName) {
    this.serverName = serverName;
  }

  @Override
  public ExtensionType getType() {
    return ExtensionType.SERVER_NAME;
  }

  @Override
  public void write(final ByteBuf bb, final boolean ignored) {
    final byte[] b = serverName.getBytes(StandardCharsets.US_ASCII);

    bb.writeShort(b.length + 3);
    bb.writeByte(0); // name type
    bb.writeShort(b.length);
    bb.writeBytes(b);
  }

  public String getServerName() {
    return serverName;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final ServerName that = (ServerName) o;
    return Objects.equals(serverName, that.serverName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serverName);
  }

  @Override
  public String toString() {
    return "ServerName{" + serverName + '}';
  }
}
