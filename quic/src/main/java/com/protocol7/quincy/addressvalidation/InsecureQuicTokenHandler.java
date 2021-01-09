/*
 * Copyright 2020 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.protocol7.quincy.addressvalidation;

import com.protocol7.quincy.protocol.ConnectionId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Optional;

/**
 * Insecure {@link QuicTokenHandler} which only does basic token generation / validation without any
 * crypto.
 *
 * <p><strong>This shouldn't be used in production.</strong>
 */
public final class InsecureQuicTokenHandler implements QuicTokenHandler {

  private static final String SERVER_NAME = "netty";
  private static final byte[] SERVER_NAME_BYTES = SERVER_NAME.getBytes(CharsetUtil.US_ASCII);
  private static final ByteBuf SERVER_NAME_BUFFER = Unpooled.wrappedBuffer(SERVER_NAME_BYTES);

  // Just package-private for unit tests
  static final int MAX_TOKEN_LEN =
      ConnectionId.MAX_LENGTH + NetUtil.LOCALHOST6.getAddress().length + SERVER_NAME_BYTES.length;

  private InsecureQuicTokenHandler() {}

  public static final InsecureQuicTokenHandler INSTANCE = new InsecureQuicTokenHandler();

  @Override
  public boolean writeToken(
      final ByteBuf out, final ConnectionId dcid, final InetSocketAddress address) {
    final byte[] addr = address.getAddress().getAddress();
    out.writeBytes(SERVER_NAME_BYTES)
        .writeByte(addr.length)
        .writeBytes(addr)
        .writeByte(dcid.getLength())
        .writeBytes(dcid.asBytes());
    return true;
  }

  @Override
  public Optional<ConnectionId> validateToken(
      final ByteBuf token, final InetSocketAddress address) {
    final byte[] addr = address.getAddress().getAddress();

    if (token.readableBytes() <= SERVER_NAME_BYTES.length + addr.length + 1 + 1) {
      return Optional.empty();
    }

    final byte[] name = new byte[SERVER_NAME_BYTES.length];
    token.readBytes(name);
    if (!Arrays.equals(SERVER_NAME_BYTES, name)) {
      return Optional.empty();
    }

    final int addrLen = token.readByte();
    final byte[] tokenAddr = new byte[addrLen];
    token.readBytes(tokenAddr);
    if (!Arrays.equals(tokenAddr, addr)) {
      return Optional.empty();
    }

    final int cidLen = token.readByte();
    final byte[] cid = new byte[cidLen];
    token.readBytes(cid);

    return Optional.of(new ConnectionId(cid));
  }

  @Override
  public int maxTokenLength() {
    return MAX_TOKEN_LEN;
  }
}
