package com.protocol7.quincy.flowcontrol;

import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.protocol.packets.Packet;

public class MockFlowControlHandler implements FlowControlHandler {
  @Override
  public void onReceivePacket(final Packet packet, final PipelineContext ctx) {
    ctx.next(packet);
  }

  @Override
  public void beforeSendPacket(final Packet packet, final PipelineContext ctx) {
    ctx.next(packet);
  }
}
