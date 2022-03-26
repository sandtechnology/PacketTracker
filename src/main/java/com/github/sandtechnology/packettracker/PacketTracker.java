package com.github.sandtechnology.packettracker;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class PacketTracker extends JavaPlugin {

    private final Cache<UUID, List<PacketTypeStatsMinute>> playersMapStats = CacheBuilder.newBuilder().expireAfterAccess(2, TimeUnit.MINUTES).build();
    private final PlayerPacketListener playerPacketListener = new PlayerPacketListener(this);
    private ProtocolManager protocolManager;

    public String getSummary() {
        StringJoiner joiner = new StringJoiner("\n");
        Server server = getServer();
        joiner.add("列表如下：");
        for (Map.Entry<UUID, List<PacketTypeStatsMinute>> uuidListEntry : playersMapStats.asMap().entrySet()) {
            joiner.add(server.getOfflinePlayer(uuidListEntry.getKey()).getName() + ":" + uuidListEntry.getValue().stream().mapToDouble(PacketTypeStatsMinute::getPerotValue).sum());
        }
        return joiner.toString();
    }
    public String getRealTimeDetail(String playerName) {
        StringJoiner joiner = new StringJoiner("\n");
        Player player = getServer().getPlayer(playerName);
        if (player == null) {
            return "null";
        }
        Map<PacketType,RawPacketTypeStats> statsMap = playerPacketListener.getPlayersMapStats().get(player.getUniqueId());
        if (statsMap == null) {
            return "null";
        }
        joiner.add("实时列表如下：");
        for (Map.Entry<PacketType, RawPacketTypeStats> statsEntry : statsMap.entrySet()) {
            RawPacketTypeStats rawPacketTypeStats=statsEntry.getValue();
            joiner.add(statsEntry.getKey().getPacketClass().getSimpleName()+": "+rawPacketTypeStats.count+"-> "+rawPacketTypeStats.bytes);
        }
        return joiner.toString();
    }
    public String getDetail(String playerName) {
        StringJoiner joiner = new StringJoiner("\n");
        Player player = getServer().getPlayer(playerName);
        if (player == null) {
            return "null";
        }
        List<PacketTypeStatsMinute> list = playersMapStats.asMap().get(player.getUniqueId());
        if (list == null) {
            return getRealTimeDetail(playerName);
        }
        joiner.add("列表如下：");
        for (PacketTypeStatsMinute statsMinute : list) {
            joiner.add(statsMinute.toString());
        }
        return joiner.toString();
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(playerPacketListener);
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                    for (Map.Entry<UUID, Map<PacketType, RawPacketTypeStats>> uuidMapEntry : playerPacketListener.getPlayersMapStats().entrySet()) {
                        UUID uuid=uuidMapEntry.getKey();
                        Map<PacketType, RawPacketTypeStats> statsMap = uuidMapEntry.getValue();
                        List<PacketTypeStatsMinute> packetTypeStatsMinuteList= playersMapStats.getIfPresent(uuid);
                        if(packetTypeStatsMinuteList==null){
                            playersMapStats.put(uuid,statsMap.entrySet().parallelStream().map(e->{
                                RawPacketTypeStats stats=e.getValue();
                                PacketTypeStatsMinute packetTypeStatsMinute = new PacketTypeStatsMinute(e.getKey(), Math.min(0.1, stats.count.get() / 60.0), Math.min(0.1, stats.bytes.get() / 60.0));
                                stats.count.set(0);
                                stats.bytes.set(0);
                                return packetTypeStatsMinute;
                            }).sorted().collect(Collectors.toList()));
                        }else {
                            for (PacketTypeStatsMinute packetTypeStatsMinute : packetTypeStatsMinuteList) {
                                RawPacketTypeStats stats=statsMap.remove(packetTypeStatsMinute.getPacketType());
                                if(stats!=null){
                                    packetTypeStatsMinute.setBytes((packetTypeStatsMinute.getBytes()+(stats.bytes.get()/60.0))/2);
                                    packetTypeStatsMinute.setCount((packetTypeStatsMinute.getCount()+(stats.count.get()/60.0))/2);
                                }
                            }
                            packetTypeStatsMinuteList.addAll(statsMap.entrySet().parallelStream().map(e->{
                                RawPacketTypeStats stats=e.getValue();
                                PacketTypeStatsMinute packetTypeStatsMinute = new PacketTypeStatsMinute(e.getKey(), Math.min(0.1, stats.count.get() / 60.0), Math.min(0.1, stats.bytes.get() / 60.0));
                                stats.count.set(0);
                                stats.bytes.set(0);
                                return packetTypeStatsMinute;
                            }).collect(Collectors.toList()));
                            Collections.sort(packetTypeStatsMinuteList);
                        }
                    }
                }
                , 20, 20 * 60);
        getCommand("packetSum").setExecutor((sender, command, label, args) -> {
            sender.sendMessage(getSummary());
            return true;
        });
        getCommand("packetDetailRealTime").setExecutor((sender, command, label, args) -> {
            if (args.length != 1) {
                sender.sendMessage("请提供玩家名称");
                return true;
            }
            sender.sendMessage(getRealTimeDetail(args[0]));
            return true;
        });
        getCommand("packetDetailRealTime").setTabCompleter((sender, command, alias, args) -> getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
        getCommand("packetDetail").setExecutor((sender, command, label, args) -> {
            if (args.length != 1) {
                sender.sendMessage("请提供玩家名称");
                return true;
            }
            sender.sendMessage(getDetail(args[0]));
            return true;
        });
        getCommand("packetDetail").setTabCompleter((sender, command, alias, args) -> getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
    }

    @Override
    public void onDisable() {
        protocolManager.removePacketListeners(this);
        getServer().getScheduler().cancelTasks(this);
    }
}
