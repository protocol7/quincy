package com.protocol7.nettyquic.protocol.frames;

import static org.junit.Assert.assertTrue;

import com.protocol7.nettyquic.protocol.StreamId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class FrameTest {

  @Test
  public void cryptoFrame() {
    CryptoFrame frame = new CryptoFrame(0, "hello".getBytes());
    assertFrame(frame);
  }

  @Test
  public void pingFrame() {
    assertFrame(PingFrame.INSTANCE);
  }

  @Test
  public void ackFrame() {
    assertFrame(new AckFrame(123, new AckBlock(12, 13)));
  }

  @Test
  public void streamFrame() {
    assertFrame(new StreamFrame(new StreamId(123), 124, true, "hello".getBytes()));
  }

  @Test
  public void paddingFrame() {
    assertFrame(PaddingFrame.INSTANCE);
  }

  @Test
  public void retireConnectionIdFrame() {
    assertFrame(new RetireConnectionIdFrame(123));
  }

  @Test
  public void resetStreamFrame() {
    assertFrame(new ResetStreamFrame(new StreamId(123), 124, 125));
  }

  @Test
  public void connectionCloseFrame() {
    ConnectionCloseFrame ccf = ConnectionCloseFrame.connection(12, 13, "hello");
    assertFrame(ccf);
  }

  @Test
  public void applicationCloseFrame() {
    ConnectionCloseFrame ccf = ConnectionCloseFrame.application(12, "hello");
    assertFrame(ccf);
  }

  @Test
  public void maxStreamDataFrame() {
    assertFrame(new MaxStreamDataFrame(new StreamId(123), 456));
  }

  @Test
  public void maxDataFrame() {
    assertFrame(new MaxDataFrame(456));
  }

  @Test
  public void maxStreamsFrame() {
    assertFrame(new MaxStreamsFrame(456, true));
  }

  @Test
  public void streamDataBlockedFrame() {
    assertFrame(new StreamDataBlockedFrame(new StreamId(123), 456));
  }

  @Test
  public void dataBlockedFrame() {
    assertFrame(new DataBlockedFrame(456));
  }

  @Test
  public void streamsBlockedFrame() {
    assertFrame(new StreamsBlockedFrame(456, true));
  }

  private void assertFrame(Frame frame) {
    ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    Frame parsed = Frame.parse(bb);

    assertTrue(parsed.getClass().equals(frame.getClass()));
  }
}
