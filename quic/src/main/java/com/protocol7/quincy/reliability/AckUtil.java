package com.protocol7.quincy.reliability;

import com.protocol7.quincy.protocol.frames.AckFrame;
import com.protocol7.quincy.protocol.frames.AckRange;

public class AckUtil {

    public static boolean contains(final AckFrame frame, final long packetNumber) {
        for (final AckRange range : frame.getRanges()) {
            if (packetNumber >= range.getSmallest() && packetNumber <= range.getLargest()) {
                return true;
            }
        }
        return false;
    }
}
