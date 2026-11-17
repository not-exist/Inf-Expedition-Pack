# 锻途：千锤之路 v0.1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Minecraft 1.21.1 / NeoForge 上做出《锻途：千锤之路》v0.1 纵向切片：大厅 → 节点地图 → 8~10 个 NBT 房间 → Act 1 Boss → 回大厅的可玩循环，并用 Inf 的锻刀作为核心成长手段。

**Architecture:** 新增独立 mod `inf_expedition`（不依赖 Inf 源码）：纯 Java 地图生成/运行状态内核 + 一层 NeoForge 适配（维度、命令、房间 NBT 放置、AABB 保护、波次、网络/地图屏）。整合包层用 KubeJS/FTB Quests/FancyMenu 和约 30 个第三方模组提供内容；房间与遭遇由数据包驱动。

**Tech Stack:** Java 21、NeoForge 21.1.266、ModDevGradle 2.0.148、JUnit 5、KubeJS 2101、FTB Quests 2101、Python 3.14（房间 NBT/贴图/打包脚本）、PIL 12、PyYAML 6。

**Spec:** `docs/superpowers/specs/2026-11-16-inf-expedition-pack-design.md`

## Global Constraints

- 仓库：`not-exist/Inf-Expedition-Pack`；核心依赖通过 `inf-core` 子模块固定于 `v0.2.0`。
- Minecraft `1.21.1`，NeoForge `21.1.266`，Java 21，官方 Mojang 映射，ModDevGradle `2.0.148`。
- 构建命令统一带 `JAVA_HOME=$JAVA_HOME`。
- 新 mod：mod id `inf_expedition`，包名 `com.inf.expedition`，版本 `0.1.0`，许可证沿用仓库 `All Rights Reserved`。
- **不修改 Inf 的源码、配方、数值、物品 ID**；只通过 `inf:hammer`、`inf:*_blank`、`inf:forge_table` 等既有 ID 集成。
- 玩家可见文本必须同时有 `zh_cn` 与 `en_us`；`zh_cn` 为默认。
- 服务端权威：地图移动、房间完成、Boss 死亡、死亡/通关判定只在服务端执行。
- 单人优先；地图/房间/死亡状态按队伍共享设计，双人 LAN 不作为 v0.1 阻塞项（P6 可选）。
- 肉鸽规则：开局与回大厅都清空背包；v0.1 没有跨局物品仓库，只有 `clears` 元进度。
- v0.1 地图参数：8 层 × 3 列 × 2 路径，Boss 层 8；参数必须可配置，不能写死。
- 不复制参考包的脚本、NBT、贴图、配置或自定义 jar。
- 第三方模组不提交进仓库；`pack/mods.yaml` 只存 project/file id 与官方下载地址。
- 每个任务结束必须 `./gradlew build` 通过；纯逻辑任务必须 `./gradlew test` 通过；提交信息用 conventional commits。

## Review Focus

1. **同一房间内重复点击地图节点 / 双击移动**：第二次请求必须被拒绝，不能放置第二个房间、不能把队伍传进错误区域、不能丢失房间状态。→ Task 3 `RunStateTest.movingTwiceIsRejected` + Task 6 命令校验。
2. **房间 NBT 缺失、标记缺失或尺寸异常**：不能把玩家困在旧房间或虚空；必须拒绝进入、保留当前房间、给玩家可读提示。→ Task 5 的失败路径 + Task 16 手动验收（缺 NBT/标记时保留当前房间）。
3. **死亡、重连、最后一人退出**：不能复制物品、不能把旁观者留在已清空房间、不能在队伍为空时继续刷波次。→ Task 3 `RunStateTest` + Task 6 生命周期 + Task 15 手动验收。
4. **保护规则误伤合法交互**：锻刀台、奖励箱、磁石出口、商人物品必须可用；破坏/放置/爆炸/生物破坏必须被拒绝；怪跑出区域必须被拉回。→ Task 5 `RoomProtection` 测试 + Task 15 手动验收。
5. **整合包清单漂移**：manifest 的 loader/project/file id 与本地 jar 不一致会导致启动失败。→ Task 10 `build_pack.py --check` 测试 + Task 14 启动冒烟。

---

## File Map

```
Inf/
├── docs/superpowers/specs/2026-11-16-inf-expedition-pack-design.md   # 已批准设计
├── docs/superpowers/plans/2026-11-16-inf-expedition-pack.md          # 本计划
├── expedition/                                                       # 新 mod，独立 Gradle 构建
│   ├── settings.gradle
│   ├── gradle.properties
│   ├── build.gradle
│   ├── gradlew / gradlew.bat / gradle/wrapper/...
│   ├── src/main/templates/META-INF/neoforge.mods.toml
│   ├── src/main/resources/
│   │   ├── pack.mcmeta
│   │   ├── assets/inf_expedition/lang/{zh_cn,en_us}.json
│   │   ├── assets/inf_expedition/models/{block,item}/*.json
│   │   ├── assets/inf_expedition/blockstates/*.json
│   │   ├── assets/inf_expedition/textures/{block,item,gui}/...
│   │   └── data/inf_expedition/
│   │       ├── dimension/{hub,run}.json
│   │       ├── dimension_type/{hub,run}.json
│   │       ├── advancement/*.json
│   │       ├── structure/hub_platform.nbt
│   │       └── expedition/{map.json,rooms/*.json,encounters/*.json}
│   └── src/main/java/com/inf/expedition/...
│       ├── Expedition.java / ExpeditionConfig.java
│       ├── map/{NodeType,MapNode,MapGraph,MapConfig,MapGenerator,MapSnapshot,NodeSnapshot}.java
│       ├── run/{RunStatus,RunState,MapSnapshotNbt,ExpeditionSavedData}.java
│       ├── data/{RoomType,RoomDefinition,RoomCatalog,EncounterDefinition,EncounterPlan,EncounterScaler,ExpeditionData,ExpeditionDataReloadListener}.java
│       ├── room/{RoomMarkers,RoomInstance,RoomPlacer,RoomProtection,RoomExitHandler,RoomManager,EncounterManager}.java
│       ├── event/{RunStartedEvent,RoomEnteredEvent,RoomCompletedEvent,RunEndedEvent}.java
│       ├── advancement/ExpeditionAdvancements.java
│       ├── command/ExpeditionCommand.java
│       ├── network/{ExpeditionNetwork,MapSyncPayload,MapMoveRequestPayload,MapMoveResponsePayload,MapOpenRequestPayload}.java
│       ├── client/{ExpeditionClient,MapClientData,MapKeyMapping,MapScreen}.java
│       ├── world/{HubManager,DimensionIds}.java
│       ├── block/{ExpeditionAltarBlock,QuestBoardBlock}.java
│       ├── item/RecallScrollItem.java
│       └── registry/{ModBlocks,ModItems,ModCreativeTabs}.java
├── pack/
│   ├── mods.yaml                        # 第三方模组唯一事实来源
│   ├── overrides/
│   │   ├── config/...
│   │   ├── kubejs/{server_scripts,client_scripts,startup_scripts,data}/...
│   │   ├── ftbquests/quests/...
│   │   └── fancymenu/...
│   ├── local-mods/                      # 构建产物复制落点（git 忽略）
│   └── export/                          # 生成的 zip/本地目录（git 忽略）
├── tools/
│   ├── build_pack.py
│   ├── gen_rooms.py
│   ├── gen_assets.py                    # 既有，扩展
│   └── test_*.py
└── docs/pack/{act1-balance.md,integration-log.md,release-checklist.md}
```

---

## Task 1: Expedition 工程骨架与可加载 mod

**Files:**
- Create: `expedition/settings.gradle`, `expedition/gradle.properties`, `expedition/build.gradle`
- Create: `expedition/gradlew`, `expedition/gradlew.bat`, `expedition/gradle/wrapper/gradle-wrapper.properties`, `expedition/gradle/wrapper/gradle-wrapper.jar`
- Create: `expedition/src/main/java/com/inf/expedition/Expedition.java`
- Create: `expedition/src/main/java/com/inf/expedition/ExpeditionConfig.java`
- Create: `expedition/src/main/templates/META-INF/neoforge.mods.toml`
- Create: `expedition/src/main/resources/pack.mcmeta`
- Create: `expedition/src/main/resources/assets/inf_expedition/lang/zh_cn.json`, `en_us.json`
- Create: `expedition/src/main/resources/data/inf_expedition/dimension/{hub,run}.json`
- Create: `expedition/src/main/resources/data/inf_expedition/dimension_type/{hub,run}.json`
- Modify: `.gitignore`

**Interfaces:**
- Consumes: 无
- Produces: `com.inf.expedition.Expedition.MODID = "inf_expedition"`；可运行的 `runServer`；dimensions `inf_expedition:hub` / `inf_expedition:run`；`ExpeditionConfig.roomCenterY()` / `clearInventoryOnRunEnd()` / `enableCoop()` / `maxMobsPerWave()` / `bossBar()` / `debug()`。

- [ ] **Step 1: 复制 wrapper 并写 Gradle 工程**

```bash
cd .
mkdir -p expedition
cp -r gradle gradlew gradlew.bat expedition/
```

`expedition/settings.gradle` 只保留 `pluginManagement { repositories { gradlePluginPortal() } }` 与 `org.gradle.toolchains.foojay-resolver-convention`。`expedition/gradle.properties` 使用 `mod_id=inf_expedition`、`mod_name=Inf Expedition`、`mod_version=0.1.0`、`mod_group_id=com.inf.expedition`、`minecraft_version=1.21.1`、`neo_version=21.1.266`、`parchment_minecraft_version=1.21.1`、`parchment_mappings_version=2024.11.17`。`build.gradle` 以根 `build.gradle` 为模板，改为 `net.neoforged.moddev` 2.0.148，`neoForge { mods { "${mod_id}" { sourceSet(sourceSets.main) } } }`，并加入 JUnit 5 依赖。

- [ ] **Step 2: 写主类与配置**

```java
package com.inf.expedition;

@Mod(Expedition.MODID)
public final class Expedition {
    public static final String MODID = "inf_expedition";

    public Expedition(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, ExpeditionConfig.SPEC);
    }
}
```

`ExpeditionConfig` 暴露：

```java
public static int roomCenterY();          // default 1, range -64..320
public static boolean clearInventoryOnRunEnd(); // default true
public static boolean enableCoop();       // default false
public static int maxMobsPerWave();       // default 16, range 1..64
public static boolean bossBar();          // default true
public static boolean debug();            // default false
```

- [ ] **Step 3: 写元数据与 pack.mcmeta**

`neoforge.mods.toml` 的 `modId="inf_expedition"`、`displayName="Inf Expedition"`、`license="All Rights Reserved"`、`version="${mod_version}"`；依赖 `neoforge` 与 `minecraft` 为 required，`inf` 为 optional（`type="optional"`, `versionRange="*"`）。`pack.mcmeta` 使用 `"pack_format": 34`。

- [ ] **Step 4: 写维度 JSON**

两个 dimension_type 除 `fixed_time` 外相同：`ambient_light: 0.2`、`bed_works: true`、`effects: minecraft:overworld`、`has_ceiling: false`、`has_raids: false`、`has_skylight: true`、`height: 256`、`logical_height: 256`、`min_y: 0`、`natural: true`、`piglin_safe: true`、`respawn_anchor_works: false`、`ultrawarm: false`、`infiniburn: "#minecraft:infiniburn_overworld"`、`fixed_time: 6000`。

