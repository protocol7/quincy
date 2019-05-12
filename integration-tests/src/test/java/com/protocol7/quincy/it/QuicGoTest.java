package com.protocol7.quincy.it;

import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.client.QuicClient;
import com.protocol7.quincy.netty.QuicBuilder;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.testcontainers.quicgo.QuicGoContainer;
import com.protocol7.testcontainers.quicgo.QuicGoPacket;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Rule;
import org.junit.Test;

public class QuicGoTest {

  @Rule public QuicGoContainer quicGo = new QuicGoContainer();

  @Test
  public void test() throws ExecutionException, InterruptedException {
    final Configuration config = new QuicBuilder().withVersion(Version.QUIC_GO).configuration();

    QuicClient client = null;
    try {
      client =
          QuicClient.connect(
                  config,
                  quicGo.getAddress(),
                  (stream, data, finished) -> System.out.println(new String(data)))
              .get();

      client.openStream().write("Hello world".getBytes(), true);
    } finally {
      if (client != null) {
        client.close();
      }
    }

    final List<QuicGoPacket> packets = quicGo.getPackets();

    // client hello without token
    assertPacket(packets.get(0), true, true, "Initial", 0);

    // token needed, retry
    assertPacket(packets.get(1), false, true, "Retry", -1);

    // client hello with token
    assertPacket(packets.get(2), true, true, "Initial", 1);

    // server hello
    assertPacket(packets.get(3), false, true, "Initial", 0);

    // handshake
    assertPacket(packets.get(4), false, true, "Handshake", 0);

    assertPacket(packets.get(5), false, false, "1-RTT", 0);

    // ack handshake
    assertPacket(packets.get(6), true, true, "Handshake", 2);

    // ack handshake
    assertPacket(packets.get(7), false, true, "Handshake", 1);
  }

  private void assertPacket(
      final QuicGoPacket actual,
      final boolean inbount,
      final boolean longHeader,
      final String type,
      final long pn) {
    assertEquals(inbount, actual.inbound);
    assertEquals(longHeader, actual.longHeader);
    assertEquals(type, actual.type);
    assertEquals(pn, actual.packetNumber);
  }
}
