package com.inf.expedition.event;

import com.inf.expedition.room.RoomInstance;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public class RoomEnteredEvent extends Event {
    private final ServerPlayer player;
    private final RoomInstance room;

    public RoomEnteredEvent(ServerPlayer player, RoomInstance room) {
        this.player = player;
        this.room = room;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public RoomInstance getRoom() {
        return room;
    }
}
