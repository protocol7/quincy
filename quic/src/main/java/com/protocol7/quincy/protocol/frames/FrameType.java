package com.protocol7.quincy.protocol.frames;

import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;

public enum FrameType {
  PADDING(0x00),
  PING(0x01),
  ACK(0x02), // 0x02-0x03
  RESET_STREAM(0x04),
  STOP_SENDING(0x05),
  CRYPTO(0x06),
  NEW_TOKEN(0x07),
  STREAM(0x08), // 0x08 - 0x0f
  MAX_DATA(0x10),
  MAX_STREAM_DATA(0x11),
  MAX_STREAMS(0x12), // 0x12-0x13
  DATA_BLOCKED(0x14),
  STREAM_DATA_BLOCKED(0x15),
  STREAMS_BLOCKED(0x16), // 0x16-0x17
  NEW_CONNECTION_ID(0x18),
  RETIRE_CONNECTION_ID(0x19),
  PATH_CHALLENGE(0x1a),
  PATH_RESPONSE(0x1b),
  CONNECTION_CLOSE(0x1c),
  APPLICATION_CLOSE(0x1d);

  public static FrameType fromByte(final byte b) {
    if (b == PADDING.type) {
      return PADDING;
    } else if (b == RESET_STREAM.type) {
      return RESET_STREAM;
    } else if (b == 0x1c) {
      return CONNECTION_CLOSE;
    } else if (b == 0x1d) {
      return APPLICATION_CLOSE;
    } else if (b == MAX_DATA.type) {
      return MAX_DATA;
    } else if (b == MAX_STREAM_DATA.type) {
      return MAX_STREAM_DATA;
    } else if (b == 0x12 || b == 0x13) {
      return MAX_STREAMS;
    } else if (b == PING.type) {
      return PING;
    } else if (b == DATA_BLOCKED.type) {
      return DATA_BLOCKED;
    } else if (b == RETIRE_CONNECTION_ID.type) {
      return RETIRE_CONNECTION_ID;
    } else if (b == STREAM_DATA_BLOCKED.type) {
      return STREAM_DATA_BLOCKED;
    } else if (b == 0x16 || b == 0x17) {
      return STREAMS_BLOCKED;
    } else if (b == NEW_CONNECTION_ID.type) {
      return NEW_CONNECTION_ID;
    } else if (b == STOP_SENDING.type) {
      return STOP_SENDING;
    } else if (b == 0x02 || b == 0x03) {
      return ACK;
    } else if (b == PATH_CHALLENGE.type) {
      return PATH_CHALLENGE;
    } else if (b == PATH_RESPONSE.type) {
      return PATH_RESPONSE;
    } else if (b == NEW_TOKEN.type) {
      return NEW_TOKEN;
    } else if (b >= 0x08 && b <= 0x0f) {
      return STREAM;
    } else if (b == CRYPTO.type) {
      return CRYPTO;
    } else {
      throw new RuntimeException("Unknown frame type " + Hex.hex(b));
    }
  }

  private final byte type;

  FrameType(final int type) {
    this.type = (byte) type;
  }

  public byte getType() {
    return type;
  }

  public void write(final ByteBuf bb) {
    bb.writeByte(type);
  }
}
