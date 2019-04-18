package com.protocol7.quincy.streams;

import static org.junit.Assert.*;

import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.protocol.StreamId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StreamsTest {

  @Mock PipelineContext ctx;
  @Mock StreamListener listener;

  private Streams streams;

  @Before
  public void setUp() {
    this.streams = new Streams(ctx);
  }

  @Test
  public void openStream() {
    final Stream stream = streams.openStream(false, false, listener);
    assertEquals(new StreamId(3), stream.getId());
  }

  @Test
  public void createAndThenGet() {
    final StreamId streamId = StreamId.random(true, true);
    final DefaultStream stream1 = streams.getOrCreate(streamId, listener);
    final DefaultStream stream2 = streams.getOrCreate(streamId, listener);

    assertSame(stream1, stream2);

    final DefaultStream stream3 = streams.getOrCreate(StreamId.random(true, true), listener);
    assertNotSame(stream1, stream3);
  }
}
