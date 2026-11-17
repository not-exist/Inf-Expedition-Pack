# 锻途：千锤之路 整合包设计文档

- 日期：2026-11-16
- 状态：已批准；独立整合包仓库设计记录
- 仓库：`not-exist/Inf-Expedition-Pack`
- 核心依赖：`inf-core` 子模块固定在 `Inf v0.2.0`
- 英文名：`Inf: Thousand Hammers`（暂定，随中文名一起使用）
- 目标：以 `inf-core` 子模块（Inf v0.2.0，Minecraft 1.21.1 / NeoForge 锻刀模组）为核心，仿照参考包的「节点地图 + NBT 房间 + 多 Act 肉鸽」结构，制作一个可玩、可扩展的整合包《锻途：千锤之路》。参考包路径仅存在于原始研究记录，不是新仓库的运行时依赖。
- 参考包：Forge 1.20.1，约 144 个模组，核心为 Tetra + 自研 `rougemap` / `hp_rouge` 模组。参考包只作为行为与结构参考，不复制其脚本、贴图或自定义 jar。

---

## 1. 意图与为什么

Inf 当前的循环只发生在锻刀台 GUI 里：放胚、落锤、价值上升/碎裂风险上升、停手结算。它缺少「为什么现在要去锻这把刀」的外部压力。

参考包给出的答案是：把锻造放进一个可重复的远征里。玩家从大厅出发，在节点地图上选择路线，进入一个个房间；战斗、事件、商店和休息室提供材料与风险，Boss 门推进 Act。锻造不再是一个孤立的小游戏，而是每一层关卡之间的资源决策。

本整合包要复刻这个结构，但把锻造核心从 Tetra 换成 Inf：

- Tetra 的模块化武器与打磨 → Inf 的五种武器、六阶位、四结果风险锻造。
- 参考包的 NBT 房间和节点地图 → 我们自研的配套地图模组 `Inf Expedition`。
- 参考包的任务书、菜单、商店、遗物 → 按 Inf 的节奏重新设计，v0.1 只做必要部分。

## 2. 目标 / 非目标 / 反目标

### 2.1 目标

- 一个可从大厅完整游玩的 Act 1：节点地图、8~10 个房间、1 个 Boss、锻造接入、任务书。
- 玩家在一局内用 Inf 锻造武器，带着武器清房间；死亡或通关后回到大厅，开始下一局。
- 地图是节点图而不是手搭世界；房间是可替换的 NBT 模板；每局地图随机。
- 单人优先；地图/房间状态按队伍共享设计，架构支持联机；双人 LAN 作为 P6 可选验证。服务端权威。
- 所有新增文本同时提供 `zh_cn` 与 `en_us`，默认中文。
- 工程可重复构建：Inf jar、`inf_expedition` jar、房间 NBT、整合包 zip 都由脚本产出。

### 2.2 非目标（v0.1）

- Act 2、Act 3、最终 Boss、跨 Act 难度树。
- 商店经济系统（自定义货币、NPC 交易 GUI）、遗物系统、技能树。
- Inf 的材质分级、龙贡、遗物机、控温小游戏。
- 自定义 FancyMenu 完整主菜单（只做标题背景与加载提示）。
- 专用服务器部署、CurseForge/Modrinth 正式发布页。
- 与 Tetra 的兼容（1.21.1 无 Tetra）。

### 2.3 反目标

- 不做成「一堆模组 + 一张手搭地图」的普通冒险包；地图与房间必须是可随机重组的系统。
- 不让 Inf 变成可选内容；锻造必须是一局内最核心的成长手段。
- 不复制参考包的 KubeJS 脚本、NBT 房间、贴图、自定义 jar；只借鉴其结构和交互。
- 不为了凑模组数量加入与核心循环无关的大型内容模组。

## 3. 约束

- Minecraft 1.21.1、NeoForge 21.1.x、Java 21、Mojang 官方映射。
- Inf 保持现有 `inf` mod id 与公开行为；v0.1 不要求修改 Inf 源码。整合包通过物品 ID、事件和 KubeJS 与其集成。
- 新地图模组独立于 Inf 构建，mod id `inf_expedition`，不硬依赖 Inf；这样 Inf 仍可单独使用。
- 参考包中的 `rougemap`、`hp_rouge`、`gatewaysjs`、`ChatBox` 等自定义 jar 不可复用；等价能力由 `inf_expedition` + 数据包 + KubeJS 实现。
- 当前环境 shell 外网 TLS 受限；模组下载与版本锁定必须在实现阶段用可用网络（或用户提供的模组缓存）完成。构建使用 Java 21，具体路径由环境提供。
- 无硬件 GPU 假设；已确认 `xvfb-run` + Mesa llvmpipe（OpenGL 4.5）可跑客户端冒烟。FancyMenu、Oculus 等客户端模组只做可选。

## 4. 参考包拆解（已确认的「地图构建方式」）

