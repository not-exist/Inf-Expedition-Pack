package com.inf.expedition.event;

import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public class RunEndedEvent extends Event {
    private final MinecraftServer server;
    private final boolean success;
    private final List<ServerPlayer> party;

    public RunEndedEvent(MinecraftServer server, boolean success, List<ServerPlayer> party) {
        this.server = server;
        this.success = success;
        this.party = List.copyOf(party);
    }

    public MinecraftServer getServer() {
        return server;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<ServerPlayer> getParty() {
        return party;
    }
}
