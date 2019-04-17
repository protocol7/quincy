package com.protocol7.testcontainers.quicgo;

import com.google.common.base.Strings;
import com.google.common.primitives.Longs;
import com.protocol7.quincy.utils.Hex;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuicGoPacketParser {

  private static final Pattern DEST_CONN_ID_PATTERN =
      Pattern.compile("DestConnectionID: 0x([a-h0-9]+)");
  private static final Pattern SRC_CONN_ID_PATTERN =
      Pattern.compile("SrcConnectionID: 0x([a-h0-9]+)");
  private static final Pattern PN_PATTERN = Pattern.compile("PacketNumber: 0x([a-h0-9]+)");
  private static final Pattern TYPE_PATTERN = Pattern.compile("Type: ([a-zA-Z]+)");

  public static List<QuicGoPacket> parse(List<String> logs) {
    List<QuicGoPacket> packets = new ArrayList<>();

    for (int i = 0; i < logs.size(); i++) {
      String log = logs.get(i);

      if (log.startsWith("server \tLong Header{") || log.startsWith("server \tShort Header{")) {
        String prevLog = logs.get(i - 1);
        boolean inbound = prevLog.contains("<- ");
        boolean longHeader = log.contains("Long Header{");

        Matcher destConnIdMatcher = DEST_CONN_ID_PATTERN.matcher(log);
        byte[] destConnId = null;
        if (destConnIdMatcher.find()) {
          destConnId = Hex.dehex(destConnIdMatcher.group(1));
        }

        Matcher srcConnIdMatcher = SRC_CONN_ID_PATTERN.matcher(log);
        byte[] srcConnId = null;
        if (srcConnIdMatcher.find()) {
          srcConnId = Hex.dehex(srcConnIdMatcher.group(1));
        }

        Matcher pnMatcher = PN_PATTERN.matcher(log);
        long pn = -1;
        if (pnMatcher.find()) {
          String s = Strings.padStart(pnMatcher.group(1), 16, '0');
          pn = Longs.fromByteArray(Hex.dehex(s));
        }

        String type = null;
        if (longHeader) {
          Matcher typeMatcher = TYPE_PATTERN.matcher(log);
          if (typeMatcher.find()) {
            type = typeMatcher.group(1);
          }
        } else {
          type = "1-RTT";
        }

        packets.add(new QuicGoPacket(inbound, longHeader, type, destConnId, srcConnId, pn));
      }
    }

    return packets;
  }
}