1. **出发大厅**：`kubejs:hone` 维度，虚空 + 4×4 个 `base_structure` NBT 平台，固定在 (0,0,0) 附近。玩家登录/重生被传送到这里。
2. **远征维度**：`kubejs:rouge` 维度，虚空 + y=0 基岩地板，固定 `the_void` 生物群系。同一时间只保留一个房间，放在原点。
3. **节点地图**：自研 `rougemap` 模组。每个 Act 生成分层节点图（楼层 × 列 × 路径），节点类型包括 BATTLE / ELITE / BOSS / SHOP / REST / EVENT / FORGE / START / FINAL_BOSS / UNKNOWN。地图按 M 打开，点击节点触发 `MapNodeEnterEvent`；服务端校验可达性后进入。
4. **房间生成**：`room.js` 在节点进入时先 `clear_old_area`（清空旧 AABB、移除实体），再从 `kubejs/structures/nbt/<类型>/` 随机抽一个 NBT，居中放到 (0,0,0)，用 Loquat 区域管理器创建 AABB 区域。
5. **房间策略**：`place_strucures_strategy` 按房间 key 决定传送点、对话、传送门生成、Boss 门等。
6. **战斗与推进**：Gateways to Eternity 生成波次门；Boss 门完成 → `nextAct` 重新生成地图；失败/死亡 → 回大厅、清背包、重置地图；一局 3 个 Act。
7. **保护与规则**：房间内禁止破坏/放置（除白名单）、禁止爆炸、禁止生物破坏、把跑出区域的怪拉回、死亡变旁观直到下一层。
8. **内容层**：FTB Quests 任务书、FancyMenu 主菜单、ChatBox 对话、Lightman's Currency 商店、Curios 遗物、Pufferfish 难度技能树。

我们的实现保留 1–7 的结构，8 按 v0.1 范围裁剪。

## 5. 方案选择

### 5.1 版本 / 加载器（用户已选）

- **选中：1.21.1 NeoForge。** 保留 Inf 现有代码与数据组件；参考包中的 Tetra、Alex's Caves 等不可用，用 1.21.1 可用内容替代。
- 未选：移植 Inf 到 1.20.1 Forge（可复用参考包生态，但需要重写数据组件/注册/网络层，约数周）；双版本（维护成本高）。

### 5.2 地图实现（用户已选）

- **选中：自研配套 mod `inf_expedition`。** 复刻节点图 GUI、SavedData、房间 NBT 放置、AABB 保护、波次结算、Act 推进。
- 未选：FTB Quests 章节当节点图（快，但随机性/可达性/每玩家进度受限）；现有地牢 mod + KubeJS（没有节点地图与房间选择）。

### 5.3 首版范围（用户已选）

- **选中：1 个 Act 的纵向切片。** 大厅 + 节点地图 + 8~10 个房间 + 1 个 Boss + 锻造接入 + 任务书。验证完整一轮后再扩 Act 2/3。

### 5.4 打包方式（实现者决定）

- **选中：CurseForge 格式 zip（`manifest.json` + `overrides/`），由 `tools/build_pack.py` 生成。** 与参考包一致，国内启动器兼容性好。
- 不选 packwiz / `.mrpack` 作为 v0.1 主格式；可在后续加导出。
- 第三方模组不放进仓库；`pack/mods.yaml` 保存 project/file id 与下载地址，构建脚本在本地缓存存在时组装可玩目录，否则生成清单并列出缺失项。

### 5.5 房间 NBT 生成（实现者决定）

- **选中：`tools/gen_rooms.py` 用 Python 标准库生成结构 NBT。** 房间用紧凑 DSL 描述（尺寸、地板/墙/装饰、标记点），可重复生成、可 diff。
- 不选：全部在游戏里用结构方块手搭（慢、不可 review）；直接复用参考包 NBT（版权与格式风险）。

## 6. 总体结构

```
┌─────────────────────────────────────────────────────────────┐
│ 整合包 / Pack（CurseForge zip，overrides）                    │
│  ├─ mods: Inf.jar + inf_expedition.jar + 第三方模组           │
│  ├─ config: inf, inf_expedition, ftbquests, fancymenu, ...   │
│  ├─ kubejs: 起始包、商店、休息、事件、难度缩放、提示            │
│  ├─ ftbquests: 《锻途指南》章节                               │
│  └─ fancymenu: 标题背景、加载提示                             │
└───────────────┬─────────────────────────────┬───────────────┘
                │                             │
      ┌─────────▼─────────┐         ┌─────────▼─────────────┐
      │ Inf（既有模组）     │         │ inf_expedition（新）    │
      │ 锻刀台 / 五武器     │         │ 节点地图 / 房间 / 波次   │
      │ 四结果 / 六阶位     │         │ 区域保护 / Act / 大厅    │
      └───────────────────┘         └─────────┬─────────────┘
                                              │ 数据包
                                   ┌──────────▼──────────────┐
                                   │ rooms/*.json + *.nbt     │
                                   │ encounters/*.json        │
                                   │ loot_tables/*.json       │
                                   │ dimensions/*.json        │
                                   └─────────────────────────┘
```

### 6.1 仓库结构

```
Inf-Expedition-Pack/
├── src/...                         # inf_expedition mod
├── build.gradle / settings.gradle
├── inf-core/                       # Git submodule, pinned Inf v0.2.0
├── pack/                           # mod list, overrides and lock file
├── tools/                          # NBT, assets and pack builders
└── docs/                           # design, roadmap and acceptance records
```

