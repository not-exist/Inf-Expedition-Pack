package com.inf.expedition.client;

import com.inf.expedition.Expedition;
import com.inf.expedition.network.MapOpenRequestPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Expedition.MODID, value = Dist.CLIENT)
public final class ExpeditionClient {
    private ExpeditionClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (MapKeyMapping.OPEN_MAP.consumeClick()) {
            PacketDistributor.sendToServer(new MapOpenRequestPayload());
        }
    }

    public static void openMap() {
        if (MapClientData.snapshot() != null && Minecraft.getInstance().screen == null) {
            Minecraft.getInstance().setScreen(new MapScreen());
        }
    }
}
