package com.protocol7.nettyquic.flowcontrol;

import com.protocol7.nettyquic.InboundHandler;
import com.protocol7.nettyquic.OutboundHandler;

public interface FlowControlHandler extends InboundHandler, OutboundHandler {}
