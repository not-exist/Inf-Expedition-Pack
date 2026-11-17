package com.inf.expedition.block;

import com.inf.expedition.run.RunManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ExpeditionAltarBlock extends Block {
    public ExpeditionAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.getServer() != null) {
                RunManager.start(serverPlayer.getServer(),
                        serverPlayer.getServer().getPlayerList().getPlayers());
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
