package com.inf.expedition;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import com.inf.expedition.data.ExpeditionDataReloadListener;
import com.inf.expedition.room.RoomExitHandler;
import com.inf.expedition.room.EncounterManager;
import com.inf.expedition.room.RoomProtection;
import com.inf.expedition.command.ExpeditionCommand;
import com.inf.expedition.registry.ModBlocks;
import com.inf.expedition.registry.ModCreativeTabs;
import com.inf.expedition.registry.ModItems;
import com.inf.expedition.network.ExpeditionNetwork;
import com.inf.expedition.run.RunManager;
import com.inf.expedition.world.HubManager;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;

@Mod(Expedition.MODID)
public final class Expedition {
    public static final String MODID = "inf_expedition";

    public Expedition(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        modBus.addListener(ExpeditionNetwork::register);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(com.inf.expedition.client.ExpeditionKeys::onRegisterKeys);
        }
        container.registerConfig(ModConfig.Type.COMMON, ExpeditionConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(ExpeditionDataReloadListener::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(ExpeditionCommand::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(HubManager::onLogin);
        NeoForge.EVENT_BUS.addListener(HubManager::onRespawn);
        NeoForge.EVENT_BUS.addListener(RunManager::onDeath);
        NeoForge.EVENT_BUS.addListener(EncounterManager::onMobDeath);
        NeoForge.EVENT_BUS.addListener(EncounterManager::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(EncounterManager::onChangeTarget);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> RunManager.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener(RoomProtection::onBreak);
        NeoForge.EVENT_BUS.addListener(RoomProtection::onPlace);
        NeoForge.EVENT_BUS.addListener(RoomProtection::onExplosion);
        NeoForge.EVENT_BUS.addListener(RoomProtection::onMobGriefing);
        NeoForge.EVENT_BUS.addListener(RoomProtection::onDestroyBlock);
        NeoForge.EVENT_BUS.addListener(RoomExitHandler::onRightClick);
    }
}
