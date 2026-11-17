package com.inf.expedition.registry;

import com.inf.expedition.Expedition;
import com.inf.expedition.block.ExpeditionAltarBlock;
import com.inf.expedition.block.QuestBoardBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Expedition.MODID);

    public static final DeferredBlock<ExpeditionAltarBlock> EXPEDITION_ALTAR =
            BLOCKS.register("expedition_altar",
                    () -> new ExpeditionAltarBlock(BlockBehaviour.Properties.of()
                            .strength(-1.0F, 3_600_000.0F)));
    public static final DeferredBlock<QuestBoardBlock> QUEST_BOARD =
            BLOCKS.register("quest_board",
                    () -> new QuestBoardBlock(BlockBehaviour.Properties.of()
                            .strength(-1.0F, 3_600_000.0F)));

    private ModBlocks() {
    }
}
