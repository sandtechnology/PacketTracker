package com.github.sandtechnology.packettracker;

import com.comphenix.protocol.PacketType;

import java.util.Objects;

public class PacketTypeStatsMinute implements Comparable<PacketTypeStatsMinute> {
    private double count;
    private double bytes;
    private final PacketType packetType;

    public PacketTypeStatsMinute(PacketType packetType, double count, double bytes) {
        this.count = toTwoBits(count);
        this.bytes = toTwoBits(bytes);
        this.packetType = packetType;
    }

    public void setCount(double count) {
        this.count = count;
    }

    public void setBytes(double bytes) {
        this.bytes = bytes;
    }

    public double getBytes() {
        return bytes;
    }

    public double getCount() {
        return count;
    }

    public PacketType getPacketType() {
        return packetType;
    }

    private static double toTwoBits(double d){
        return ((int)(d*10000))*0.0001;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PacketTypeStatsMinute that = (PacketTypeStatsMinute) o;
        return Double.compare(that.count, count) == 0 && Double.compare(that.bytes, bytes) == 0 && Objects.equals(packetType, that.packetType);
    }

    @Override
    public String toString() {
        return packetType.getPacketClass().getSimpleName() + ": " + toTwoBits(count) + "-> " + toTwoBits(bytes);
    }

    public double getPerotValue() {
        return toTwoBits(bytes * count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(count, bytes, packetType);
    }

    @Override
    public int compareTo(PacketTypeStatsMinute o) {
        return Double.compare(count * bytes, o.count * o.bytes);
    }
}