The root Gradle project builds `inf_expedition`; `inf-core` is built separately and
its jar is consumed through `inf-core/build/libs/`. Third-party jars stay outside Git.

## 7. 世界、维度与大厅

### 7.1 维度

| 维度 | 用途 | 生成器 | 关键设置 |
|---|---|---|---|
| `inf_expedition:hub` | 出发大厅 | flat：1 层基岩 + 空气 | 固定时间 6000，环境光 0.2，the_void，无自然结构 |
| `inf_expedition:run` | 远征房间 | flat：1 层基岩 + 空气 | 固定时间 6000，the_void，y=1 起放置房间 |

- 两个维度都禁止自然刷怪：服务端启动时设置 `doMobSpawning=false`，遭遇战全部由 mod 手动生成。
- 掉出世界：在 run 维度 y < -16 时传送回当前房间的 spawn；hub 同理回大厅中心。
- 玩家登录/重生：默认传送到 hub 中心；首次进入时放置大厅平台。若玩家属于进行中的 run 队伍（见 7.4），则传送到当前房间 spawn。

### 7.2 大厅

大厅是一个小型的「远征前厅」，由 `hub_platform.nbt` 生成，包含：

- 一个「远征祭坛」：右键开始一局。
- 一个任务书展示架（右键领取 FTB Quests 任务书；任务书也在起始包里）。
- 返回点/出生点。
- **没有锻刀台、没有末影箱、没有可带进 run 的容器**：v0.1 不做跨局物品仓库。锻造只发生在起始房间与锻造房间。
- 返回卷轴（`inf_expedition:recall_scroll`）在 run 维度右键使用：把该玩家移出本局并传回大厅；若队伍还有其他成员，run 继续；若没有，run 结束（视为放弃）。单人时即结束本局。
- 大厅平台只在首次创建时放置；后续进入不重复生成。

### 7.3 局（Run）状态

`RunState` 保存在主世界 `SavedData`：

```
seed, act, floor, partyNodeId, visited, cleared, roomInstanceId,
runStartTime, players, status (IDLE / RUNNING / COMPLETE / FAILED)
```

- v0.1 同时只有一支 run 队伍；队伍成员在开局时确定（单人通常为 1 人）。
- 开新局：清空旧房间、重置地图、清空玩家背包、给起始包、传送到 START 房间。
- 通关 Act 1：`status=COMPLETE`，结算 clears +1，3 秒后回大厅并清空背包。
- 全队死亡：`status=FAILED`，回大厅并清空背包；clears 不变。

### 7.4 队伍生命周期（v0.1 规则）

- 开局时在线且非旁观的玩家成为队伍成员；单人即一人。大厅里的其他玩家不会自动加入进行中的 run。
- 进行中登录/重生：若玩家在队伍内，传送到当前房间 spawn；否则传送到大厅。
- 返回卷轴：只移除使用者；若队伍仍有成员，run 继续；最后一人离开则 run 结束。
- 服务器重启/存档重载时若 `status=RUNNING`：v0.1 直接判定为放弃，队伍回大厅，避免房间实例与区块状态悬空。
- 人数缩放只统计当前在线的队伍成员。

## 8. 节点地图

### 8.1 地图数据

- 节点图在服务端生成，按 run seed 决定；同一 seed 可复现。
- 配置来自数据包 `data/inf_expedition/expedition/map.json`：

```json
{
  "acts": 1,
  "floors": 8,
  "columns": 3,
  "paths": 2,
  "elite_min_floor": 3,
  "shop_min_floor": 2,
  "forge_min_floor": 2,
  "boss_floor": 8,
  "weights": {
    "battle": 40, "event": 20, "forge": 14,
    "rest": 12, "shop": 8, "elite": 6
  },
  "guarantee_rest_before_boss": true
}
```

- 生成算法：Slay-the-Spire 风格。每条路径从 floor 0 均匀分布的列出发，逐层向相邻列随机游走；相邻层的节点按列差 ≤1 连接；合并重复节点；保证起点/终点唯一、每个节点至少一条入边和出边。Boss 层只有 BOSS 节点；Boss 前一层若配置保证休息，则把其中一个节点设为 REST。
- 节点类型在生成连接后填充；权重过滤 `min_floor`；同一层避免重复 FORGE/SHOP/REST。
- 节点字段：`id, floor, column, type, connections, visited, revealed, completed, locked`。

### 8.2 地图界面

- 按 `M` 打开，ESC 关闭；地图打开时不暂停游戏（服务端权威）。
- 画面：全屏暗色背景 + 中央节点图；顶部显示 Act / 层数 / 队伍状态；右侧图例。
- 节点：24×24 图标（battle/elite/boss/shop/rest/event/forge/start），连线，玩家当前位置高亮，可达节点发光，已访问打勾，锁定节点变暗。
- 悬停显示类型名与一句说明；点击可达节点 → 发送 `MoveRequest`。
- 房间未完成时地图可查看但不能移动（显示「先完成当前房间」）。
- v0.1 不做拖拽/缩放；8 层 × 3 列适配屏幕。后续可加。
- 多人：地图显示队伍共享位置；任何队员点击都移动全队。v0.1 不做每玩家独立位置。

