package com.protocol7.nettyquick.protocol.frames;

import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;

public enum FrameType {

  PADDING(0x00),
  RST_STREAM(0x01),
  CONNECTION_CLOSE(0x02),
  APPLICATION_CLOSE(0x03),
  MAX_DATA(0x04),
  MAX_STREAM_DATA(0x05),
  MAX_STREAM_ID(0x06),
  PING(0x07),
  BLOCKED(0x08),
  STREAM_BLOCKED(0x09),
  STREAM_ID_BLOCKED(0x0a),
  NEW_CONNECTION_ID(0x0b),
  STOP_SENDING(0x0c),
  PONG(0x0d),
  ACK(0x0e),
  STREAM(0x10); // 0x10 - 0x17

  public static FrameType fromByte(byte b) {
    if (b == PADDING.type) {
      return PADDING;
    } else if (b == RST_STREAM.type) {
      return RST_STREAM;
    } else if (b == CONNECTION_CLOSE.type) {
      return CONNECTION_CLOSE;
    } else if (b == APPLICATION_CLOSE.type) {
      return APPLICATION_CLOSE;
    } else if (b == MAX_DATA.type) {
      return MAX_DATA;
    } else if (b == MAX_STREAM_DATA.type) {
      return MAX_STREAM_DATA;
    } else if (b == MAX_STREAM_ID.type) {
      return MAX_STREAM_ID;
    } else if (b == PING.type) {
      return PING;
    } else if (b == BLOCKED.type) {
      return BLOCKED;
    } else if (b == STREAM_BLOCKED.type) {
      return STREAM_BLOCKED;
    } else if (b == STREAM_ID_BLOCKED.type) {
      return STREAM_ID_BLOCKED;
    } else if (b == NEW_CONNECTION_ID.type) {
      return NEW_CONNECTION_ID;
    } else if (b == STOP_SENDING.type) {
      return STOP_SENDING;
    } else if (b == PONG.type) {
      return PONG;
    } else if (b == ACK.type) {
      return ACK;
    } else if (b >= 0x10 && b <= 0x17) {
      return STREAM;
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

  public void write(ByteBuf bb) {
    bb.writeByte(type);
  }
}
