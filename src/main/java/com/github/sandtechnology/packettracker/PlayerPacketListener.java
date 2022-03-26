package com.github.sandtechnology.packettracker;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class PlayerPacketListener extends PacketAdapter {
    private static final PacketType[] packetTypes;

    static {
        List<PacketType> list = new ArrayList<>();
        for (PacketType packetType : PacketType.Play.Server.getInstance().values()) {
            if (packetType.isSupported()) {
                list.add(packetType);
            }
        }
        packetTypes = list.toArray(new PacketType[0]);
    }

    private final Class<?> temporaryPlayerClass;
    private final Cache<UUID, Map<PacketType, RawPacketTypeStats>> playersMapStats = CacheBuilder.newBuilder()

            .expireAfterWrite(30, TimeUnit.SECONDS).build();

    public PlayerPacketListener(Plugin plugin) {
        super(plugin, ListenerPriority.MONITOR, packetTypes);
        Class<?> localTemporaryPlayerClass;
        try {
            localTemporaryPlayerClass = Class.forName("com.comphenix.protocol.injector.temporary.TemporaryPlayer");
        } catch (ClassNotFoundException unused1) {
            try {
                localTemporaryPlayerClass = Class.forName("com.comphenix.protocol.injector.server.TemporaryPlayer");
            } catch (ClassNotFoundException unused2) {
                plugin.getLogger().log(Level.WARNING, "Failed to find temporaryPlayer class!");
                localTemporaryPlayerClass = this.getClass();
            }
        }
        this.temporaryPlayerClass = localTemporaryPlayerClass;
    }

    public Map<UUID, Map<PacketType, RawPacketTypeStats>> getPlayersMapStats() {
        return playersMapStats.asMap();
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        if (!temporaryPlayerClass.isInstance(player)) {
            Map<UUID, Map<PacketType, RawPacketTypeStats>> mapView = playersMapStats.asMap();
            UUID uuid = player.getUniqueId();
            Map<PacketType, RawPacketTypeStats> statsMap = mapView.get(uuid);
            if (statsMap == null) {
                statsMap = new ConcurrentHashMap<>();
                mapView.put(uuid, statsMap);
            }
            Object packetHandle = event.getPacket().getHandle();
            final int packetSize = PacketTypeSizeCache.getPacketSize(packetHandle);
            if (packetSize == 0) {
                plugin.getLogger().log(Level.WARNING, "packetSize==0 for " + packetHandle.getClass().getName());
            }
            statsMap.compute(event.getPacketType(), (packetType, packetTypeStats) -> {
                if (packetTypeStats == null) {
                    return new RawPacketTypeStats(1, packetSize);
                } else {
                    packetTypeStats.count.incrementAndGet();
                    packetTypeStats.bytes.addAndGet(packetSize);
                    return packetTypeStats;
                }
            });
        }
    }
}
