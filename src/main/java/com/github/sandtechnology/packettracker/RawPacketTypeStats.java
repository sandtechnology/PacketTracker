package com.github.sandtechnology.packettracker;

import java.util.concurrent.atomic.AtomicLong;

public class RawPacketTypeStats {
    AtomicLong count;
    AtomicLong bytes;

    public RawPacketTypeStats(long count, long bytes) {
        this.count = new AtomicLong(count);
        this.bytes = new AtomicLong(bytes);
    }
}
