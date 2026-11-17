package com.inf.expedition.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.registries.BuiltInRegistries;

public class QuestBoardBlock extends Block {
    public QuestBoardBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            Item book = BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath("ftbquests", "book"));
            if (book == Items.AIR) {
                player.displayClientMessage(
                        Component.translatable("gui.inf_expedition.quest.missing"), true);
            } else if (!player.getInventory().contains(new ItemStack(book))) {
                player.getInventory().add(new ItemStack(book));
                player.displayClientMessage(
                        Component.translatable("gui.inf_expedition.quest.received"), true);
            } else {
                player.displayClientMessage(
                        Component.translatable("gui.inf_expedition.quest.already"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
