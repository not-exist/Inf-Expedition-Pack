package com.inf.expedition.run;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.inf.expedition.map.MapConfig;
import com.inf.expedition.map.MapGenerator;
import com.inf.expedition.map.MapSnapshot;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class MapSyncPayloadTest {
    @Test
    void mapTagRoundTripsThroughSnapshotNbt() {
        MapSnapshot snapshot = new MapGenerator(5L).generate(MapConfig.DEFAULT).snapshot();
        CompoundTag tag = MapSnapshotNbt.write(snapshot);
        assertEquals(snapshot, MapSnapshotNbt.read(tag));
    }
}
