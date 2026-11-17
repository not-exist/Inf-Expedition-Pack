package com.inf.expedition.event;

import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public class RunStartedEvent extends Event {
    private final MinecraftServer server;
    private final List<ServerPlayer> players;

    public RunStartedEvent(MinecraftServer server, List<ServerPlayer> players) {
        this.server = server;
        this.players = List.copyOf(players);
    }

    public MinecraftServer getServer() {
        return server;
    }

    public List<ServerPlayer> getPlayers() {
        return players;
    }
}
