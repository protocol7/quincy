package com.protocol7.quincy.logging;

import com.protocol7.quincy.InboundHandler;
import com.protocol7.quincy.OutboundHandler;
import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.protocol.packets.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingHandler implements InboundHandler, OutboundHandler {

  private final Logger log = LoggerFactory.getLogger(LoggingHandler.class);

  private final boolean isClient;

  public LoggingHandler(final boolean isClient) {
    this.isClient = isClient;
  }

  @Override
  public void onReceivePacket(final Packet packet, final PipelineContext ctx) {
    log.info("<<< {} received state={} packet={}", actor(), ctx.getState(), packet);

    ctx.next(packet);
  }

  @Override
  public void beforeSendPacket(final Packet packet, final PipelineContext ctx) {
    log.info(">>> {} sent state={} packet={}", actor(), ctx.getState(), packet);

    ctx.next(packet);
  }

  private String actor() {
    if (isClient) {
      return "Client";
    } else {
      return "Server";
    }
  }
}
