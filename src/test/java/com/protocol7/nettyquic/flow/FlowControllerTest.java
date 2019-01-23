package com.protocol7.nettyquic.flow;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class FlowControllerTest {

  private AtomicLong readMaxUpdates = new AtomicLong(-1);
  private FlowController controller = new FlowController(100, 100, readMaxUpdates::set, 100, 0.25);

  @Test
  public void readMaxUpdate() {
    controller.addReadBytes(30);
      assertEquals(-1, readMaxUpdates.get()); // no update
    controller.addReadBytes(50);
    assertEquals(180, readMaxUpdates.get());
    controller.addReadBytes(50);
    assertEquals(230, readMaxUpdates.get());
    controller.addReadBytes(50);
    assertEquals(280, readMaxUpdates.get());
  }

  @Test
  public void write() {
    controller.addWriteBytes(40);
    assertEquals(60, controller.remainingWrite());
    assertTrue(controller.canWrite(60));
    controller.addWriteBytes(60);
    assertEquals(00, controller.remainingWrite());
    assertFalse(controller.canWrite(1));

    controller.updateWriteMax(170);

    assertEquals(70, controller.remainingWrite());
    assertTrue(controller.canWrite(70));
  }
}
