package com.inf.expedition.data;

public final class EncounterScaler {
    private EncounterScaler() {
    }

    public static double healthMultiplier(int act, int partySize, EncounterDefinition.Scale scale) {
        int safeAct = Math.max(1, act);
        int safeParty = Math.max(1, partySize);
        return (1.0 + scale.healthPerAct() * (safeAct - 1))
                * (1.0 + scale.healthPerExtraPlayer() * (safeParty - 1));
    }

    public static double damageMultiplier(int act, int partySize, EncounterDefinition.Scale scale) {
        int safeAct = Math.max(1, act);
        int safeParty = Math.max(1, partySize);
        return (1.0 + scale.damagePerAct() * (safeAct - 1))
                * (1.0 + scale.damagePerExtraPlayer() * (safeParty - 1));
    }

    public static int scaledCount(int base, int act, double countPerAct, int maxMobs) {
        if (base <= 0) {
            return 0;
        }
        int safeAct = Math.max(1, act);
        int cap = Math.max(1, maxMobs);
        double raw = base * (1.0 + countPerAct * (safeAct - 1));
        return Math.min(cap, Math.max(1, (int) Math.ceil(raw)));
    }
}
