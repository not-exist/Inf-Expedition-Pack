package com.inf.expedition.room;

import com.inf.expedition.data.RoomDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RoomPlacer {
    public static final String ROOM_TAG = "inf_expedition:room";
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomPlacer.class);

    private RoomPlacer() {
    }

    public static Optional<RoomInstance> place(
            ServerLevel level,
            RoomDefinition definition,
            int nodeId,
            BlockPos center) {
        Optional<StructureTemplate> optional = level.getStructureManager().get(definition.structure());
        if (optional.isEmpty()) {
            LOGGER.warn("Missing room structure {}", definition.structure());
            return Optional.empty();
        }
        StructureTemplate template = optional.get();
        var size = template.getSize();
        BlockPos origin = center.offset(-size.getX() / 2, 0, -size.getZ() / 2);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE);
        template.placeInWorld(level, origin, origin, settings, level.random, 2);

        AABB bounds = new AABB(
                origin.getX() - 1,
                origin.getY() - 1,
                origin.getZ() - 1,
                origin.getX() + size.getX() + 1,
                origin.getY() + size.getY() + 1,
                origin.getZ() + size.getZ() + 1);
        RoomMarkers markers = scanMarkers(level, origin, size);
        if (markers.spawns().isEmpty() || markers.exits().isEmpty()) {
            LOGGER.warn("Room {} is missing spawn or exit markers", definition.id());
            return Optional.empty();
        }
        return Optional.of(new RoomInstance(
                UUID.randomUUID(),
                nodeId,
                definition.type(),
                definition.id(),
                definition.structure(),
                bounds,
                markers));
    }

    public static void clear(ServerLevel level, RoomInstance room) {
        clear(level, room.bounds());
    }

    public static void clear(ServerLevel level, AABB bounds) {
        BlockPos min = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
        BlockPos max = BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockState(pos).is(Blocks.BEDROCK)) {
                continue;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        level.getEntitiesOfClass(Entity.class, bounds, entity -> !(entity instanceof Player))
                .forEach(Entity::discard);
    }

    private static RoomMarkers scanMarkers(ServerLevel level, BlockPos origin, net.minecraft.core.Vec3i size) {
        List<BlockPos> spawns = new ArrayList<>();
        List<BlockPos> mobs = new ArrayList<>();
        List<BlockPos> bosses = new ArrayList<>();
        List<BlockPos> rewards = new ArrayList<>();
        List<BlockPos> exits = new ArrayList<>();
        List<BlockPos> shops = new ArrayList<>();
        List<BlockPos> rests = new ArrayList<>();
        BlockPos max = origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
        for (BlockPos pos : BlockPos.betweenClosed(origin, max)) {
            if (!level.getBlockState(pos).is(Blocks.JIGSAW)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof JigsawBlockEntity jigsaw)) {
                continue;
            }
            String name = jigsaw.getName().getPath();
            BlockPos marker = pos.immutable();
            switch (name) {
                case "spawn" -> spawns.add(marker);
                case "mob" -> mobs.add(marker);
                case "boss" -> bosses.add(marker);
                case "reward" -> rewards.add(marker);
                case "exit" -> exits.add(marker);
                case "shop" -> shops.add(marker);
                case "rest" -> rests.add(marker);
                default -> {
                }
            }
            level.setBlock(marker, Blocks.AIR.defaultBlockState(), 3);
        }
        for (BlockPos exit : exits) {
            level.setBlockAndUpdate(exit, Blocks.LODESTONE.defaultBlockState());
        }
        return new RoomMarkers(spawns, mobs, bosses, rewards, exits, shops, rests);
    }
}
