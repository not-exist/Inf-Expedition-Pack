package com.inf.expedition.room;

import java.util.List;
import net.minecraft.core.BlockPos;

public record RoomMarkers(
        List<BlockPos> spawns,
        List<BlockPos> mobs,
        List<BlockPos> bosses,
        List<BlockPos> rewards,
        List<BlockPos> exits,
        List<BlockPos> shops,
        List<BlockPos> rests) {

    public static final RoomMarkers EMPTY = new RoomMarkers(
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

    public RoomMarkers {
        spawns = List.copyOf(spawns);
        mobs = List.copyOf(mobs);
        bosses = List.copyOf(bosses);
        rewards = List.copyOf(rewards);
        exits = List.copyOf(exits);
        shops = List.copyOf(shops);
        rests = List.copyOf(rests);
    }

    public List<BlockPos> byName(String name) {
        return switch (name) {
            case "spawn" -> spawns;
            case "mob" -> mobs;
            case "boss" -> bosses;
            case "reward" -> rewards;
            case "exit" -> exits;
            case "shop" -> shops;
            case "rest" -> rests;
            default -> List.of();
        };
    }
}
