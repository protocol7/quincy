package com.protocol7.testcontainers.quicly;

import com.protocol7.nettyquic.utils.Bytes;
import com.protocol7.nettyquic.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;

public class PacketParser {

  public static List<QuiclyPacket> parse(List<String> logs) {
    List<QuiclyPacket> packets = new ArrayList<>();

    boolean inbound = false;
    ByteBuf bb = Unpooled.buffer();
    for (String log : logs) {
      if (log.startsWith("recvmsg") || log.startsWith("sendmsg")) {
        if (bb.writerIndex() > 0) {
          packets.add(new QuiclyPacket(inbound, Bytes.peekToArray(bb)));
          bb.clear();
        }
        inbound = log.startsWith("recvmsg");
      } else {
        bb.writeBytes(Hex.dehex(log.trim()));
      }
    }
    if (bb.writerIndex() > 0) {
      packets.add(new QuiclyPacket(inbound, Bytes.peekToArray(bb)));
    }
    return packets;
  }
}