### 8.3 命令

`/infexpedition start|reset|map|move <nodeId>|complete|info|nextact`
权限等级 2；`info` 输出 seed/act/节点/房间状态，便于调试。KubeJS 与构建脚本用这些命令做冒烟测试。

## 9. 房间系统

### 9.1 房间清单

房间定义放在数据包 `data/inf_expedition/expedition/rooms/<name>.json`：

```json
{
  "type": "battle",
  "weight": 10,
  "structure": "inf_expedition:rooms/battle/crypt",
  "encounter": "inf_expedition:battle/crypt",
  "min_act": 1,
  "max_act": 1,
  "title": "room.inf_expedition.crypt"
}
```

- `type` 与节点类型一一对应：`battle / elite / boss / shop / rest / event / forge / start`。
- `structure` 是结构模板 ID，NBT 位于 `data/<namespace>/structure/...nbt`。
- `encounter` 可选；非战斗房间可以没有。
- `weight` 在同类型内加权随机；`min_act/max_act` 控制出现范围。

### 9.2 NBT 标记

房间 NBT 用 `minecraft:jigsaw` 方块做标记；放置时读取并替换为空气：

| 标记名 | 含义 |
|---|---|
| `spawn` | 玩家出生点，按玩家数轮流使用 |
| `mob` | 遭遇战刷怪点；没有则围绕房间中心生成 |
| `boss` | Boss 生成点 |
| `reward` | 奖励箱位置；房间清空后生成箱子 |
| `exit` | 离开/继续点；放置磁石 + 提示文本 |
| `shop` | 商人生成点 |
| `rest` | 休息交互点（营火/床） |

- 结构原点放在房间中心；mod 读取模板尺寸后按配置偏移放置（默认 X/Z 居中，Y=1）。
- 房间 AABB = 模板尺寸向四周扩 1 格；用于保护、清空、刷怪限制。

### 9.3 进入房间

1. 校验地图可达、当前房间已完成。
2. 清空旧房间：按 AABB 分批设为空气（每 tick 一批），移除带房间标签的实体。
3. 从 `rooms` 清单按节点类型 + 权重抽一个房间；`StructureTemplateManager` 加载 NBT。
4. 读取 jigsaw 标记，放置结构；把标记替换为空气。
5. 创建 `RoomInstance`（UUID、nodeId、type、bounds、markers、encounter），写入 SavedData。
6. 全队传送到 `spawn` 标记；给 1 秒无敌/缓慢下落，防止卡墙。
7. 触发 `RoomEnteredEvent`（NeoForge 事件），KubeJS/其他 mod 可监听。
8. 若有 encounter，延迟 2 秒开始第一波；否则直接解锁 exit。

### 9.4 保护与规则

- 禁止在房间 AABB 内破坏/放置方块（创造模式除外）；白名单：`inf:forge_table` 的放置、`minecraft:torch`？v0.1 只允许 forge table。
- 禁止爆炸（`ExplosionEvent.Start` 取消）。
- 禁止生物破坏（`EntityMobGriefingEvent` DENY）。
- 每 20 tick 把带房间标签、跑出 AABB 的怪拉回中心。
- 带 `room_mob` 标签的怪之间不互相索敌/伤害（避免波次内斗）。
- 房间清空后不自动清除方块；等下一次进入时统一清理。

### 9.5 遭遇战

`data/inf_expedition/expedition/encounters/<name>.json`：

```json
{
  "title": "encounter.inf_expedition.crypt",
  "boss": false,
  "waves": [
    {"mobs": [{"id": "minecraft:zombie", "count": 4}]},
    {"mobs": [{"id": "minecraft:skeleton", "count": 3},
              {"id": "minecraft:zombie", "count": 2}]}
  ],
  "scale": {
    "health_per_act": 0.25,
    "damage_per_act": 0.15,
    "count_per_act": 0.2,
    "health_per_extra_player": 0.5,
    "damage_per_extra_player": 0.25
  },
  "reward_loot": "inf_expedition:chests/battle_common"
}
```

- 每波从 `mob` 标记中随机取点；数量按 act 与队伍人数缩放，封顶。
- 属性缩放用命名空间属性修饰符（`inf_expedition:act_scale`），避免污染基础值。
- 当前波全部死亡 → 3 秒后下一波；全部波次完成 → 房间完成。
- Boss：单波 + `ServerBossEvent` 血条；Boss 死亡 → 房间完成 + 通关流程。
- 完成：在 `reward` 标记生成箱子并填入战利品表；播放音效；聊天栏提示；解锁 exit。
- 玩家死亡：若还有其他存活队员，死亡玩家变旁观，房间完成/进入下一房间时复活；若全队死亡，run 失败。

### 9.6 非战斗房间

