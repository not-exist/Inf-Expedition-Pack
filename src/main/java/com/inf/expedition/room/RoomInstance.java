package com.inf.expedition.room;

import com.inf.expedition.data.RoomType;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RoomInstance {
    private final UUID id;
    private final int nodeId;
    private final RoomType type;
    private final String definitionId;
    private final ResourceLocation structure;
    private final AABB bounds;
    private final RoomMarkers markers;
    private boolean completed;

    public RoomInstance(
            UUID id,
            int nodeId,
            RoomType type,
            String definitionId,
            ResourceLocation structure,
            AABB bounds,
            RoomMarkers markers) {
        this.id = id;
        this.nodeId = nodeId;
        this.type = type;
        this.definitionId = definitionId;
        this.structure = structure;
        this.bounds = bounds;
        this.markers = markers;
    }

    public UUID id() {
        return id;
    }

    public int nodeId() {
        return nodeId;
    }

    public RoomType type() {
        return type;
    }

    public String definitionId() {
        return definitionId;
    }

    public ResourceLocation structure() {
        return structure;
    }

    public AABB bounds() {
        return bounds;
    }

    public RoomMarkers markers() {
        return markers;
    }

    public boolean completed() {
        return completed;
    }

    public void markCompleted() {
        completed = true;
    }

    public boolean contains(BlockPos pos) {
        return bounds.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public Vec3 center() {
        return bounds.getCenter();
    }
}
