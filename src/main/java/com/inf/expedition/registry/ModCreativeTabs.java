package com.inf.expedition.registry;

import com.inf.expedition.Expedition;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Expedition.MODID);

    public static final Supplier<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.inf_expedition"))
                    .icon(() -> new ItemStack(ModItems.RECALL_SCROLL.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.EXPEDITION_ALTAR_ITEM.get());
                        output.accept(ModItems.QUEST_BOARD_ITEM.get());
                        output.accept(ModItems.RECALL_SCROLL.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
