package com.inf.expedition.registry;

import com.inf.expedition.Expedition;
import com.inf.expedition.item.RecallScrollItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Expedition.MODID);

    public static final DeferredItem<BlockItem> EXPEDITION_ALTAR_ITEM =
            ITEMS.register("expedition_altar",
                    () -> new BlockItem(ModBlocks.EXPEDITION_ALTAR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> QUEST_BOARD_ITEM =
            ITEMS.register("quest_board",
                    () -> new BlockItem(ModBlocks.QUEST_BOARD.get(), new Item.Properties()));
    public static final DeferredItem<RecallScrollItem> RECALL_SCROLL =
            ITEMS.register("recall_scroll",
                    () -> new RecallScrollItem(new Item.Properties().stacksTo(16)));

    private ModItems() {
    }
}