| 类型 | 行为 |
|---|---|
| `start` | 起始房间；有锻刀台、起始箱、exit；进入即完成，可直接离开 |
| `forge` | 锻刀台 + 工具箱（锤子/刀胚）；KubeJS 给一条锻造提示；exit 可随时使用 |
| `rest` | 营火/床；右键营火恢复 50% 生命一次；exit 可随时使用 |
| `shop` | KubeJS 在 `shop` 标记生成商人（村民），用绿宝石交易刀胚/锤子/食物/药水 |
| `event` | 宝箱 + 可选伏击；开箱触发遭遇或直接给奖励；exit 在事件结束后解锁 |
| `boss` | Boss 竞技场；Boss 死亡即完成 Act |

- 非战斗房间的 exit 放置 `minecraft:lodestone`，右键触发 `RoomExitEvent`；战斗房间在清空后才放置/激活。
- 房间内聊天提示：「右键磁石离开房间」。

## 10. Inf 核心整合

### 10.1 一局内的锻造

- 起始包：1 把锻锤 + 2 个随机刀胚 + 8 个熟牛肉 + 1 个返回卷轴 + 1 本 FTB Quests 任务书。
- 起始房间与每个 forge 房间都有 `inf:forge_table`；玩家可随时锻造。
- 战斗奖励包含：额外刀胚、锻锤、食物、药水、绿宝石（商店货币）。
- 武器与物品都不跨局保留：开局与回大厅时都会清空背包；v0.1 没有跨局物品仓库，只有 `clears` 元进度。
- 一局内锻造出的武器直接用于战斗；高价值武器是通关 Boss 的关键。

### 10.2 元进度（v0.1 最小）

- 每次通关 Act 1，**每个队伍成员**的 `clears` +1（玩家持久数据 + 同名 scoreboard，供 FTB Quests 读取）。单人时即该玩家；联机时人人 +1。
- clears 影响起始包：
  - 1 次通关：起始包 +1 刀胚。
  - 3 次通关：起始包 +1 锻锤。
  - 5 次通关：起始包额外 1 瓶治疗药水。
- 大厅任务书与起始包任务书显示 clears 与下一档奖励。
- 失败不扣 clears；v0.1 不做难度技能树。

### 10.3 不修改 Inf 的边界

- v0.1 不新增 Inf 物品、不改 Inf 配方、不改 Inf 数值；只使用其现有物品/方块。
- 通过 KubeJS 监听 `RoomEnteredEvent`、玩家 tick、物品使用来实现起始包、商店、休息、提示。
- 若后续需要「锻造增益/材质分级」，作为独立阶段再评估；不放进 v0.1。

## 11. 整合包内容

### 11.1 模组清单（v0.1 目标 ~30 个）

必装（核心）：

| 类别 | 模组 | 说明 |
|---|---|---|
| 核心 | Inf（本地） | 锻刀 |
| 核心 | Inf Expedition（本地） | 地图/房间/Act |
| 脚本 | KubeJS（1.21.1 NeoForge） | 内容粘合 |
| 任务 | FTB Quests + FTB Library + FTB Teams（CurseForge） | 指南 |
| 物品 | JEI、Jade | 查询/信息 |
| 饰品 | Curios API | 后续遗物接口；v0.1 只作为依赖 |
| 属性 | Apothic Attributes + Placebo + AttributeFix | 属性显示/兼容 |
| 储物 | Sophisticated Backpacks + Sophisticated Core | 背包 |
| 食物 | Farmer's Delight | 休息/食物 |
| 战斗内容 | Mowzie's Mobs | Act 1 Boss/精英 |
| 战斗内容 | Legendary Monsters | 普通/精英怪 |
| 战斗内容 | L_Ender's Cataclysm | 后续 Act 内容；v0.1 只用少量怪 |
| 性能/客户端 | ModernFix、FerriteCore、Clumps、Embeddium | 稳定性 |
| 客户端 UI | FancyMenu + Konkrete + Melody | 标题/加载 |
| 客户端 QoL | Xaero's Minimap、Controlling、Searchables、Mouse Tweaks、Inventory Profiles Next + libIPN、Inventory HUD+ | 体验 |
| 库 | GeckoLib、Architectury API、Balm、Cloth Config、Lionfish API | 依赖 |

可选（用户确认后再加）：`Tips`、`Drippy Loading Screen`、`Oculus`、`Lootr`、`Waystones`、`Pufferfish's Skills`、`Lightman's Currency`、`Iron's Spells 'n Spellbooks`、`Bosses' Rise`、`EEEAB's Mobs`、`When Dungeons Arise`、`Dimensional Dungeons`。

- 版本必须锁定在 `pack/mods.yaml`；实现阶段逐个验证 1.21.1 NeoForge 可用性。已知 Alex's Caves 无 1.21.1 版本，不列入。
- FTB Quests 走 CurseForge；其余优先 Modrinth 下载。

### 11.2 配置

- `config/inf-common.toml`：沿用默认数值；若测试发现 Act 1 节奏过慢，只调 `baseGain`/`riskPerStrike`，并记录理由。
- `config/inf_expedition-common.toml`：地图层数/列/路径、房间中心、保护开关、队伍模式、死亡清背包、缩放系数。
- 服务端 gamerules（KubeJS 启动时设置）：`doMobSpawning=false`、`keepInventory=true`、`mobGriefing=false`、`doFireTick=false`、`naturalRegeneration=true`。
- `config/ftbquests`、`config/fancymenu`、`config/jade`、`config/xaero` 等按包调整。

