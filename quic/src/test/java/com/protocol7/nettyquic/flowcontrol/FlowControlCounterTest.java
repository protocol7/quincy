package com.protocol7.nettyquic.flowcontrol;

import com.protocol7.nettyquic.flowcontrol.FlowControlCounter.TryConsumeResult;
import com.protocol7.nettyquic.protocol.StreamId;
import org.junit.Test;

import static org.junit.Assert.*;

public class FlowControlCounterTest {

  private final double maxConn = 15.0;
  private final double maxStream = 10.0;

  private FlowControlCounter fcm = new FlowControlCounter((long) maxConn, (long) maxStream);
  private StreamId sid = new StreamId(123);
  private StreamId sid2 = new StreamId(456);

  @Test
  public void tryConsumeConnection() {
    assertConsume(fcm.tryConsume(sid, 7), true, 7 / maxConn, 7 / maxStream);
    assertConsume(fcm.tryConsume(sid2, 8), true, 15 / maxConn, 8 / maxStream);
    assertConsume(fcm.tryConsume(sid, 8), false, 15 / maxConn, 7 / maxStream);

    fcm.setConnectionMaxBytes(20);
    assertConsume(fcm.tryConsume(sid, 9), true, 17 / 20.0, 9 / maxStream);
  }

  @Test
  public void tryConsumeStream() {
    assertConsume(fcm.tryConsume(sid, 9), true, 9 / maxConn, 9 / maxStream);
    assertConsume(fcm.tryConsume(sid, 11), false, 9 / maxConn, 9 / maxStream);

    fcm.setStreamMaxBytes(sid, 20);
    assertConsume(fcm.tryConsume(sid, 11), true, 11 / maxConn, 11 / 20.0);
  }

  @Test
  public void tryConsumeOutOfOrder() {
    assertConsume(fcm.tryConsume(sid, 8), true, 8 / maxConn, 8 / maxStream);
    assertConsume(fcm.tryConsume(sid, 7), true, 8 / maxConn, 8 / maxStream);
  }

  @Test
  public void finishStream() {
    assertConsume(fcm.tryConsume(sid, 2), true, 2 / maxConn, 2 / maxStream);
    fcm.finishStream(sid, 5);
    assertConsume(fcm.tryConsume(sid2, 1), true, 6 / maxConn, 1 / maxStream);
  }

  @Test
  public void finishStreamOutOfOrder() {
    assertConsume(fcm.tryConsume(sid, 2), true, 2 / maxConn, 2 / maxStream);
    fcm.finishStream(sid, 5);

    // get a stream offset that is smaller orr equal the finished value
    assertConsume(fcm.tryConsume(sid, 5), true, 5 / maxConn, 5 / maxStream);
  }

  @Test(expected = IllegalStateException.class)
  public void offsetForFinishedStream() {
    assertConsume(fcm.tryConsume(sid, 2), true, 2 / maxConn, 2 / maxStream);
    fcm.finishStream(sid, 5);
    fcm.tryConsume(sid, 6);
  }

  @Test
  public void tryConsumeTooSmallConnectionSet() {
    assertConsume(fcm.tryConsume(sid, 8), true, 8 / maxConn, 8 / maxStream);

    fcm.setConnectionMaxBytes(9); // must be ignored as the current max is larger
    assertConsume(fcm.tryConsume(sid, 10), true, 10 / maxConn, 10 / maxStream);
  }

  @Test(expected = IllegalArgumentException.class)
  public void tryConsumeNegative() {
    fcm.tryConsume(sid, -8);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setMaxConnectionNegative() {
    fcm.setConnectionMaxBytes(-1);
  }

  private void assertConsume(
      TryConsumeResult actual, boolean success, double connection, double stream) {
    assertEquals(success, actual.isSuccessful());
    assertEquals("Connection ratio", connection, actual.getConnection(), 0.01);
    assertEquals("Stream ratio", stream, actual.getStream(), 0.01);
  }
}