两个 dimension 使用 flat generator：

```json
{
  "type": "inf_expedition:hub",
  "generator": {
    "type": "minecraft:flat",
    "settings": {
      "biome": "minecraft:the_void",
      "lakes": false,
      "features": false,
      "layers": [
        {"block": "minecraft:bedrock", "height": 1},
        {"block": "minecraft:air", "height": 255}
      ],
      "structure_overrides": []
    }
  }
}
```

- [ ] **Step 5: 构建与服务端冒烟**

```bash
cd .
JAVA_HOME=$JAVA_HOME ./gradlew build
test -f build/libs/inf_expedition-0.1.0.jar
timeout 180 env JAVA_HOME=$JAVA_HOME ./gradlew runServer --console=plain > /tmp/expedition-server.log 2>&1
grep -q 'Done' /tmp/expedition-server.log
! grep -E 'Missing or unsupported|Failed to parse dimension|Unknown dimension' /tmp/expedition-server.log
```

Expected: `BUILD SUCCESSFUL`；服务器日志出现 `Done`；无维度/依赖错误。

- [ ] **Step 6: 提交**

```bash
cd .
git add .gitignore expedition docs/superpowers/specs/2026-11-16-inf-expedition-pack-design.md docs/superpowers/plans/2026-11-16-inf-expedition-pack.md
git commit -m "chore: bootstrap inf_expedition mod and pack design"
```

---

## Task 2: 纯 Java 地图模型与生成器

**Files:**
- Create: `expedition/src/main/java/com/inf/expedition/map/NodeType.java`
- Create: `expedition/src/main/java/com/inf/expedition/map/MapNode.java`
- Create: `expedition/src/main/java/com/inf/expedition/map/MapGraph.java`
- Create: `expedition/src/main/java/com/inf/expedition/map/MapConfig.java`
- Create: `expedition/src/main/java/com/inf/expedition/map/MapSnapshot.java`
- Create: `expedition/src/main/java/com/inf/expedition/map/NodeSnapshot.java`
- Create: `expedition/src/main/java/com/inf/expedition/map/MapGenerator.java`
- Test: `expedition/src/test/java/com/inf/expedition/map/MapGeneratorTest.java`

**Interfaces:**
- Consumes: 无
- Produces:
  - `NodeType`：`BATTLE/ELITE/BOSS/SHOP/REST/EVENT/FORGE/START`；`String id()`；`boolean isCombat()`；`static NodeType byId(String)`。
  - `MapNode`：`int id()`, `int floor()`, `int column()`, `NodeType type()`, `void setType(NodeType)`, `Set<Integer> connections()`, `void addConnection(int)`, `boolean visited()`, `void markVisited()`, `boolean completed()`, `void markCompleted()`。
  - `MapGraph`：`List<MapNode> nodeList()`, `MapNode node(int)`, `List<MapNode> floor(int)`, `int startId()`, `int bossId()`, `boolean canMove(int fromId, int toId)`, `MapSnapshot snapshot()`, `static MapGraph fromSnapshot(MapSnapshot)`。
  - `MapConfig`：`record MapConfig(int acts, int floors, int columns, int paths, int bossFloor, int eliteMinFloor, int shopMinFloor, int forgeMinFloor, Map<NodeType,Integer> weights, boolean guaranteeRestBeforeBoss)`；`MapConfig.DEFAULT = new MapConfig(1, 8, 3, 2, 8, 3, 2, 2, Map.of(BATTLE,40, EVENT,20, FORGE,14, REST,12, SHOP,8, ELITE,6), true)`；`int weight(NodeType)`。
  - `MapSnapshot`：`record MapSnapshot(int startId, int bossId, List<NodeSnapshot> nodes)`。
  - `NodeSnapshot`：`record NodeSnapshot(int id, int floor, int column, String type, Set<Integer> connections, boolean visited, boolean completed)`。
  - `MapGenerator`：`MapGenerator(long seed)`, `MapGraph generate(MapConfig config)`。

- [ ] **Step 1: 写失败测试**

```java
class MapGeneratorTest {
    private static final MapConfig C = MapConfig.DEFAULT;

    @Test void sameSeedProducesSameGraph() {
        assertEquals(new MapGenerator(42L).generate(C).snapshot(),
                     new MapGenerator(42L).generate(C).snapshot());
    }

    @Test void startAndBossAreUnique() {
        MapGraph g = new MapGenerator(7L).generate(C);
        assertEquals(1, g.floor(0).size());
        assertEquals(1, g.floor(C.bossFloor()).size());
        assertEquals(NodeType.START, g.node(g.startId()).type());
        assertEquals(NodeType.BOSS, g.node(g.bossId()).type());
    }

    @Test void connectionsAlwaysAdvanceOneFloor() {
        MapGraph g = new MapGenerator(11L).generate(C);
        for (MapNode n : g.nodeList()) {
            for (int target : n.connections()) {
                assertEquals(n.floor() + 1, g.node(target).floor());
            }
        }
    }

    @Test void everyContentNodeHasIncomingAndOutgoing() {
        MapGraph g = new MapGenerator(13L).generate(C);
        Set<Integer> incoming = new HashSet<>();
        for (MapNode n : g.nodeList()) incoming.addAll(n.connections());
        for (MapNode n : g.nodeList()) {
            if (n.type() != NodeType.START) assertTrue(incoming.contains(n.id()), "no incoming " + n.id());
            if (n.type() != NodeType.BOSS) assertFalse(n.connections().isEmpty(), "no outgoing " + n.id());
        }
    }

    @Test void minFloorRulesHold() {
        MapGraph g = new MapGenerator(17L).generate(C);
        for (MapNode n : g.nodeList()) {
            if (n.type() == NodeType.ELITE) assertTrue(n.floor() >= C.eliteMinFloor());
            if (n.type() == NodeType.SHOP) assertTrue(n.floor() >= C.shopMinFloor());
            if (n.type() == NodeType.FORGE) assertTrue(n.floor() >= C.forgeMinFloor());
        }
    }

    @Test void restIsGuaranteedBeforeBoss() {
        MapGraph g = new MapGenerator(19L).generate(C);
        assertTrue(g.floor(C.bossFloor() - 1).stream().anyMatch(n -> n.type() == NodeType.REST));
    }

    @Test void snapshotRoundTrip() {
        MapGraph g = new MapGenerator(23L).generate(C);
        assertEquals(g.snapshot(), MapGraph.fromSnapshot(g.snapshot()).snapshot());
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `cd expedition && JAVA_HOME=$JAVA_HOME ./gradlew test --tests com.inf.expedition.map.MapGeneratorTest`
Expected: 编译失败/断言失败，因为类尚不存在。

- [ ] **Step 3: 实现模型与生成器**

生成算法必须按以下顺序；实现者只写这一种算法：

1. 创建 start 节点：floor `0`，column `columns / 2`，type `START`。
2. 创建 boss 节点：floor `bossFloor`，column `columns / 2`，type `BOSS`。
3. 对每条路径 `p in [0, paths)`：起始列 `startCol = round(p * (columns - 1) / max(1, paths - 1))`；对 `floor in [1, bossFloor)` 逐层：在 `(floor, col)` 取或创建节点；把上一节点连接到本节点；下一层 `col = clamp(col + random.nextInt(3) - 1, 0, columns - 1)`。
4. 把 start 连接到 floor 1 的所有节点；把 floor `bossFloor-1` 的所有节点连接到 boss。
5. 对每个 content 节点，若 `connections()` 为空，连接到下一层最近的列节点。
6. 按 `floor` 升序、`column` 升序分配节点 id（0..n-1），并重写 connections 为 id 集合。
7. 填类型：start/boss 固定；其余节点按权重加权随机，过滤 `min_floor`；同一层已有 `FORGE/SHOP/REST` 时，重抽一次，仍重复则退化为 `BATTLE`。
8. 若 `guaranteeRestBeforeBoss` 且 floor `bossFloor-1` 没有 REST：优先把一个 `BATTLE` 或 `EVENT` 节点改为 REST；没有则改第一个非 ELITE 节点。
9. `MapGraph.canMove(from,to)` = `from` 存在、`to` 存在、`from.connections().contains(to)`、`to.floor() == from.floor() + 1`。

`MapSnapshot` 的 nodes 按 id 升序；`fromSnapshot` 重建节点与连接。所有集合用 `LinkedHashSet` 保持确定性。

- [ ] **Step 4: 跑测试确认通过**

Run: `cd expedition && JAVA_HOME=$JAVA_HOME ./gradlew test --tests com.inf.expedition.map.MapGeneratorTest`
Expected: PASS（7 tests）。

- [ ] **Step 5: 提交**

```bash
git add expedition/src/main/java/com/inf/expedition/map expedition/src/test/java/com/inf/expedition/map
git commit -m "feat: add deterministic node map generator"
```

---

## Task 3: 运行状态、快照 NBT 与 SavedData

**Files:**
- Create: `expedition/src/main/java/com/inf/expedition/run/RunStatus.java`
- Create: `expedition/src/main/java/com/inf/expedition/run/RunState.java`
- Create: `expedition/src/main/java/com/inf/expedition/run/MapSnapshotNbt.java`
- Create: `expedition/src/main/java/com/inf/expedition/run/ExpeditionSavedData.java`
- Test: `expedition/src/test/java/com/inf/expedition/run/RunStateTest.java`
- Test: `expedition/src/test/java/com/inf/expedition/run/MapSnapshotNbtTest.java`

**Interfaces:**
- Consumes: Task 2 的 `MapGraph`、`MapSnapshot`、`NodeSnapshot`。
- Produces:
  - `enum RunStatus { IDLE, RUNNING, COMPLETE, FAILED }`
  - `RunState`：`static RunState idle()`, `void begin(long seed, MapGraph graph, Collection<UUID> party)`, `RunStatus status()`, `long seed()`, `int currentNodeId()`, `Set<UUID> party()`, `boolean isPartyMember(UUID)`, `void removeMember(UUID)`, `boolean moveTo(MapGraph, int)`, `void completeCurrent(MapGraph)`, `void finish(boolean success)`, `boolean currentCompleted(MapGraph)`。
  - `MapSnapshotNbt`：`static CompoundTag write(MapSnapshot snapshot)`, `static MapSnapshot read(CompoundTag tag)`。
  - `ExpeditionSavedData extends SavedData`：`static ExpeditionSavedData get(MinecraftServer server)`, `MapGraph map()`, `void setMap(MapGraph)`, `RunState run()`, `void setRun(RunState)`, `boolean hubPlaced()`, `void setHubPlaced(boolean)`, `void resetRun()`, `CompoundTag save(CompoundTag, HolderLookup.Provider)`, `static ExpeditionSavedData load(CompoundTag, HolderLookup.Provider)`。

- [ ] **Step 1: 写 RunState 失败测试**

```java
class RunStateTest {
    private MapGraph graph;
    private int start, next, far;

    @BeforeEach void setup() {
        graph = new MapGenerator(42L).generate(MapConfig.DEFAULT);
        start = graph.startId();
        next = graph.node(start).connections().iterator().next();
        far = graph.floor(3).get(0).id();
    }

    @Test void beginMarksStartVisited() {
        RunState s = RunState.idle();
        s.begin(1L, graph, List.of(UUID.randomUUID()));
        assertEquals(RunStatus.RUNNING, s.status());
        assertEquals(start, s.currentNodeId());
        assertTrue(graph.node(start).visited());
    }

