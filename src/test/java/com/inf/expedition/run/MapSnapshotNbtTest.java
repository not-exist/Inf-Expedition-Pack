package com.inf.expedition.run;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.inf.expedition.map.MapConfig;
import com.inf.expedition.map.MapGenerator;
import com.inf.expedition.map.MapSnapshot;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class MapSnapshotNbtTest {
    @Test
    void roundTripPreservesSnapshot() {
        MapSnapshot snapshot = new MapGenerator(99L).generate(MapConfig.DEFAULT).snapshot();
        CompoundTag tag = MapSnapshotNbt.write(snapshot);
        assertEquals(snapshot, MapSnapshotNbt.read(tag));
    }
}
