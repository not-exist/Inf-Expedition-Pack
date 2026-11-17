package com.inf.expedition.room;

import com.inf.expedition.Expedition;
import com.inf.expedition.ExpeditionConfig;
import com.inf.expedition.data.EncounterDefinition;
import com.inf.expedition.data.EncounterPlan;
import com.inf.expedition.data.EncounterScaler;
import com.inf.expedition.run.RunManager;
import com.inf.expedition.world.DimensionIds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EncounterManager {
    public static final String TAG_ROOM_MOB = "inf_expedition:room_mob";
    private static final Logger LOGGER = LoggerFactory.getLogger(EncounterManager.class);
    private static final ResourceLocation HEALTH_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(Expedition.MODID, "act_health");
    private static final ResourceLocation DAMAGE_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(Expedition.MODID, "act_damage");

    private static RoomInstance room;
    private static EncounterDefinition encounter;
    private static List<ServerPlayer> party = List.of();
    private static List<EncounterPlan.WavePlan> waves = List.of();
    private static int waveIndex = -1;
    private static final Set<UUID> aliveMobs = new HashSet<>();
    private static final Map<UUID, ServerBossEvent> bossBars = new HashMap<>();

    private EncounterManager() {
    }

    public static void start(
            ServerLevel level,
            RoomInstance roomInstance,
            EncounterDefinition definition,
            List<ServerPlayer> players) {
        clear();
        room = roomInstance;
        encounter = definition;
        party = List.copyOf(players);
        waves = EncounterPlan.build(definition, 1, Math.max(1, players.size()),
                ExpeditionConfig.maxMobsPerWave());
        waveIndex = -1;
        schedule(level, 40);
    }

    public static void onMobDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (!living.getTags().contains(TAG_ROOM_MOB)) {
            return;
        }
        if (!aliveMobs.remove(living.getUUID())) {
            return;
        }
        removeBossBar(living.getUUID());
        if (aliveMobs.isEmpty() && room != null) {
            MinecraftServer server = living.getServer();
            if (server != null) {
                schedule(server, 60);
            }
        }
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        Entity victim = event.getEntity();
        if (attacker != null && victim != null
                && attacker.getTags().contains(TAG_ROOM_MOB)
                && victim.getTags().contains(TAG_ROOM_MOB)) {
            event.setCanceled(true);
        }
    }

    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (entity.getTags().contains(TAG_ROOM_MOB)
                && target != null
                && target.getTags().contains(TAG_ROOM_MOB)) {
            event.setCanceled(true);
        }
    }

    public static boolean isActive() {
        return room != null;
    }

    public static void tick(MinecraftServer server) {
        if (room == null || aliveMobs.isEmpty()) {
            return;
        }
        ServerLevel level = server.getLevel(DimensionIds.RUN_KEY);
        if (level == null) {
            return;
        }
        for (UUID mobId : new ArrayList<>(aliveMobs)) {
            Entity entity = level.getEntity(mobId);
            if (entity == null) {
                removeBossBar(mobId);
                if (aliveMobs.remove(mobId) && aliveMobs.isEmpty() && room != null) {
                    schedule(server, 60);
                }
                continue;
            }
            if (!room.contains(entity.blockPosition())) {
                var center = room.center();
                entity.teleportTo(center.x, center.y, center.z);
            }
            if (entity instanceof Mob mob && mob.getTarget() == null) {
                ServerPlayer target = firstAlivePartyMember();
                if (target != null) {
                    mob.setTarget(target);
                }
            }
            ServerBossEvent bar = bossBars.get(mobId);
            if (bar != null && entity instanceof LivingEntity living) {
                bar.setProgress(Math.max(0.0F, living.getHealth() / living.getMaxHealth()));
            }
        }
    }

    public static void clear() {
        bossBars.values().forEach(bar -> {
            bar.removeAllPlayers();
            bar.setVisible(false);
        });
        bossBars.clear();
        aliveMobs.clear();
        waves = List.of();
        waveIndex = -1;
        room = null;
        encounter = null;
        party = List.of();
    }

    private static void schedule(ServerLevel level, int delay) {
        schedule(level.getServer(), delay);
    }

    private static void schedule(MinecraftServer server, int delay) {
        server.tell(new TickTask(server.getTickCount() + delay, () -> spawnNext(server)));
    }

    private static void spawnNext(MinecraftServer server) {
        if (room == null || encounter == null) {
            return;
        }
        ServerLevel level = server.getLevel(DimensionIds.RUN_KEY);
        if (level == null) {
            return;
        }
        waveIndex++;
        if (waveIndex >= waves.size()) {
            finish(level);
            return;
        }
        int spawned = 0;
        for (EncounterPlan.MobPlan plan : waves.get(waveIndex).mobs()) {
            for (int index = 0; index < plan.count(); index++) {
                boolean bossBar = encounter.boss() && spawned == 0;
                if (spawnMob(level, plan.entity(), spawnPosition(level, encounter.boss()), bossBar)) {
                    spawned++;
                }
            }
        }
        if (aliveMobs.isEmpty()) {
            LOGGER.error("Encounter {} produced no mobs; spawning fallback", encounter.id());
            int count = EncounterFallback.countForWave(encounter.boss());
            ResourceLocation fallback = EncounterFallback.forWave(encounter.boss());
            for (int index = 0; index < count; index++) {
                spawnMob(level, fallback, spawnPosition(level, encounter.boss()),
                        encounter.boss() && index == 0);
            }
        }
        if (aliveMobs.isEmpty()) {
            LOGGER.error("Fallback also failed for encounter {}", encounter.id());
            for (ServerPlayer player : party) {
                player.displayClientMessage(
                        Component.translatable("gui.inf_expedition.encounter.invalid"), false);
            }
            return;
        }
    }

    private static boolean spawnMob(
            ServerLevel level,
            ResourceLocation entityId,
            BlockPos position,
            boolean bossBar) {
        EntityType<?> type = EntityType.byString(entityId.toString()).orElse(null);
        if (type == null) {
            LOGGER.warn("Unknown encounter entity {}", entityId);
            return false;
        }
        Entity entity = type.create(level);
        if (entity == null) {
            return false;
        }
        entity.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
        entity.addTag(TAG_ROOM_MOB);
        entity.getPersistentData().putString(RoomPlacer.ROOM_TAG, room.id().toString());
        scale(entity);
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
            ServerPlayer target = firstAlivePartyMember();
            if (target != null) {
                mob.setTarget(target);
            }
        }
        level.addFreshEntity(entity);
        aliveMobs.add(entity.getUUID());
        if (bossBar && entity instanceof LivingEntity living) {
            createBossBar(living);
        }
        return true;
    }

    private static BlockPos spawnPosition(ServerLevel level, boolean boss) {
        if (boss && !room.markers().bosses().isEmpty()) {
            return room.markers().bosses().getFirst();
        }
        if (!room.markers().mobs().isEmpty()) {
            return room.markers().mobs().get(level.random.nextInt(room.markers().mobs().size()));
        }
        return BlockPos.containing(room.center());
    }

    private static void scale(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        int partySize = Math.max(1, party.size());
        double health = EncounterScaler.healthMultiplier(1, partySize, encounter.scale());
        double damage = EncounterScaler.damageMultiplier(1, partySize, encounter.scale());
        AttributeInstance maxHealth = living.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.addPermanentModifier(new AttributeModifier(
                    HEALTH_MODIFIER, health - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            living.setHealth(living.getMaxHealth());
        }
        AttributeInstance attackDamage = living.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.addPermanentModifier(new AttributeModifier(
                    DAMAGE_MODIFIER, damage - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    private static void createBossBar(LivingEntity boss) {
        if (!ExpeditionConfig.bossBar()) {
            return;
        }
        ServerBossEvent bar = new ServerBossEvent(
                boss.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        bar.setVisible(true);
        for (ServerPlayer player : party) {
            bar.addPlayer(player);
        }
        bossBars.put(boss.getUUID(), bar);
    }

    private static void removeBossBar(UUID mobId) {
        ServerBossEvent bar = bossBars.remove(mobId);
        if (bar != null) {
            bar.removeAllPlayers();
            bar.setVisible(false);
        }
    }

    private static ServerPlayer firstAlivePartyMember() {
        return party.stream()
                .filter(player -> player.isAlive() && !player.isSpectator())
                .findFirst()
                .orElse(null);
    }

    private static void finish(ServerLevel level) {
        if (room == null) {
            return;
        }
        spawnReward(level);
        MinecraftServer server = level.getServer();
        clear();
        RunManager.complete(server, true);
    }

    private static void spawnReward(ServerLevel level) {
        if (encounter != null && encounter.rewardLoot() != null) {
            spawnRewardChest(level, room, encounter.rewardLoot());
        }
    }

    public static void spawnRewardChest(
            ServerLevel level,
            RoomInstance roomInstance,
            ResourceLocation lootId) {
        if (roomInstance == null || lootId == null || roomInstance.markers().rewards().isEmpty()) {
            return;
        }
        BlockPos position = roomInstance.markers().rewards().getFirst();
        level.setBlockAndUpdate(position, Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(position) instanceof ChestBlockEntity chest) {
            chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, lootId));
            chest.setLootTableSeed(level.random.nextLong());
        }
    }
}