### 11.3 KubeJS 脚本

- `server_scripts/expedition/start_kit.js`：开新局/登录时给起始包；按 clears 调整。
- `server_scripts/expedition/shop.js`：监听 `RoomEnteredEvent`，在 shop 房间生成商人并设置交易。
- `server_scripts/expedition/rest.js`：休息点右键回血（每房间一次）。
- `server_scripts/expedition/event.js`：事件房间宝箱/伏击逻辑。
- `server_scripts/expedition/rules.js`：gamerules、清怪、提示、难度缩放兜底。
- `server_scripts/expedition/boss.js`：Boss 通关/失败消息、clears 统计、元奖励。
- `client_scripts/expedition/tooltips.js`：地图按键提示、房间提示。
- 所有脚本只调用 Inf/Expedition 的公开行为，不直接改 NBT 内部结构。

### 11.4 FTB Quests

《锻途指南》一章，12~15 个任务：

1. 领取起始包。
2. 打开地图（M）。
3. 锻造第一把武器。
4. 锻出精良/稀有。
5. 清空第一个战斗房间。
6. 使用锻造房间。
7. 在商店买到刀胚。
8. 在休息点回血。
9. 击败精英。
10. 击败 Act 1 Boss。
11. 完成一次远征（回大厅）。
12. 累计通关 3 次。

奖励：经验、食物、额外刀胚、绿宝石；不给直接影响 Boss 战的装备。FTB Quests 设为默认自动领取，奖励在 run 内立即进背包；任务进度按玩家持久化，不随清包丢失。

### 11.5 界面与美术

- FancyMenu：暗色锻炉主题标题背景 + logo；加载提示复用 Inf 的锻造文案。
- 地图贴图：节点图标、背景、玩家标记、连线；由 `tools/gen_assets.py` 扩展生成占位图，后续可替换。
- 房间美术：v0.1 用原版方块（深板岩、黑石、橡木、营火、铁栏杆）搭出可辨识的锻炉/墓穴/营地风格；不做自定义模型。
- 语言：`zh_cn` 为默认，`en_us` 同步；所有房间/节点/遭遇文本进 lang 文件。

## 12. 纵向切片（v0.1）内容清单

### 12.1 房间

| # | 类型 | NBT | 遭遇 | 说明 |
|---|---|---|---|---|
| 1 | start | `start/camp` | 无 | 锻刀台、起始箱、磁石出口 |
| 2 | battle | `battle/crypt` | 2 波（僵尸/骷髅） | 墓穴 |
| 3 | battle | `battle/forge_hall` | 2 波（掠夺者/卫道士） | 废弃锻炉 |
| 4 | battle | `battle/cavern` | 3 波（蜘蛛/蠹虫） | 洞窟 |
| 5 | elite | `elite/arena` | 1 波精英 | 竞技场 |
| 6 | forge | `forge/smithy` | 无 | 锻刀台 + 工具箱 |
| 7 | rest | `rest/camp` | 无 | 营火回血 |
| 8 | shop | `shop/stall` | 无 | 商人 |
| 9 | event | `event/treasure` | 可选伏击 | 宝箱 |
| 10 | boss | `boss/wroughtnaut` | 1 个 Boss | Act 1 Boss |

- Boss 首选 Mowzie's Mobs 的 Ferrous Wroughtnaut；若 1.21.1 版本数值不适合，替换为 Legendary Monsters 的 Act 1 级 Boss，并在计划中记录选择。
- 地图层数 8：floor 1 START，floor 2–6 内容，floor 7 保证 REST，floor 8 BOSS。

### 12.2 验收标准

1. 新建世界 → 自动到大厅 → 右键祭坛开始 → 地图可打开、可点击可达节点。
2. 进入每个节点都能正确放置对应类型房间；旧房间被清空，实体不残留。
3. 战斗房间：波次生成、怪物不内斗、跑出区域被拉回、清空后给奖励并解锁出口。
4. 非战斗房间：锻造/休息/商店/事件/出口都可用。
5. Boss 房间：Boss 血条、死亡通关、回大厅、clears +1；下一次地图不同。
6. 死亡：单人失败 → 回大厅、背包清空；若启用双人联机，一人死亡变旁观、队友清完房间后复活。
7. 锻刀台在大厅与锻造房间可用；一局内锻造的武器能正常参与战斗。
8. 任务书章节可见、任务可完成；中英文文本齐全。
9. 整合包能启动，无缺失依赖；加载约 30 个模组稳定运行 30 分钟。
10. `./gradlew test`（Inf）与 `expedition/gradlew test` 全绿；地图生成器有确定性/连通性单测。

## 13. 测试与验证

### 13.1 单元测试

- `MapGeneratorTest`：同 seed 输出一致；每个非起点节点有入边；起点/终点唯一；Boss 层只有一个 BOSS；保证休息规则生效；无孤立节点。
- `RoomCatalogTest`：JSON 解析、权重过滤、类型映射。
- `ScalingTest`：Act/人数缩放公式、封顶、取整。
- `MarkerTest`：从模板读取 jigsaw 标记、缺失标记回退。

