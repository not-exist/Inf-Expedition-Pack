package com.inf.expedition.event;

import com.inf.expedition.room.RoomInstance;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

public class RoomCompletedEvent extends Event {
    private final MinecraftServer server;
    private final RoomInstance room;
    private final boolean success;

    public RoomCompletedEvent(MinecraftServer server, RoomInstance room, boolean success) {
        this.server = server;
        this.room = room;
        this.success = success;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public RoomInstance getRoom() {
        return room;
    }

    public boolean isSuccess() {
        return success;
    }
}
