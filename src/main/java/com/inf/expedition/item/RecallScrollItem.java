package com.inf.expedition.item;

import com.inf.expedition.run.ExpeditionSavedData;
import com.inf.expedition.run.RunManager;
import com.inf.expedition.run.RunState;
import com.inf.expedition.world.DimensionIds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RecallScrollItem extends Item {
    public RecallScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && serverPlayer.getServer() != null) {
            if (!level.dimension().equals(DimensionIds.RUN_KEY)) {
                player.displayClientMessage(
                        Component.translatable("gui.inf_expedition.recall.wrong_dimension"), true);
                return InteractionResultHolder.fail(stack);
            }
            RunState run = ExpeditionSavedData.get(serverPlayer.getServer()).run();
            if (!run.isPartyMember(serverPlayer.getUUID())) {
                player.displayClientMessage(
                        Component.translatable("gui.inf_expedition.recall.not_member"), true);
                return InteractionResultHolder.fail(stack);
            }
            RunManager.leave(serverPlayer);
            stack.shrink(1);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.success(stack);
    }
}