### 13.2 游戏内测试

- `runServer` + `/infexpedition start` + `/infexpedition move` + `/infexpedition complete` 冒烟。
- GameTest（可行时）：放置小房间、验证 AABB 保护、清空、exit 交互。
- 自动客户端冒烟：`xvfb-run -a ./gradlew runClient`（Mesa llvmpipe 已确认可用）；能到主菜单/进世界即算通过，不评估帧率。
- 手动客户端（由用户或带显示环境执行）：完整跑一轮 Act 1；死亡/失败/通关三条路径；双人 LAN 仅 P6 可选。

### 13.3 整合包验证

- `tools/build_pack.py --check`：校验 `mods.yaml`、本地 jar 存在、overrides 路径、manifest 版本范围。
- 用启动器（HMCL/Prism）导入生成的 zip，启动到主菜单，进世界跑验收标准 1–4。
- 记录 `logs/latest.log` 中的缺失依赖与 mixin 冲突。

### 13.4 本机图形环境结论

- `DISPLAY=:0`、`xvfb-run`、Mesa llvmpipe（OpenGL 4.5）可用；可以跑无头客户端冒烟和截图。
- 未发现 Prism/HMCL 启动器；整合包完整测试优先用 `pack/export` + 用户启动器，或后续加一个 `pack-test` 运行配置。
- 所有 `runClient` 验证都加 `xvfb-run -a`，避免占用真实显示。

## 14. 风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| 自定义地图 GUI/网络层在 1.21.1 的 API 细节多 | 高 | 先做技术 spike：SavedData + MapScreen + 一个节点移动；通过后再做房间系统 |
| 1.21.1 模组可用性与版本冲突 | 高 | 先锁定核心 15 个模组的精确版本并启动空包；内容模组逐个加 |
| 参考包的自定义能力（Gateways/ChatBox/Merchant）不可用 | 中 | 地图 mod 自带波次；商店用村民交易；对话用聊天/标题 |
| NBT 房间生成格式错误 | 中 | Python 生成器带自检：尺寸/palette/标记数量；先在超平坦测试放置 |
| Boss 数值不适合 Inf 武器 | 中 | Boss 选择可替换；遭遇 JSON 可调血量/伤害；先做 3 档配置测试 |
| 多人状态同步/死亡复活复杂 | 中 | v0.1 单队伍共享位置；死亡规则先做单人，再做双人；用 SavedData 单一事实源 |
| 网络受限导致模组无法下载 | 中 | `mods.yaml` 保留 project/file id；构建脚本支持本地 `cache/`；用户可手动放置 jar |
| 版权/分发 | 中 | 不复制参考包资源；第三方模组不打包进仓库；发布时附 credits |

## 15. 实施阶段（供计划使用）

| 阶段 | 内容 | 交付 |
|---|---|---|
| P0 | 锁定模组版本、空包启动、spike 地图屏 | 可启动空包 + 地图屏截图 |
| P1 | `inf_expedition` 核心：维度、SavedData、地图生成、命令、房间放置、保护 | 服务端可跑通 `/infexpedition start/move/complete` |
| P2 | 遭遇战/奖励/出口/死亡/Act 结算 | 单人可完整跑 Act 1（无美术） |
| P3 | 房间 NBT + 房间 JSON + 10 个房间 | 10 个房间可进入/清空 |
| P4 | 大厅、起始包、商店、休息、事件、元进度 | 大厅→远征→回大厅闭环 |
| P5 | 任务书、FancyMenu、地图贴图、中英文文本 | 可发布纵向切片 |
| P6 | 双人验证、平衡、打包、验收 | `inf-expedition-0.1.0.zip` |

## 16. 留给实现者的决定

- 地图生成的具体随机游走参数、节点权重微调。
- Boss 最终选择（Wroughtnaut 或 Legendary Monsters 的某个 Boss），以实测为准。
- 房间 NBT 的具体尺寸与装饰；只要满足标记约定即可。
- KubeJS 商店交易表、休息回血数值、起始包具体物品数量。
- FancyMenu 标题图的最终美术；v0.1 允许占位图。
- 是否在 v0.1 加 `Lootr`/`Waystones` 等可选模组；以稳定性和下载可行性为准。

## 17. 已确认的产品选择

1. **名称**：《锻途：千锤之路》，英文暂定 `Inf: Thousand Hammers`。
2. **联机**：v0.1 单人优先；地图/房间/死亡状态按队伍共享设计，双人 LAN 放到 P6 作为可选验证，不作为 v0.1 验收阻塞项。
3. **死亡/通关**：回大厅时清空本局背包；v0.1 没有跨局物品仓库，只有 `clears` 元进度保留。这是肉鸽循环的一部分，不提供默认关闭选项（可在配置中保留开关，但默认开启）。

## 18. 运行状态的所有权边界

为避免 mod 与 KubeJS 各管一半导致状态分裂，v0.1 明确：