    @Test void movingTwiceIsRejected() {
        RunState s = RunState.idle();
        s.begin(1L, graph, List.of(UUID.randomUUID()));
        assertFalse(s.moveTo(graph, next));          // 房间未完成
        s.completeCurrent(graph);
        assertTrue(s.moveTo(graph, next));
        assertFalse(s.moveTo(graph, next));          // 已访问
    }

    @Test void nonAdjacentMoveIsRejected() {
        RunState s = RunState.idle();
        s.begin(1L, graph, List.of(UUID.randomUUID()));
        s.completeCurrent(graph);
        assertFalse(s.moveTo(graph, far));
    }

    @Test void completeMarksCurrentNode() {
        RunState s = RunState.idle();
        s.begin(1L, graph, List.of(UUID.randomUUID()));
        s.completeCurrent(graph);
        assertTrue(graph.node(start).completed());
        assertTrue(s.currentCompleted(graph));
    }

    @Test void finishSetsStatus() {
        RunState s = RunState.idle();
        s.begin(1L, graph, List.of(UUID.randomUUID()));
        s.finish(true);
        assertEquals(RunStatus.COMPLETE, s.status());
        s.finish(false);
        assertEquals(RunStatus.FAILED, s.status());
    }
}
```

- [ ] **Step 2: 写 NBT 往返失败测试**

```java
class MapSnapshotNbtTest {
    @Test void roundTripPreservesSnapshot() {
        MapSnapshot snap = new MapGenerator(99L).generate(MapConfig.DEFAULT).snapshot();
        CompoundTag tag = MapSnapshotNbt.write(snap);
        assertEquals(snap, MapSnapshotNbt.read(tag));
    }
}
```

- [ ] **Step 3: 跑测试确认失败**

Run: `cd expedition && JAVA_HOME=$JAVA_HOME ./gradlew test --tests 'com.inf.expedition.run.*'`
Expected: 编译失败。

- [ ] **Step 4: 实现**

`RunState` 规则：

- `begin` 只在 `IDLE` 时有效；设置 seed、party 副本、current=start、status=RUNNING、`graph.node(start).markVisited()`。
- `moveTo` 必须 `RUNNING`、`currentCompleted(graph)`、`graph.canMove(current,target)`、`!graph.node(target).visited()`；成功后 `current=target`、`markVisited`，返回 true。
- `completeCurrent` 标记当前节点 completed。
- `finish(success)` 设 `COMPLETE`/`FAILED`；不改变地图。
- `removeMember` 移除 UUID；不自动结束 run。

`MapSnapshotNbt` 写 `CompoundTag`：`start` int、`boss` int、`nodes` ListTag；每个 node 写 `id/floor/column/type`、`connections` IntArrayTag、`visited/completed` boolean。读回时校验字段，未知 type 用 `NodeType.BATTLE`。

`ExpeditionSavedData` 用 `SavedData.Factory<>(ExpeditionSavedData::new, ExpeditionSavedData::load)` 挂到主世界 `server.overworld().getDataStorage().computeIfAbsent(factory, "inf_expedition")`；保存 `map`、`run.seed/status/current/party`、`hubPlaced`；加载时若 `status == RUNNING`，直接转 `FAILED`（设计 §7.4：重启视为放弃）。

- [ ] **Step 5: 跑测试确认通过**

Run: `cd expedition && JAVA_HOME=$JAVA_HOME ./gradlew test --tests 'com.inf.expedition.run.*'`
Expected: PASS（6 tests）。

- [ ] **Step 6: 提交**

```bash
git add expedition/src/main/java/com/inf/expedition/run expedition/src/test/java/com/inf/expedition/run
git commit -m "feat: add run state and map persistence"
```

---

## Task 4: 数据驱动的房间、遭遇与地图配置

**Files:**
- Create: `expedition/src/main/java/com/inf/expedition/data/RoomType.java`
- Create: `expedition/src/main/java/com/inf/expedition/data/RoomDefinition.java`
- Create: `expedition/src/main/java/com/inf/expedition/data/EncounterDefinition.java`
- Create: `expedition/src/main/java/com/inf/expedition/data/EncounterPlan.java`
- Create: `expedition/src/main/java/com/inf/expedition/data/EncounterScaler.java`
- Create: `expedition/src/main/java/com/inf/expedition/data/RoomCatalog.java`
- Create: `expedition/src/main/java/com/inf/expedition/data/ExpeditionData.java`
- Create: `expedition/src/main/java/com/inf/expedition/data/ExpeditionDataReloadListener.java`
- Test: `expedition/src/test/java/com/inf/expedition/data/RoomCatalogTest.java`
- Test: `expedition/src/test/java/com/inf/expedition/data/EncounterScalerTest.java`

**Interfaces:**
- Consumes: Task 2 `NodeType`；Task 1 配置。
- Produces:
  - `enum RoomType { START, BATTLE, ELITE, BOSS, FORGE, REST, SHOP, EVENT }`；`String id()`、`static RoomType byId(String)`、`NodeType nodeType()`、`static RoomType forNode(NodeType)`。
  - `record RoomDefinition(String id, RoomType type, int weight, ResourceLocation structure, ResourceLocation encounter, int minAct, int maxAct, String titleKey, ResourceLocation rewardLoot)`（`encounter`/`rewardLoot` 可为 null）。
  - `record EncounterDefinition(ResourceLocation id, String titleKey, boolean boss, List<Wave> waves, Scale scale, ResourceLocation rewardLoot)`，内部 `record Wave(List<MobSpawn> mobs)`、`record MobSpawn(ResourceLocation entity, int count)`、`record Scale(double healthPerAct, double damagePerAct, double countPerAct, double healthPerExtraPlayer, double damagePerExtraPlayer)`。
  - `EncounterPlan`：`static List<WavePlan> build(EncounterDefinition def, int act, int partySize, int maxMobs)`；`record WavePlan(List<MobPlan> mobs)`、`record MobPlan(ResourceLocation entity, int count)`。
  - `EncounterScaler`：`static double healthMultiplier(int act, int partySize, EncounterDefinition.Scale)`, `static double damageMultiplier(...)`, `static int scaledCount(int base, int act, double countPerAct, int maxMobs)`。
  - `RoomCatalog`：`static RoomCatalog fromJson(Map<String,String> rooms, Map<String,String> encounters)`, `Optional<RoomDefinition> pick(RoomType type, int act, Random random)`, `Optional<EncounterDefinition> encounter(ResourceLocation id)`, `Collection<RoomDefinition> rooms()`。
  - `ExpeditionData`：`static MapConfig mapConfig()`, `static RoomCatalog catalog()`, `static void set(MapConfig, RoomCatalog)`。
  - `ExpeditionDataReloadListener`：注册到 `AddReloadListenerEvent`，读取 `expedition/map.json`、`expedition/rooms/*.json`、`expedition/encounters/*.json`。

- [ ] **Step 1: 写解析/权重失败测试**

```java
class RoomCatalogTest {
    @Test void parsesRoomAndEncounter() {
        var cat = RoomCatalog.fromJson(
            Map.of("crypt", """
              {"type":"battle","weight":10,"structure":"inf_expedition:rooms/battle/crypt",
               "encounter":"inf_expedition:battle/crypt","min_act":1,"max_act":1}
              """),
            Map.of("crypt", """
              {"title":"encounter.inf_expedition.crypt","boss":false,
               "waves":[{"mobs":[{"id":"minecraft:zombie","count":4}]}],
               "scale":{"health_per_act":0.25,"damage_per_act":0.15,"count_per_act":0.2,
                        "health_per_extra_player":0.5,"damage_per_extra_player":0.25},
               "reward_loot":"inf_expedition:chests/battle_common"}
              """));
        assertEquals(1, cat.rooms().size());
        assertTrue(cat.encounter(ResourceLocation.parse("inf_expedition:battle/crypt")).isPresent());
    }

    @Test void pickFiltersByTypeAndAct() {
        var cat = RoomCatalog.fromJson(
            Map.of("a", """
              {"type":"battle","weight":1,"structure":"inf_expedition:rooms/battle/a","min_act":2,"max_act":3}
              """),
            Map.of());
        assertTrue(cat.pick(RoomType.BATTLE, 1, new Random(1)).isEmpty());
        assertTrue(cat.pick(RoomType.BATTLE, 2, new Random(1)).isPresent());
    }

    @Test void invalidTypeIsSkipped() {
        var cat = RoomCatalog.fromJson(Map.of("bad", "{\"type\":\"nope\",\"weight\":1}"), Map.of());
        assertEquals(0, cat.rooms().size());
    }
}
```

- [ ] **Step 2: 写缩放失败测试**

```java
class EncounterScalerTest {
    private static final EncounterDefinition.Scale S =
        new EncounterDefinition.Scale(0.25, 0.15, 0.2, 0.5, 0.25);

    @Test void actAndPartyScale() {
        assertEquals(1.0, EncounterScaler.healthMultiplier(1, 1, S), 1e-9);
        assertEquals(1.25, EncounterScaler.healthMultiplier(2, 1, S), 1e-9);
        assertEquals(1.875, EncounterScaler.healthMultiplier(2, 2, S), 1e-9);
        assertEquals(1.25, EncounterScaler.damageMultiplier(2, 2, S), 1e-9);
    }

    @Test void countScalesAndCaps() {
        assertEquals(1, EncounterScaler.scaledCount(1, 1, 0.2, 16));
        assertEquals(2, EncounterScaler.scaledCount(1, 3, 0.2, 16));
        assertEquals(16, EncounterScaler.scaledCount(100, 1, 0.2, 16));
    }
}
```

- [ ] **Step 3: 跑测试确认失败**

Run: `cd expedition && JAVA_HOME=$JAVA_HOME ./gradlew test --tests 'com.inf.expedition.data.*'`
Expected: 编译失败。

- [ ] **Step 4: 实现**

- JSON 用 Gson `JsonParser` 读取；字段用 snake_case；`ResourceLocation.parse` 失败则跳过并在 `LOGGER.warn` 输出文件 id。
- `RoomCatalog.pick`：收集 `type` 匹配且 `minAct <= act <= maxAct` 的条目；按 `weight` 加权；空则 `Optional.empty()`。
- `EncounterScaler.healthMultiplier = (1 + healthPerAct*(act-1)) * (1 + healthPerExtraPlayer*(partySize-1))`；damage 同理；`scaledCount = min(maxMobs, max(1, round(base * (1 + countPerAct*(act-1)))))`。
- `EncounterPlan.build` 对每波每个 `MobSpawn` 调 `scaledCount`；结果为 `WavePlan` 列表。
- `ExpeditionDataReloadListener` 用 `SimpleJsonResourceReloadListener`（Gson, `"expedition/rooms"` 与 `"expedition/encounters"` 各一个实例；`map.json` 用第二个监听器读取 `expedition` 目录）。注册在 `AddReloadListenerEvent`；完成后 `ExpeditionData.set(...)`。
- `MapConfig` 解析：缺字段用 `MapConfig.DEFAULT` 的值；`weights` 缺省用默认；`acts` 只允许 1（v0.1），大于 1 时日志警告并夹到 1。

- [ ] **Step 5: 跑测试确认通过**

Run: `cd expedition && JAVA_HOME=$JAVA_HOME ./gradlew test --tests 'com.inf.expedition.data.*'`
Expected: PASS（5 tests）。

- [ ] **Step 6: 提交**

```bash
git add expedition/src/main/java/com/inf/expedition/data expedition/src/test/java/com/inf/expedition/data
git commit -m "feat: add data-driven room and encounter catalog"
```

---

## Task 5: 房间放置、标记、保护与出口

**Files:**
- Create: `expedition/src/main/java/com/inf/expedition/room/RoomMarkers.java`
- Create: `expedition/src/main/java/com/inf/expedition/room/RoomInstance.java`
- Create: `expedition/src/main/java/com/inf/expedition/room/RoomPlacer.java`
- Create: `expedition/src/main/java/com/inf/expedition/room/RoomProtection.java`
- Create: `expedition/src/main/java/com/inf/expedition/room/RoomExitHandler.java`
- Create: `expedition/src/main/java/com/inf/expedition/room/RoomManager.java`
- Test: `expedition/src/test/java/com/inf/expedition/room/RoomInstanceTest.java`
- Test: `expedition/src/test/java/com/inf/expedition/room/ProtectionPolicyTest.java`

**Interfaces:**
- Consumes: Task 2/3/4 的 `MapGraph`、`RunState`、`RoomDefinition`、`RoomCatalog`。
- Produces:
  - `record RoomMarkers(List<BlockPos> spawns, List<BlockPos> mobs, List<BlockPos> bosses, List<BlockPos> rewards, List<BlockPos> exits, List<BlockPos> shops, List<BlockPos> rests)`；`static RoomMarkers EMPTY`；`List<BlockPos> byName(String)`。
  - `RoomInstance`：`UUID id()`, `int nodeId()`, `RoomType type()`, `String definitionId()`, `ResourceLocation structure()`, `AABB bounds()`, `RoomMarkers markers()`, `boolean completed()`, `void markCompleted()`, `boolean contains(BlockPos)`, `Vec3 center()`。
  - `RoomPlacer`：`static Optional<RoomInstance> place(ServerLevel level, RoomDefinition def, int nodeId, BlockPos center)`；失败时日志并返回 empty。
  - `RoomProtection`：静态事件处理方法 `onBreak`, `onPlace`, `onExplosion`, `onMobGriefing`, `onDestroyBlock`；`static boolean isAllowedBlockId(ResourceLocation id)`；白名单只含 `inf:forge_table`。
  - `RoomManager`：`static RoomInstance current()`, `static void enter(MinecraftServer, RunState, MapGraph, int nodeId)`, `static void complete(MinecraftServer, boolean success)`, `static void clear(MinecraftServer)`, `static void exitRoom(ServerPlayer)`, `static void tick(MinecraftServer)`。

- [ ] **Step 1: 写 RoomInstance/Protection 失败测试**

```java
class RoomInstanceTest {
    @Test void containsAndCenter() {
        RoomInstance r = new RoomInstance(UUID.randomUUID(), 1, RoomType.BATTLE, "x",
            ResourceLocation.parse("inf_expedition:rooms/battle/x"),
            new AABB(0, 0, 0, 10, 5, 10), RoomMarkers.EMPTY);
        assertTrue(r.contains(new BlockPos(5, 2, 5)));
        assertFalse(r.contains(new BlockPos(11, 2, 5)));
        assertEquals(5.0, r.center().x, 1e-9);
        assertEquals(2.5, r.center().y, 1e-9);
    }
}

class ProtectionPolicyTest {
    @Test void onlyForgeTableIsWhitelisted() {
        assertTrue(RoomProtection.isAllowedBlockId(ResourceLocation.parse("inf:forge_table")));
        assertFalse(RoomProtection.isAllowedBlockId(ResourceLocation.parse("minecraft:stone")));
        assertFalse(RoomProtection.isAllowedBlockId(ResourceLocation.parse("minecraft:chest")));
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `cd expedition && JAVA_HOME=$JAVA_HOME ./gradlew test --tests 'com.inf.expedition.room.*'`
Expected: 编译失败。

- [ ] **Step 3: 实现放置器**

```java
public static Optional<RoomInstance> place(ServerLevel level, RoomDefinition def, int nodeId, BlockPos center) {
    // 1. structureManager.get(def.structure()); empty -> warn + empty
    // 2. size = template.getSize(); origin = center.offset(-size.getX()/2, 0, -size.getZ()/2)
    // 3. settings = new StructurePlaceSettings().setMirror(Mirror.NONE).setRotation(Rotation.NONE)
    // 4. template.placeInWorld(level, origin, origin, settings, level.random, 2)
    // 5. bounds = AABB from origin to origin.offset(size) 外扩 1
    // 6. scanMarkers(level, bounds) -> 扫描 Blocks.JIGSAW，读 JigsawBlockEntity#getName()
    //    spawn/mob/boss/reward/exit/shop/rest；替换为空气；exit 放 lodestone
    // 7. 返回 RoomInstance（id=UUID.randomUUID()）
}
```

`RoomManager.enter`：

1. 从 `ExpeditionData.catalog().pick(RoomType.forNode(nodeType), act, random)` 取房间；空则中止本次移动并保留旧房间。
2. **进入前预检** `level.getStructureManager().get(def.structure())`；不存在则中止移动并保留旧房间，给玩家消息。
3. 若 `current()` 非空，`clear(server)`：按 bounds 把方块设为空气（跳过 bedrock），移除带 `inf_expedition:room` 标签的实体。
4. 调 `RoomPlacer.place`；失败则 `RunManager.end(server,false)` 并把队伍传回大厅（不能把玩家留在已清空的旧房间/虚空），给玩家消息。
5. 设置 `current`；全队传送到 `markers.spawns()`（轮流；没有则 center）；给 1 秒抗性/缓降。
6. `start` 房间进入即 `markCompleted`；其他房间等待遭遇清空或 exit 右键。
7. 发送 `RoomEnteredEvent`。

`RoomManager.exitRoom(player)`：若当前房间未完成且类型非战斗 -> `markCompleted`、`run.completeCurrent(map)`、同步地图；若战斗房间未清空 -> 提示「先清空房间」；已完成 -> 仅提示可开地图。

`RoomProtection`：

- `BlockEvent.BreakEvent`：若 `current()!=null && current.contains(pos)` 且非创造 -> cancel。
- `BlockEvent.EntityPlaceEvent`：同上；白名单 `inf:forge_table` 允许（用 `isAllowedBlockId` 判断注册表 id）。
- `ExplosionEvent.Start`：若爆炸中心在房间内 -> cancel。
- `EntityMobGriefingEvent`：DENY。
- `LivingDestroyBlockEvent`：若位置在房间内 -> cancel。
- `PlayerInteractEvent.RightClickBlock`：若目标是 `Blocks.LODESTONE` 且在 `markers.exits()` 中 -> `RoomManager.exitRoom(player)`，cancel。

- [ ] **Step 4: 跑测试确认通过**

Run: `cd expedition && JAVA_HOME=$JAVA_HOME ./gradlew test --tests 'com.inf.expedition.room.*'`
Expected: PASS（2 tests）。

- [ ] **Step 5: 手动放置验证**

加临时命令 `/infexpedition debugplace inf_expedition:rooms/battle/crypt`（Task 6 正式命令）；在 `run` 维度原点放置后，用 `/setblock` 检查 jigsaw 标记已被替换、出口有 lodestone、AABB 内破坏被拒绝。记录到 `docs/pack/integration-log.md`。

- [ ] **Step 6: 提交**

```bash
git add expedition/src/main/java/com/inf/expedition/room expedition/src/test/java/com/inf/expedition/room
git commit -m "feat: add room placement, markers and protection"
```

---

## Task 6: 运行管理器、命令、大厅与返回物品

**Files:**
- Create: `expedition/src/main/java/com/inf/expedition/event/{RunStartedEvent,RoomEnteredEvent,RoomCompletedEvent,RunEndedEvent}.java`
- Create: `expedition/src/main/java/com/inf/expedition/command/ExpeditionCommand.java`
- Create: `expedition/src/main/java/com/inf/expedition/world/{DimensionIds,HubManager}.java`
- Create: `expedition/src/main/java/com/inf/expedition/block/{ExpeditionAltarBlock,QuestBoardBlock}.java`
- Create: `expedition/src/main/java/com/inf/expedition/item/RecallScrollItem.java`
- Create: `expedition/src/main/java/com/inf/expedition/registry/{ModBlocks,ModItems,ModCreativeTabs}.java`
- Create: `expedition/src/main/resources/assets/inf_expedition/blockstates/{expedition_altar,quest_board}.json`
- Create: `expedition/src/main/resources/assets/inf_expedition/models/block/{expedition_altar,quest_board}.json`
- Create: `expedition/src/main/resources/assets/inf_expedition/models/item/{expedition_altar,quest_board,recall_scroll}.json`
- Test: `expedition/src/test/java/com/inf/expedition/run/RunManagerRulesTest.java`

**Interfaces:**
- Consumes: Task 2–5。
- Produces:
  - 事件类均 `extends net.neoforged.bus.api.Event`，提供 JavaBean getter，供 KubeJS 调用：
    - `RunStartedEvent`：`getServer()`, `getPlayers()`
    - `RoomEnteredEvent`：`getPlayer()`, `getRoom()`
    - `RoomCompletedEvent`：`getServer()`, `getRoom()`, `isSuccess()`
    - `RunEndedEvent`：`getServer()`, `isSuccess()`, `getParty()`
    全部 post 到 `NeoForge.EVENT_BUS`。
  - `RunManager`：`static void start(MinecraftServer, List<ServerPlayer>)`, `static void move(ServerPlayer, int nodeId)`, `static void complete(MinecraftServer, boolean)`, `static void leave(ServerPlayer)`, `static void end(MinecraftServer, boolean)`, `static void tick(MinecraftServer)`。
  - `HubManager`：`static void ensurePlatform(MinecraftServer)`, `static void teleportToHub(ServerPlayer)`, `static BlockPos hubSpawn()`。
  - 注册项：`inf_expedition:expedition_altar`、`inf_expedition:quest_board`、`inf_expedition:recall_scroll`。
  - 命令：`/infexpedition start|reset|map|move <id>|complete|info|debugplace <structure>`。

- [ ] **Step 1: 写纯规则测试**

```java
class RunManagerRulesTest {
    @Test void partyIsCopiedAndLastMemberLeaves() {
        RunState s = RunState.idle();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        s.begin(1L, new MapGenerator(1L).generate(MapConfig.DEFAULT), List.of(a, b));
        s.removeMember(a);
        assertTrue(s.isPartyMember(b));
        s.removeMember(b);
        assertFalse(s.isPartyMember(b));
        assertTrue(s.party().isEmpty());
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `cd expedition && JAVA_HOME=$JAVA_HOME ./gradlew test --tests com.inf.expedition.run.RunManagerRulesTest`
Expected: 编译失败（若无新方法则调整 `RunState`）。

- [ ] **Step 3: 实现 RunManager.start**

1. 若 `ExpeditionSavedData.get(server).run().status() != IDLE` -> 给发起者消息，返回。
2. `party = players.stream().filter(p -> !p.isSpectator()).toList()`；空 -> 消息，返回。
3. `seed = server.overworld().getRandom().nextLong()`；`map = new MapGenerator(seed).generate(ExpeditionData.mapConfig())`；`run.begin(seed, map, party ids)`。
4. 对每个玩家：`player.getInventory().clearContent()`；给基础起始包：
   - `inf:hammer` ×1（若注册表存在）
   - 随机一种 `inf:*_blank` ×2
   - `minecraft:cooked_beef` ×8
   - `inf_expedition:recall_scroll` ×1
   - `ftbquests:book` ×1（若注册表存在）
5. 保存 map/run；`RoomManager.enter(server, run, map, map.startId())`；post `RunStartedEvent`。
6. 给所有玩家 `player.setHealth(player.getMaxHealth())`。

**起始包物品通过 `BuiltInRegistries.ITEM.get(ResourceLocation)` 查；不存在则跳过并日志警告。** 不直接引用 Inf 类。

- [ ] **Step 4: 实现 move/complete/end/leave**

- `move(player,nodeId)`：校验玩家在 party、run RUNNING、当前房间 completed、`map.canMove(current,nodeId)`、目标未访问；失败给消息；成功 `run.moveTo`，`RoomManager.enter` 目标节点，post `RoomEnteredEvent`，同步地图。
- `complete(server,success)`：`run.completeCurrent(map)`；`RoomManager.complete`；若当前节点是 BOSS -> `end(server,true)`；否则同步地图。
- `end(server,success)`：`run.finish(success)`；post `RunEndedEvent`；`server.tell(new TickTask(server.getTickCount()+60, () -> { clear inventories; RoomManager.clear; teleport party hub; saved.resetRun(); }))`。
- `leave(player)`：从 party 移除；传回 hub；若 party 空 -> `end(server,false)`；否则同步地图。
- `tick(server)`：每 20 tick 检查 party 在线成员；全离线 -> end(false)；清理孤儿 boss bar（Task 7）。
- 死亡：`LivingDeathEvent`：若实体是 party 玩家且 run RUNNING：
  - 还有其他在线 party 生存玩家 -> `event.setCanceled(true)`、`setHealth(1)`、`setGameMode(SPECTATOR)`、消息。
  - 否则 `end(server,false)`。
  - 进入下一房间时，把队伍中的旁观者恢复生存并传送到 spawn。

- [ ] **Step 5: 实现大厅与交互方块**

- `HubManager.ensurePlatform`：若 `saved.hubPlaced()` 为 false，尝试加载 `inf_expedition:hub_platform` 结构放在 `(0, 64, 0)`；失败则用代码在 `(0, 64, 0)` 搭 9×9 石砖平台，中心放 `expedition_altar`，旁边放 `quest_board`；设置 `hubPlaced=true`。
- `teleportToHub`：`player.teleportTo(server.getLevel(DimensionIds.HUB), 0.5, 66, 0.5, 0, 0)`；`hubSpawn()` 返回 `new BlockPos(0, 66, 0)`。
- 登录/重生：若 run RUNNING 且玩家在 party -> 当前房间 spawn；否则 hub。首次登录调用 `ensurePlatform`。
- `ExpeditionAltarBlock.useWithoutItem`：服务端 -> `RunManager.start(server, server.getPlayerList().getPlayers())`；客户端返回 SUCCESS。
- `QuestBoardBlock.useWithoutItem`：服务端 -> 若玩家没有 `ftbquests:book` 则给一本；否则提示已拥有。
- `RecallScrollItem.use`：在 `run` 维度且玩家在 party -> `RunManager.leave(player)`，物品消耗；否则提示。
- 注册 block/item/creative tab（`inf_expedition` tab），写 blockstate/model/lang。

- [ ] **Step 6: 实现命令**

`ExpeditionCommand.register`：`/infexpedition` 需要权限 2（`start/reset/debugplace`），`map/move/complete/info` 允许队伍成员。`info` 输出 `seed/status/act/currentNode/room/partySize`。`debugplace <structure>` 在 run 维度 `(0,1,0)` 调 `RoomPlacer.place`。

- [ ] **Step 7: 服务端冒烟**

```bash
cd .
timeout 180 env JAVA_HOME=$JAVA_HOME ./gradlew runServer --console=plain > /tmp/exp-server2.log 2>&1
grep -q 'Done' /tmp/exp-server2.log
! grep -E 'Exception|Failed to|Unknown dimension' /tmp/exp-server2.log
```

在服务器控制台执行 `infexpedition info`（通过 stdin 或 RCON 不必要；仅验证无异常）。Expected: 命令注册成功，`info` 输出 `IDLE`。

- [ ] **Step 8: 提交**

```bash
git add expedition/src/main/java/com/inf/expedition/{event,command,world,block,item,registry} expedition/src/main/resources/assets/inf_expedition expedition/src/test/java/com/inf/expedition/run
git commit -m "feat: add run lifecycle, commands and hub"
```

---

## Task 7: 遭遇战、缩放、Boss 血条与奖励

**Files:**
- Create: `expedition/src/main/java/com/inf/expedition/room/EncounterManager.java`
- Modify: `expedition/src/main/java/com/inf/expedition/room/RoomManager.java`
- Test: `expedition/src/test/java/com/inf/expedition/data/EncounterPlanTest.java`

**Interfaces:**
- Consumes: Task 4 `EncounterDefinition`/`EncounterPlan`/`EncounterScaler`；Task 5 `RoomInstance`/`RoomMarkers`；Task 6 `RunEndedEvent`。
- Produces:
  - `EncounterManager`：`static void start(ServerLevel, RoomInstance, EncounterDefinition, List<ServerPlayer>)`, `static void onMobDeath(LivingDeathEvent)`, `static void clear()`, `static void tick(MinecraftServer)`。
  - 常量：`TAG_ROOM_MOB = "inf_expedition:room_mob"`，persistent key `inf_expedition:room`。

- [ ] **Step 1: 写 EncounterPlan 测试**

```java
class EncounterPlanTest {
    @Test void buildsScaledWaves() {
        var def = new EncounterDefinition(ResourceLocation.parse("inf_expedition:e"),
            "e", false,
            List.of(new EncounterDefinition.Wave(List.of(
                new EncounterDefinition.MobSpawn(ResourceLocation.parse("minecraft:zombie"), 4)))),
            new EncounterDefinition.Scale(0.25, 0.15, 0.2, 0.5, 0.25),
            ResourceLocation.parse("inf_expedition:chests/x"));
        var plan = EncounterPlan.build(def, 2, 2, 16);
        assertEquals(1, plan.size());
        assertEquals(5, plan.get(0).mobs().get(0).count()); // round(4 * 1.2) = 5
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `cd expedition && JAVA_HOME=$JAVA_HOME ./gradlew test --tests com.inf.expedition.data.EncounterPlanTest`
Expected: 失败/编译失败。

- [ ] **Step 3: 实现 EncounterManager**

- `start`：保存 current room/def/party；`waveIndex=0`；延迟 40 tick 调 `spawnWave`。
- `spawnWave`：用 `EncounterPlan.build`；从 `room.markers().mobs()` 随机取点（空则 center 周围 5 格）；对每个 `MobPlan` 生成 `EntityType`：`EntityType.byString(id)`；空则跳过并日志。`entity.setPos`；`entity.setPersistenceRequired()`；加 scoreboard tag；persistentData 写 room UUID；按 `healthMultiplier`/`damageMultiplier` 加 `AttributeModifier`（`"inf_expedition:act_scale"`, ADD_MULTIPLIED_BASE）；`entity.setHealth(entity.getMaxHealth())`；设置目标为最近玩家。Boss 波：在 `markers.bosses()` 生成，创建 `ServerBossEvent`（红、PROGRESS），add players；保存 UUID->bossbar。
- `onMobDeath`：若实体有 tag 且 room UUID 匹配 -> 从存活集合移除；若集合空：若还有下一波 -> 延迟 60 tick；否则 `RoomManager.complete(server, true)`；Boss bar remove。
- `RoomManager.complete(server, success)` 是唯一完成路径：标记房间 completed、在 `markers.rewards()` 生成箱子并设置 `LootTable`（encounter.rewardLoot 或 room.rewardLoot）、触发 `RoomCompletedEvent`；若 `room.type() == BOSS` 再调 `RunManager.end(server, true)`。`EncounterManager` 清完最后一波只调 `RoomManager.complete`，不要直接调 `RunManager.complete` 以免重复完成。`RunManager.complete` 只服务 `/infexpedition complete` 与 exit 命令，内部转调 `RoomManager.complete`。
- `tick`：每 20 tick 对每个存活怪，若不在 room AABB 内 -> 传送回 center；若目标为空 -> 找最近 party 玩家设目标；把房间外的怪清掉? 保留。
- 取消怪互相伤害：`LivingAttackEvent`/`LivingChangeTargetEvent` 检查双方 tag。

- [ ] **Step 4: 跑测试确认通过**

Run: `cd expedition && JAVA_HOME=$JAVA_HOME ./gradlew test --tests com.inf.expedition.data.EncounterPlanTest`
Expected: PASS。

- [ ] **Step 5: 手动验证**

在 run 维度放置 `battle/crypt`（Task 9 生成后更完整），手动生成遭遇；检查波次、Boss 血条、奖励箱、怪被拉回。记录到 `docs/pack/integration-log.md`。

- [ ] **Step 6: 提交**

```bash
git add expedition/src/main/java/com/inf/expedition/room expedition/src/test/java/com/inf/expedition/data/EncounterPlanTest.java
git commit -m "feat: add encounter waves, scaling and rewards"
```

---

## Task 8: 网络层、地图屏与 M 键

**Files:**
- Create: `expedition/src/main/java/com/inf/expedition/network/ExpeditionNetwork.java`
- Create: `expedition/src/main/java/com/inf/expedition/network/MapSyncPayload.java`
- Create: `expedition/src/main/java/com/inf/expedition/network/MapMoveRequestPayload.java`
- Create: `expedition/src/main/java/com/inf/expedition/network/MapMoveResponsePayload.java`
- Create: `expedition/src/main/java/com/inf/expedition/network/MapOpenRequestPayload.java`
- Create: `expedition/src/main/java/com/inf/expedition/client/{ExpeditionClient,MapClientData,MapKeyMapping,MapScreen}.java`
- Modify: `expedition/src/main/java/com/inf/expedition/Expedition.java`
- Test: `expedition/src/test/java/com/inf/expedition/run/MapSyncPayloadTest.java`

**Interfaces:**
- Consumes: Task 2/3/6。
- Produces:
  - `MapSyncPayload(CompoundTag map, int current, int act, String status, boolean canMove)`；`MapMoveRequestPayload(int nodeId)`；`MapMoveResponsePayload(boolean ok, String message)`；`MapOpenRequestPayload()`。
  - `ExpeditionNetwork.register(RegisterPayloadHandlersEvent)`；`sendMap(ServerPlayer, MapGraph, RunState)`；`requestMap(ServerPlayer)`。
  - `MapClientData.update(MapSyncPayload)`；`MapKeyMapping.OPEN_MAP`；`MapScreen`。

- [ ] **Step 1: 写 payload 失败测试**

```java
class MapSyncPayloadTest {
    @Test void mapTagRoundTripsThroughSnapshotNbt() {
        MapSnapshot snap = new MapGenerator(5L).generate(MapConfig.DEFAULT).snapshot();
        assertEquals(snap, MapSnapshotNbt.read(MapSnapshotNbt.write(snap)));
    }
}
```

（`MapSyncPayload` 的 codec 直接复用 `MapSnapshotNbt`；NBT 往返覆盖核心。）

- [ ] **Step 2: 跑测试确认失败/通过**

Run: `cd expedition && JAVA_HOME=$JAVA_HOME ./gradlew test --tests com.inf.expedition.run.MapSyncPayloadTest`
Expected: 编译失败（类未存在）→ 实现后 PASS。

- [ ] **Step 3: 实现网络与客户端**

- `MapSyncPayload` codec：`StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, MapSyncPayload::map, ByteBufCodecs.VAR_INT, MapSyncPayload::current, ByteBufCodecs.VAR_INT, MapSyncPayload::act, ByteBufCodecs.STRING_UTF8, MapSyncPayload::status, ByteBufCodecs.BOOL, MapSyncPayload::canMove, MapSyncPayload::new)`。
- `ExpeditionNetwork.register`：`registrar.playToClient(MapSyncPayload.TYPE, MapSyncPayload.STREAM_CODEC, (p,ctx)->ctx.enqueueWork(()->MapClientData.update(p)))`；`playToServer(MapOpenRequestPayload.TYPE, ..., (p,ctx)->ExpeditionNetwork.sendMap(ctx.player(), ...))`；`playToServer(MapMoveRequestPayload.TYPE, ..., (p,ctx)->RunManager.move(ctx.player(), p.nodeId()))`；`MapMoveResponsePayload` 客户端弹消息。
- `MapKeyMapping`：`new KeyMapping("key.inf_expedition.map", InputConstants.KEY_M, "key.categories.inf_expedition")`；`RegisterKeyMappingsEvent` 注册。
- `ExpeditionClient.onClientTick(ClientTickEvent.Post)`：`while (OPEN_MAP.consumeClick())` 发 `MapOpenRequestPayload`。
- `MapScreen extends Screen`：`isPauseScreen() -> false`；渲染背景（缺贴图时用暗色矩形）、标题、Act、节点圆点（按 NodeType 颜色）、连线、玩家当前位置、图例、悬停 tooltip；节点点击：若 `canMove && node reachable` 发 `MapMoveRequestPayload`；否则聊天提示。`NodeType` 颜色：BATTLE 橙、ELITE 红橙、BOSS 红、SHOP 金、REST 绿、EVENT 紫、FORGE 橙黄、START 蓝。节点坐标：`x = 40 + column * 80`，`y = height - 60 - floor * 52`；按屏幕缩放夹取。
- 地图打开：M 键；关闭：ESC。

- [ ] **Step 4: 客户端冒烟**

```bash
cd .
timeout 180 xvfb-run -a env JAVA_HOME=$JAVA_HOME ./gradlew runClient --console=plain > /tmp/exp-client.log 2>&1
grep -qE 'Done|Backend library' /tmp/exp-client.log
! grep -q 'Mixin apply failed' /tmp/exp-client.log
```

Expected: 客户端到主菜单（日志无 crash）。截图可选（`xwd` 不可用时跳过）。

- [ ] **Step 5: 提交**

```bash
git add expedition/src/main/java/com/inf/expedition/{network,client} expedition/src/main/java/com/inf/expedition/Expedition.java expedition/src/test/java/com/inf/expedition/run/MapSyncPayloadTest.java
git commit -m "feat: add map screen, keybind and networking"
```

---

## Task 9: NBT 生成器、大厅平台与 v0.1 房间

**Files:**
- Create: `tools/gen_rooms.py`
- Create: `tools/test_gen_rooms.py`
- Create: `pack/overrides/kubejs/data/inf_expedition/structure/rooms/<type>/<name>.nbt`（生成）
- Create: `pack/overrides/kubejs/data/inf_expedition/structure/hub_platform.nbt`（生成）
- Create: `pack/overrides/kubejs/data/inf_expedition/expedition/rooms/*.json`（生成）
- Create: `pack/overrides/kubejs/data/inf_expedition/expedition/encounters/*.json`（生成）
- Create: `pack/overrides/kubejs/data/inf_expedition/loot_table/chests/*.json`（生成）
- Create: `pack/overrides/kubejs/data/inf_expedition/expedition/map.json`

**Interfaces:**
- Consumes: 设计 §9.2 标记约定。
- Produces: 10 个房间 + hub 平台；生成脚本可重复运行；`python3 tools/gen_rooms.py --check` 自检。

- [ ] **Step 1: 写 Python NBT 写入器与自检测试**

`tools/gen_rooms.py` 必须包含：

```python
def write_structure(path, size, palette, blocks, block_entities=()) -> None
def room_specs() -> list[RoomSpec]
def main(argv) -> int  # --out, --check
```

NBT 根 compound：`size`（3 ints）、`palette`（list of {Name, Properties?}）、`blocks`（list of {pos:[x,y,z], state:int, nbt?}）、`entities`（空 list）、`DataVersion` int `3955`；gzip 压缩。`blocks` 的 `pos` 是相对坐标；palette index 必须在范围。

`tools/test_gen_rooms.py`（`unittest`）：

- 生成到临时目录；用最小 NBT reader 解析；断言 `size` 与 spec 一致；所有 `state` 在 palette 范围；`spawn`/`exit` 标记数量 ≥1；gzip 文件可读。
- 运行：`python3 tools/test_gen_rooms.py -v`。

- [ ] **Step 2: 实现 10 个房间**

| id | 尺寸 | 必需标记 | 说明 |
|---|---|---|---|
| `start/camp` | 24×8×24 | spawn×1, exit×1, reward×1 | 锻刀台、起始箱 |
| `battle/crypt` | 28×10×28 | spawn×1, exit×1, mob×4, reward×1 | 墓穴 |
| `battle/forge_hall` | 28×10×28 | 同上 | 废弃锻炉 |
| `battle/cavern` | 30×12×30 | 同上，mob×5 | 洞窟 |
| `elite/arena` | 30×12×30 | 同上，mob×5 | 竞技场 |
| `forge/smithy` | 24×8×24 | spawn×1, exit×1, reward×1 | 锻刀台 + 工具箱 |
| `rest/camp` | 20×8×20 | spawn×1, exit×1, rest×2 | 营火/床 |
| `shop/stall` | 20×8×20 | spawn×1, exit×1, shop×1, reward×1 | 商店箱/商人 |
| `event/treasure` | 24×8×24 | spawn×1, exit×1, reward×1, mob×3 | 宝箱 + 伏击 |
| `boss/wroughtnaut` | 36×14×36 | spawn×1, exit×1, boss×1, reward×1 | Boss 竞技场 |

每个房间由 spec 里的地板/墙/装饰方块生成；房间必须封闭、有照明、出口可达。标记用 `minecraft:jigsaw`，block entity `name` 为上述标记名，`target`/`pool` 空，`final_state` 为 `minecraft:air`。

`hub_platform` 32×8×32：中心 `inf_expedition:expedition_altar`，旁边 `inf_expedition:quest_board`，出生点上方 2 格留空，四周有围墙/屋顶。

- [ ] **Step 3: 生成房间 JSON 与遭遇**

每个房间 JSON 按 Task 4 schema；`structure` 为 `inf_expedition:rooms/<type>/<name>`；`encounter` 指向同 id；`reward_loot` 指向 `inf_expedition:chests/<name>`。遭遇：

- `battle/crypt`：2 波（zombie 4；skeleton 3 + zombie 2）
- `battle/forge_hall`：2 波（pillager 3；vindicator 2 + pillager 2）
- `battle/cavern`：3 波（spider 4；cave_spider 4；silverfish 5）
- `elite/arena`：1 波（`legendary_monsters:ambusher` 1 + zombie 2）
- `boss/wroughtnaut`：1 波（`mowziesmobs:ferrous_wroughtnaut` 1），`boss:true`
- 缩放 `health_per_act=0.35`、`damage_per_act=0.2`、`count_per_act=0.2`、`health_per_extra_player=0.5`、`damage_per_extra_player=0.25`；v0.1 act=1，实际不触发 act 缩放。

**实体 ID 必须在 pack 加载后实测**：`/summon mowziesmobs:ferrous_wroughtnaut` 与 `/summon legendary_monsters:ambusher` 成功才保留；失败则改用 `minecraft:ravager` / `minecraft:witch` 作为 v0.1 回退，并记录到 `docs/pack/integration-log.md`。

- [ ] **Step 4: 生成战利品表**

- `chests/battle_common`：emerald 4–8、iron_ingot 2–4、cooked_beef 3–5、随机 `inf:*_blank` 1（权重 20）。
- `chests/elite`：emerald 8–12、`inf:hammer` 1、blank 2、golden_apple 1。
- `chests/boss`：`inf:hammer` 1、blank 2、emerald 16–24、diamond 2–4。
- `chests/forge_cache`：blank 2–3、`inf:hammer` 1（50%）、coal 8。
- `chests/event`：emerald 10–20、golden_apple 1–2、random blank 1。

用标准 `minecraft:chest` loot table JSON；物品 id 用字符串。

- [ ] **Step 5: 运行自检与生成**

```bash
cd .
python3 tools/test_gen_rooms.py -v
python3 tools/gen_rooms.py --out pack/overrides/kubejs/data/inf_expedition --check
```

Expected: tests PASS；10 个房间 + hub 平台 + JSON 生成；`--check` 输出 `OK`。

- [ ] **Step 6: 提交**

```bash
git add tools/gen_rooms.py tools/test_gen_rooms.py pack/overrides/kubejs/data/inf_expedition
git commit -m "feat: generate v0.1 rooms, hub platform and loot"
```

---

## Task 10: 整合包骨架、模组清单与打包脚本

**Files:**
- Create: `pack/mods.yaml`
- Create: `pack/overrides/README.md`
- Create: `pack/overrides/config/`（初始配置占位）
- Create: `tools/build_pack.py`
- Create: `tools/test_build_pack.py`
- Create: `CREDITS.md`
- Modify: `.gitignore`（`pack/export/`, `pack/local-mods/`, `pack/cache/`）

**Interfaces:**
- Consumes: 设计 §11.1 与 §21 的模组清单。
- Produces:
  - `python3 tools/build_pack.py --check`：校验清单/本地 jar/overrides。
  - `python3 tools/build_pack.py --zip`：生成 `pack/export/duantu-qianchui-0.1.0.zip`（CF manifest + overrides）。
  - `python3 tools/build_pack.py --local`：生成 `pack/export/local/` 可玩目录。

- [ ] **Step 1: 写 mods.yaml**

```yaml
pack:
  name: "锻途：千锤之路"
  version: "0.1.0"
  minecraft: "1.21.1"
  loader: "neoforge-21.1.266"
  author: "Inf"
mods:
  - name: "KubeJS"
    source: curseforge
    project_id: 238086  # 已确认
    file_id: null       # 实现时锁定
    side: both
  - name: "FTB Quests"
    source: curseforge
    project_id: 289412  # 已确认
    file_id: null       # 实现时用 CurseForge 页面/API 解析并锁定
    side: both
  # 其余按设计 §21 表逐项加入；每项必须有 name/source/side
local_mods:
  - name: "Inf"
    path: "build/libs/inf-0.2.0.jar"          # 相对仓库根目录
  - name: "Inf Expedition"
    path: "expedition/build/libs/inf_expedition-0.1.0.jar"
overrides: "overrides"
```

必须加入 v0.1 核心：KubeJS、FTB Quests/Library/Teams、JEI、Jade、Curios、Lootr、Waystones、Apothic Attributes/Placebo、Sophisticated Backpacks/Core、Farmer's Delight、Mowzie's Mobs、Legendary Monsters、Cataclysm、Bosses'Rise、Aquamirae、ModernFix、FerriteCore、Embeddium、Clumps、GeckoLib、Architectury API、Balm、Cloth Config、Lionfish API、Xaero's Minimap、Controlling、Searchables、Mouse Tweaks、IPN、Inventory HUD+、FancyMenu/Konkrete/Melody、Tips、Drippy。每条都要写 `side: client|server|both` 与 CurseForge `project_id/file_id`。若某模组在 CurseForge 上没有对应 1.21.1 版本，替换为 CF 上同版本或从清单移除；不要用 Modrinth 条目生成 CF zip。

- [ ] **Step 2: 写 build_pack.py**

```python
def load_spec(path: Path) -> dict
def validate(spec: dict, root: Path) -> list[str]
def build_manifest(spec: dict) -> dict
def build_zip(spec: dict, root: Path, out: Path) -> Path
def build_local(spec: dict, root: Path, out: Path) -> Path
```

规则：

- `validate`：pack 版本/loader/维度；每个模组有 `name/source/side`；curseforge 条目有 `project_id/file_id`（`file_id` 为 null 时 warning 而非 error）；local_mods 文件存在；`overrides` 目录存在。
- `build_manifest`：CF manifest 结构，`files` 仅包含 curseforge 条目。`--zip` 遇到 `source: modrinth` 的条目必须 **error**（CF manifest 不能引用 Modrinth）；所有进入 zip 的第三方模组都必须在 CurseForge 上有 project/file id。Modrinth 只用于版本核验或本地开发。
- `build_zip`：把 `overrides/` 复制到临时目录；把 local_mods 复制到 `overrides/mods/`；写 `manifest.json`；zip；不包含第三方 jar。
- `build_local`：创建 `mods/`，复制 local_mods；若 `pack/cache/<name>.jar` 存在则复制第三方 jar，否则打印缺失清单；复制 overrides 内容到根。
- `--check` 退出码 0/1。

- [ ] **Step 3: 写 Python 测试**

`tools/test_build_pack.py`：临时目录构造最小 pack（1 个 local_mod、1 个 curseforge、1 个 modrinth），断言 `build_manifest` 的 loader/文件数、`build_zip` 内 manifest.json 可解析、`build_local` 的 mods 目录包含 local jar；`file_id=null` 时 `validate` 返回 warning 而非 error。

Run: `python3 tools/test_build_pack.py -v` → PASS。

- [ ] **Step 4: 运行检查**

```bash
cd .
python3 tools/build_pack.py --check
```

Expected: 输出缺失的本地 jar/未锁定 file_id 清单；本地 jar 在 Task 1 与 Task 9 之后存在。**不要求此刻所有第三方 jar 已下载**；`--check` 必须能区分 error 与 warning。

- [ ] **Step 5: 提交**

```bash
git add pack tools/build_pack.py tools/test_build_pack.py CREDITS.md .gitignore
git commit -m "feat: add pack manifest, mod list and build script"
```

---

## Task 11: KubeJS 内容层与 expedition advancements

**Files:**
- Create: `expedition/src/main/resources/data/inf_expedition/advancement/{first_run,first_room,first_boss,clear_act1,clears_3}.json`
- Create: `expedition/src/main/java/com/inf/expedition/advancement/ExpeditionAdvancements.java`
- Modify: `RunManager`（award points）
- Create: `pack/overrides/kubejs/server_scripts/expedition/start_kit.js`
- Create: `pack/overrides/kubejs/server_scripts/expedition/shop.js`
- Create: `pack/overrides/kubejs/server_scripts/expedition/rest.js`
- Create: `pack/overrides/kubejs/server_scripts/expedition/rules.js`
- Create: `pack/overrides/kubejs/server_scripts/expedition/boss.js`
- Create: `pack/overrides/kubejs/client_scripts/expedition/tooltips.js`
- Create: `pack/overrides/kubejs/startup_scripts/expedition/items.js`（仅注册提示，不新增物品）
- Test: none（由 server/client 日志与手动验收覆盖）

**Interfaces:**
- Consumes: Task 6 事件；Task 9 房间 JSON；Task 10 pack skeleton。
- Produces: 起始包追加、商店、休息、规则、clears/scoreboard、advancement 授予。

- [ ] **Step 1: 写 advancement JSON + award helper**

五个 advancement 都用 `criteria: {"unlocked": {"trigger": "minecraft:impossible"}}`，`display` 用 `inf_expedition:textures/gui/advancement_bg.png`（Task 13 生成，缺失时用 `minecraft:textures/block/stone.png` 回退）；标题/描述走 lang。

`ExpeditionAdvancements.award(ServerPlayer, String path)` 复刻 Inf 的 `ForgeAdvancements` 模式：从 `server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("inf_expedition", path))` 取 holder，`player.getAdvancements().award(holder, "unlocked")`。在 `RunManager.start` 授予 `first_run`；`RoomManager.complete` 授予 `first_room`；Boss 结束授予 `first_boss`；`RunManager.end(true)` 授予 `clear_act1`；KubeJS 在 clears>=3 时授予 `clears_3`。

- [ ] **Step 2: 写 KubeJS 脚本**

`start_kit.js`：

```js
NativeEvents.onEvent(Java.loadClass('com.inf.expedition.event.RunStartedEvent'), event => {
  event.getPlayers().forEach(player => {
    let clears = player.persistentData.getInt('inf_expedition_clears')
    if (clears >= 1) player.give('inf:sword_blank')
    if (clears >= 3) player.give('inf:hammer')
    if (clears >= 5) player.give('minecraft:potion{Potion:"minecraft:strong_healing"}')
  })
})
```

`shop.js`：监听 `RoomEnteredEvent`；若 `event.getRoom().type().id() === 'shop'`，取 `event.getRoom().markers().shops().get(0)`，生成 `minecraft:villager`（`level.createEntity('minecraft:villager')`，`setPos(x,y,z)`，`spawn()`），并把该位置写入脚本级变量 `shopChestPos`。`BlockEvents.rightClicked` 时若 `event.block.pos` 等于 `shopChestPos`：手持绿宝石右键 -> 扣 4 个、给随机刀胚/熟牛肉/药水，聊天提示；空手 -> 显示价格。`RoomCompletedEvent` 时清空 `shopChestPos`。**不依赖 MerchantJS/Lightman's。**

`rest.js`：`RoomEnteredEvent` 时把 `event.getRoom().markers().rests()` 写入脚本级变量 `restPositions`。`BlockEvents.rightClicked` 若方块是 `minecraft:campfire` 且位置在 `restPositions` 中，且该房间未使用过（`player.persistentData` 存 room UUID），回 50% 最大生命并标记；`RoomCompletedEvent` 清空 `restPositions`。

`rules.js`：`ServerEvents.loaded` 设置 `gamerule doMobSpawning false`、`keepInventory true`、`mobGriefing false`、`doFireTick false`、`naturalRegeneration true`；每 100 tick 清除非房间 tag 的敌对怪（仅 run 维度）。

`boss.js`：监听 `RunEndedEvent`；仅当 `event.isSuccess()` 时，对每个 party 玩家 `persistentData.putInt('inf_expedition_clears', +1)`，同步 `scoreboard players set <name> inf_expedition_clears <n>`，并在 clears>=3 时 `advancement grant <name> only inf_expedition:clears_3`。成功/失败都给聊天提示。clear_act1 由 mod 在通关时授予。

`tooltips.js`：`ItemEvents.tooltip` 给 recall scroll/altar 加说明；聊天提示 M 键。

若 KubeJS 无法解析自定义事件类（`NativeEvents.onEvent(Java.loadClass(...))` 报错），回退为：mod 在 `RunStartedEvent`/`RunEndedEvent` 时写 `scoreboard` 与 `persistentData`，KubeJS 在 `PlayerEvents.tick` 每 20 tick 轮询。不得因此修改 mod 的权威状态。

- [ ] **Step 3: 服务端加载验证**

用整合包启动（Task 14）或 `pack/export/local` 启动服务器，检查 `logs/kubejs/server.log` 无语法错误；`/reload` 不报错。Expected: 脚本全部 loaded。

- [ ] **Step 4: 提交**

```bash
git add expedition/src/main/resources/data/inf_expedition/advancement expedition/src/main/java/com/inf/expedition/advancement expedition/src/main/java/com/inf/expedition/run pack/overrides/kubejs
git commit -m "feat: add kubejs content layer and expedition advancements"
```

---

## Task 12: FTB Quests《锻途指南》

**Files:**
- Create: `pack/overrides/config/ftbquests/quests/data.snbt`
- Create: `pack/overrides/config/ftbquests/quests/chapter_groups.snbt`
- Create: `pack/overrides/config/ftbquests/quests/chapters/duantu.snbt`
- Create: `pack/overrides/config/ftbquests/quests/reward_tables/*.snbt`
- Create: `pack/overrides/kubejs/assets/ftbquests/lang/zh_cn.json` / `en_us.json`（若需要）

**Interfaces:**
- Consumes: Task 11 advancements/scoreboard；Inf advancements。
- Produces: 12 个任务的指南章节；默认自动领取。

- [ ] **Step 1: 写 data.snbt 与 chapter group**

`data.snbt`：`default_autoclaim_rewards: "enabled"`、`progression_mode: "flexible"`、`drop_book_on_death: false`、`pause_game: false`、`version: 13`。`chapter_groups.snbt` 一组 `default`。

- [ ] **Step 2: 写 12 个任务**

章节 `duantu`（title `chapter.inf_expedition.duantu`），任务：

1. 领取起始包 — checkmark。
2. 打开地图（M）— checkmark。
3. 锻造第一把武器 — advancement `inf:first_weapon`。
4. 锻出精良 — advancement `inf:tier_fine`。
5. 完成第一场战斗 — advancement `inf_expedition:first_room`。
6. 进入锻造房间 — checkmark（房间类型）。
7. 在商店消费 — checkmark。
8. 在休息点回血 — checkmark。
9. 击败精英 — advancement `inf_expedition:first_boss`？不，精英用 checkmark；Boss 用 first_boss。
10. 击败 Act 1 Boss — advancement `inf_expedition:first_boss`。
11. 完成一次远征 — advancement `inf_expedition:clear_act1`。
12. 累计通关 3 次 — `type: "advancement"`，`advancement: "inf_expedition:clears_3"`（KubeJS 在 clears>=3 时授予）。

奖励：XP、cooked_beef、emerald、blank；不奖励 Boss 级武器。

- [ ] **Step 3: 客户端加载验证**

在 Task 14 的 pack 客户端打开任务书；检查章节、图标、任务依赖、自动领取。Expected: 无 missing icon/quest id 错误。

- [ ] **Step 4: 提交**

```bash
git add pack/overrides/config/ftbquests pack/overrides/kubejs/assets/ftbquests
git commit -m "feat: add act 1 quest guide"
```

---

## Task 13: FancyMenu、地图贴图与完整中英文文本

**Files:**
- Modify: `tools/gen_assets.py`
- Create: `pack/overrides/config/fancymenu/assets/*`（生成）
- Create: `pack/overrides/config/fancymenu/customization/title_screen.txt`
- Create: `pack/overrides/config/fancymenu/options.txt`（最小）
- Create: `expedition/src/main/resources/assets/inf_expedition/textures/gui/map/*`（生成）
- Complete: all `lang/zh_cn.json` / `en_us.json`

**Interfaces:**
- Consumes: Task 8 `MapScreen` 期望的贴图路径；Task 9 房间标题 key。
- Produces: 可辨识的标题背景/加载提示/地图节点图标/玩家标记；语言键无缺失。

- [ ] **Step 1: 扩展 gen_assets.py**

生成：

- `pack/overrides/config/fancymenu/assets/title_bg.png` 1920×1080（暗色锻炉/火星/铁砧剪影）。
- `title_logo.png` 800×200（「锻途：千锤之路」用 PIL 字体绘制；若中文字体缺失，用英文 `THOUSAND HAMMERS` 并在 FancyMenu 文本元素里放中文）。
- `loading_tips.txt`（10 条锻造/远征提示）。
- `expedition/src/main/resources/assets/inf_expedition/textures/gui/map/background.png` 1024×768。
- `nodes/{battle,elite,boss,shop,rest,event,forge,start}.png` 32×32。
- `player.png` 16×16。

运行 `python3 tools/gen_assets.py`；输出到对应目录。PIL 字体用 `ImageFont.load_default()` 或系统字体 `/usr/share/fonts/...`；找不到中文字体时用英文占位并在文档记录。

- [ ] **Step 2: 写 FancyMenu 配置**

`title_screen.txt` 使用 FancyMenu 4 格式（与参考包同结构，但内容原创）：背景图、logo、版本文字、单机/多人按钮保留。`options.txt` 开启 `loading_screen` 提示列表（若 FancyMenu 版本字段不同，以加载日志为准）。**不复制参考包配置。**

- [ ] **Step 3: 补全 lang**

所有 `inf_expedition.*`、`gui.inf_expedition.*`、`room.inf_expedition.*`、`encounter.inf_expedition.*`、`chapter.inf_expedition.*`、`quest.inf_expedition.*` 键同时存在 `zh_cn` 与 `en_us`。用脚本检查：

```bash
python3 - <<'PY'
import json, pathlib
for lang in ['zh_cn','en_us']:
    p=pathlib.Path(f'expedition/src/main/resources/assets/inf_expedition/lang/{lang}.json')
    d=json.loads(p.read_text())
    assert d, p
    zh=json.loads(pathlib.Path('expedition/src/main/resources/assets/inf_expedition/lang/zh_cn.json').read_text())
    en=json.loads(pathlib.Path('expedition/src/main/resources/assets/inf_expedition/lang/en_us.json').read_text())
    assert set(zh) == set(en), (set(zh) ^ set(en))
print('lang ok')
PY
```

- [ ] **Step 4: 客户端截图验证**

```bash
cd .
timeout 180 xvfb-run -a env JAVA_HOME=$JAVA_HOME ./gradlew runClient --console=plain > /tmp/exp-client2.log 2>&1
grep -qE 'Done|Backend library' /tmp/exp-client2.log
```

若 `xwd`/ImageMagick 可用，截主菜单；否则记录日志。Expected: 无 crash，贴图缺失日志为空。

- [ ] **Step 5: 提交**

```bash
git add tools/gen_assets.py pack/overrides/config/fancymenu expedition/src/main/resources/assets/inf_expedition
git commit -m "feat: add title screen, map art and bilingual text"
```

---

## Task 14: 整合包集成冒烟（核心模组 → 内容模组）

**Files:**
- Modify: `pack/mods.yaml`（锁定 file_id/版本）
- Create: `pack/overrides/config/`（Inf、expedition、ftbquests、fancymenu、jade、xaero 等）
- Create: `docs/pack/integration-log.md`
- Modify: `tools/build_pack.py`（如需）

**Interfaces:**
- Consumes: Task 1–13。
- Produces: 可启动的本地整合包目录；集成日志；修复清单。

- [ ] **Step 1: 构建两份 jar 并复制到 pack/local-mods**

```bash
cd .
JAVA_HOME=$JAVA_HOME ./gradlew build
JAVA_HOME=$JAVA_HOME ./gradlew -p expedition build
python3 tools/build_pack.py --local
```

Expected: `pack/export/local/mods/` 有两份 jar；若第三方 jar 缺失，脚本列出清单。

- [ ] **Step 2: 核心 15 模组启动**

先只放：Inf、Inf Expedition、KubeJS、FTB Quests/Library/Teams、JEI、Jade、Curios、Architectury API、Balm、Cloth Config、GeckoLib、Lionfish API、ModernFix、FerriteCore、Clumps、Embeddium、FancyMenu/Konkrete/Melody。用本地启动器/`pack/export/local` 启动到主菜单；进世界；验收：

- 自动到 hub；hub 平台/祭坛/任务板存在。
- 右键祭坛开局；M 打开地图；点击 START 后续节点能放置房间（Task 9 的 `start/camp`）。
- 锻刀台可用；起始包有锤子/刀胚/返回卷轴/任务书。

若本机没有 Prism/HMCL，完整包启动由用户执行；自动化只到 `--check` 与 `runClient` 冒烟。记录到 `docs/pack/integration-log.md`。

- [ ] **Step 3: 加入内容模组**

按 `pack/mods.yaml` 逐步加入 Mowzie's、Legendary Monsters、Cataclysm、Bosses'Rise、Aquamirae、Farmer's Delight、Sophisticated、Lootr、Waystones、Apothic、Xaero's、IPN、Inventory HUD+、Tips、Drippy。每次只加 2–3 个，启动到主菜单/进世界；记录缺失依赖、mixin 冲突、注册表错误。

- [ ] **Step 4: 修复并锁定**

- 修改 `mods.yaml` 的 `file_id`/版本至实际可启动组合。
- 若某内容模组导致启动失败，移入 `optional` 列表并记录原因。
- 更新 `docs/pack/integration-log.md` 的最终清单。

- [ ] **Step 5: 提交**

```bash
git add pack/mods.yaml pack/overrides/config docs/pack/integration-log.md
git commit -m "feat: integrate pack mods and record smoke results"
```

---

## Task 15: Act 1 完整平衡、死亡/失败与可选双人

**Files:**
- Modify: `pack/overrides/kubejs/data/inf_expedition/expedition/encounters/*.json`
- Modify: `pack/overrides/kubejs/data/inf_expedition/loot_table/chests/*.json`
- Modify: `pack/overrides/config/inf-common.toml`（如需要）
- Create: `docs/pack/act1-balance.md`

**Interfaces:**
- Consumes: Task 14。
- Produces: 可完整通关的 Act 1；平衡记录；验收结果。

- [ ] **Step 1: 单人完整跑一轮**

按顺序：hub → START → 至少 3 个战斗节点 → FORGE → REST → SHOP → ELITE → BOSS。记录：

- 每房间用时、死亡次数、锻造出的最高阶位、剩余锤子耐久。
- Boss 战是否在 3 次尝试内可过。
- 起始包/掉落是否足够（锤子耐久、刀胚数量）。

- [ ] **Step 2: 调整数值**

只允许改数据包 JSON 与 `inf-common.toml` 的 `baseGain`/`riskPerStrike`/`gainVariance`（不改 Inf 代码/公式）。目标：

- 普通战斗 2–4 分钟/房间。
- 一局 20–35 分钟。
- Boss 需要至少精良/稀有武器，但不要求传说。
- 死亡/失败后下一局仍可在 5 分钟内重新起步。

把每次调整与结果写入 `docs/pack/act1-balance.md`。

- [ ] **Step 3: 死亡/失败/重连路径**

- 单人死亡：回 hub，背包清空，clears 不变。
- 主动返回卷轴：回 hub，run 结束。
- 服务器重启：run 放弃，登录回 hub。
- 进行中重连：回当前房间 spawn（若队伍仍在运行）。
记录到 `docs/pack/act1-balance.md`。

- [ ] **Step 4: 可选双人 LAN（不阻塞）**

若时间允许：两名玩家开局、共享节点、死亡旁观/下一房间复活、人数缩放。记录结果；不做则写 `not tested in v0.1`。

- [ ] **Step 5: 提交**

```bash
git add pack/overrides docs/pack/act1-balance.md
git commit -m "feat: tune act 1 and record balance results"
```

---

## Task 16: 发布验证与打包

**Files:**
- Create: `docs/pack/release-checklist.md`
- Modify: `CREDITS.md`
- Create: `pack/export/duantu-qianchui-0.1.0.zip`（生成，git 忽略）

**Interfaces:**
- Consumes: Task 1–15。
- Produces: 可分发 zip；验证记录；最终提交。

- [ ] **Step 1: 全量自动化**

```bash
cd .
JAVA_HOME=$JAVA_HOME ./gradlew clean test build
JAVA_HOME=$JAVA_HOME ./gradlew -p expedition clean test build
python3 tools/test_gen_rooms.py && python3 tools/test_build_pack.py
python3 tools/gen_rooms.py --check
python3 tools/build_pack.py --check
```

Expected: 全部 PASS；`--check` 无 error（warning 可接受，列在 release checklist）。

- [ ] **Step 2: 客户端/服务端冒烟**

```bash
cd expedition
timeout 180 env JAVA_HOME=$JAVA_HOME ./gradlew runServer --console=plain > /tmp/final-server.log 2>&1
grep -q 'Done' /tmp/final-server.log
timeout 180 xvfb-run -a env JAVA_HOME=$JAVA_HOME ./gradlew runClient --console=plain > /tmp/final-client.log 2>&1
grep -qE 'Done|Backend library' /tmp/final-client.log
```

- [ ] **Step 3: 生成发布 zip**

```bash
cd .
python3 tools/build_pack.py --zip
ls -lh pack/export/duantu-qianchui-0.1.0.zip
unzip -l pack/export/duantu-qianchui-0.1.0.zip | grep -E 'manifest.json|overrides/mods/inf|overrides/kubejs'
```

Expected: zip 包含 `manifest.json`、两份自研 jar、KubeJS 数据、FTB Quests、FancyMenu；不含第三方 jar。

- [ ] **Step 4: 写 release checklist**

`docs/pack/release-checklist.md` 按设计 §12.2 的 9 条逐项打勾；未执行的双人/截图项写 `N/A (v0.1 single-player first)`。包括：

- [ ] 世界创建 → 大厅 → 开局 → 地图。
- [ ] 10 个房间可进入/清空/离开。
- [ ] Boss 通关 → clears +1 → 下一局地图不同。
- [ ] 死亡/失败/重连/重启路径。
- [ ] 锻刀台在起始/锻造房间可用。
- [ ] 任务书 12 任务可完成。
- [ ] 中英文文本无 missing key。
- [ ] 整合包启动无缺失依赖。
- [ ] `./gradlew test` / expedition test / Python tests 全绿。

- [ ] **Step 5: 提交**

```bash
git add docs/pack CREDITS.md
git commit -m "docs: add v0.1 release checklist and credits"
```

---

## Self-Review Notes

- **Spec coverage:** 设计 §7 维度/大厅 → Task 1/6；§8 节点地图 → Task 2/3/8；§9 房间/遭遇 → Task 4/5/7/9；§10 Inf 集成 → Task 6/11/15；§11 整合包 → Task 10/11/12/13/14；§12 验收 → Task 15/16；§13 测试 → 各任务；§14 风险 → Task 10/14；§17–22 → Global Constraints。
- **Type consistency:** `MapGraph`/`MapSnapshot`/`NodeSnapshot` 在 Task 2 定义，Task 3/8 复用；`RoomDefinition`/`EncounterDefinition` 在 Task 4 定义，Task 5/7/9 复用；`RunManager` 在 Task 6 定义，Task 7/8/11 复用；`RoomInstance` 在 Task 5 定义，Task 6/7/11 复用。
- **Open builder decisions:** 房间装饰、Boss 最终选择（Wroughtnaut 失败则回退）、FancyMenu 字体、CurseForge file id 解析、双人是否验证。各自在任务内给出默认值。
- **Known environment constraints:** 无第三方启动器；完整整合包验收需用户提供启动器/模组缓存。`xvfb-run` + llvmpipe 已确认可用于客户端冒烟。
