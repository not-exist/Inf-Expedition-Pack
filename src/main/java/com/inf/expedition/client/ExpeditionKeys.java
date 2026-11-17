package com.inf.expedition.client;

import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class ExpeditionKeys {
    private ExpeditionKeys() {
    }

    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(MapKeyMapping.OPEN_MAP);
    }
}