- `inf_expedition` 拥有：run 状态、地图位置、房间实例、玩家传送、开局清背包/发基础起始包、死亡/通关判定、回大厅。
- KubeJS 拥有：起始包的**追加**内容（按 clears 加刀胚/锤子/药水）、商店交易、休息回血、事件房间脚本、提示语、任务联动。
- mod 在关键节点发出 NeoForge 事件：`RunStartedEvent`、`RoomEnteredEvent`、`RoomCompletedEvent`、`RunEndedEvent`（成功/失败）。KubeJS 通过 `NativeEvents.onEvent` 监听；如果某个事件在 KubeJS 侧不可用，退化为 mod 写入 scoreboard/persistentData，KubeJS 轮询。
- 所有影响通关的判定（节点可达、房间完成、Boss 死亡、全队死亡）只在服务端执行；客户端只画 UI 和发请求。

## 19. 数据包与内容位置

- v0.1 的房间 NBT、房间 JSON、遭遇 JSON、战利品表放在整合包 `overrides/kubejs/data/inf_expedition/...`，由 KubeJS 数据包在服务端加载；这与参考包用 KubeJS data 目录放 structure 的做法一致。
- `inf_expedition` 自带一份最小内置数据（大厅平台、一个测试房间、维度 JSON），保证不装整合包也能启动和自测。
- 若 KubeJS 数据包在 reload 顺序上不可见，回退方案：把 v0.1 内容移入 `expedition/src/main/resources/data/inf_expedition/...`，重新构建 jar；计划中保留这个切换点。

## 20. 全部 Act 的目标参数（v0.1 之后）

参考包的默认地图是 15 层 × 6 条路径 × 7 列。v0.1 用 8 层 × 2 条路径 × 3 列做纵向切片；扩展到 3 Act 时回到 15/6/7，并按 Act 调整权重和 Boss 层。v0.1 的生成器参数必须从第一天就按可配置实现，不能写死 8/2/3。

## 21. 已验证的 1.21.1 NeoForge 模组（2026-11-16）

以下版本在 Modrinth/CurseForge 上确认存在 1.21.1 NeoForge 构建；最终以 `pack/mods.yaml` 锁定为准：

| 模组 | 版本/来源 |
|---|---|
| KubeJS | `2101.7.2-build.381`（Modrinth） |
| FTB Quests | CurseForge，1.21.1 NeoForge；随 FTB Library / FTB Teams 锁定 |
| JEI | `19.70.0.469` |
| Jade | `15.10.6+neoforge` |
| Curios API | `9.5.1+1.21.1` |
| Lootr | `1.21.1-1.11.38.127` |
| Waystones | `21.1.47+neoforge-1.21.1` |
| Apothic Attributes | `1.21.1-2.11.0` |
| Sophisticated Backpacks / Core | `1.21.1-3.26.21.2249` / `1.21.1-1.5.37.2482` |
| Farmer's Delight | `1.21.1-1.3.4` |
| Mowzie's Mobs | `1.8.2` |
| Legendary Monsters | `2.2.4` |
| L_Ender's Cataclysm | `3.33` |
| Bosses'Rise | `2.1.2` |
| Aquamirae | `7.2.13` |
| ModernFix | `5.27.27+mc1.21.1` |
| FerriteCore | `7.0.3-neoforge` |
| Embeddium | `1.0.15+mc1.21.1` |
| Clumps | `19.0.0.1` |
| GeckoLib | `4.9.3` |
| Architectury API | `13.0.12+neoforge` |
| Balm | `21.0.67+neoforge-1.21.1` |
| Cloth Config | `15.0.140+neoforge` |
| Lionfish API | `3.1` |
| Xaero's Minimap | `neoforge-1.21.1-26.6.0` |
| Controlling | `19.0.5` |
| Searchables | `1.0.2` |
| Mouse Tweaks | `1.21-2.26.1-neoforge` |
| FancyMenu | `3.10.0-1.21.1-neoforge` |
| Konkrete | `1.9.9-1.21-neoforge` |
| Melody | `1.0.10-1.21-neoforge` |
| Inventory Profiles Next | `neoforge-1.21.1-2.2.5` |
| Tips | `21.1.3` |
| Drippy Loading Screen | `3.1.5-1.21.1-neoforge` |
| Lightman's Currency | `1.21-2.3.0.6` |
| When Dungeons Arise | `2.1.68`（可选） |
| Dimensional Dungeons | `203`（可选） |
| Iron's Spells 'n Spellbooks | `1.21.1-3.16.3`（可选） |

**明确排除**：Alex's Caves（无 1.21.1 构建）、EEEAB's Mobs 主项目（无 1.21.1 构建）、Tetra 及其 addon（无 1.21.1 构建）。

## 22. 版权与分发

- 参考包只用于行为研究；不复制其 KubeJS 脚本、NBT 房间、贴图、FancyMenu 配置或自定义 jar。
- 自研 `inf_expedition` 与整合包配置/脚本/美术由本仓库产出；`Inf` 的许可证按仓库现有 `All Rights Reserved` 执行，发布前由仓库所有者确认。
- 第三方模组不提交进仓库；`pack/mods.yaml` 只保存 project/file id 与官方下载地址。发布 zip 时附 `CREDITS.md`，列出所有模组与作者。
- 参考包名称、作者名不用于宣传本包。
