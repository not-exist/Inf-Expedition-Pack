# 锻途：千锤之路 — 从 v0.1 到 v1.0 完整路线图

- 独立仓库：`not-exist/Inf-Expedition-Pack`；Inf 核心固定为 `inf-core` 子模块 tag `v0.2.0`
- 当前状态：源仓库 v0.1 代码、整合包构建和部分真机验收完成；独立仓库正在做 clean-clone 复验。
- 关联文档：
  - 设计：`docs/superpowers/specs/2026-11-16-inf-expedition-pack-design.md`
  - v0.1 实施计划：`docs/superpowers/plans/2026-11-16-inf-expedition-pack.md`
  - 集成记录：`docs/pack/integration-log.md`
  - 发布清单：`docs/pack/release-checklist.md`

---

## 0. 执行更新（2026-11-16）

- 用户已确认：**Tier S + A + B 全部一次性加入**。
- `pack/mods.yaml` 已从 39 个条目扩展到 76 个条目（75 required，1 optional）；76 个 jar 已缓存，客户端/专用服务端子集均已启动验证。
- Artifacts 战利品、Pufferfish 难度技能树、Act 1 外部怪物遭遇已接入；Easy NPC 预设、装饰 palette、Dimensional Dungeons 事件房间仍需运行时/手动步骤。`.mrpack` 与本地可玩目录已生成。详见 `docs/pack/external-mod-status.md` 与 `docs/pack/mod-matrix.md`。

## 1. 目标与边界

### 1.1 最终目标

一个 **以 Inf 锻刀为核心、节点地图 + NBT 房间 + 多 Act 肉鸽** 的可分发整合包：

- 玩家从大厅出发，在节点地图上选择路线；
- 房间提供战斗、精英、锻造、休息、商店、事件；
- 每局锻造的武器决定能否击败当前 Act 的 Boss；
- 通关/失败后回大厅，保留 `clears` 元进度；
- 3 个 Act，每 Act 有独立 Boss 与房间池；
- 外部模组只服务于上述循环，不取代 Inf 的锻造核心。

### 1.2 非目标

- 不做开放世界生存整合包；不做主线以外的超大内容包。
- 不加入会重写 Inf 武器数值/攻击判定的模组，除非实测证明兼容。
- 不在 v1.0 前做联机专用服务器产品化。

### 1.3 当前 v0.1 已完成

- `inf_expedition`：确定性节点地图、SavedData、10 个 NBT 房间、区域保护、遭遇波次、Boss 血条/奖励、地图屏、命令、大厅、返回卷轴、离线清包、重启恢复。
- 内容：start/battle×3/elite/forge/rest/shop/event/boss，10 个房间；12 个 FTB Quests 任务；KubeJS 商店/休息/规则/clears；标题/地图贴图。
- 验证：Inf 40 测试 + expedition 35 测试；Python 自检；服务端 RCON `catalog=10 floors=8`，4 个结构 `Placed: true`；客户端加载 `mod/inf` + `mod/inf_expedition`。
- 打包：`tools/build_pack.py` 支持 `--check/--local/--zip`；`--zip` 已对未锁定 CurseForge id 做门禁。

### 1.4 当前阻塞

1. `pack/mods.yaml` 中 39 个第三方条目未锁定 CurseForge `project_id/file_id`。
2. 本机没有第三方 jar（`pack/cache/` 为空），也没有 Prism/HMCL 启动器。
3. KubeJS/FTB Quests/FancyMenu 未在真实整合包运行时验证。
4. Act 1 手动验收、平衡、死亡/重连/双人尚未执行。

---

## 2. 最终模组组合

### 2.1 核心必装（不参与玩法替换）

| 模组 | 1.21.1 NeoForge 版本 | 作用 | 备注 |
|---|---|---|---|
| Inf | 本仓库 `0.2.0` | 锻刀核心 | 不再扩展数值前先保持 |
| Inf Expedition | 本仓库 `0.1.0` | 地图/房间/Act | 与 Inf 解耦 |
| KubeJS | `2101.7.2-build.381` | 内容粘合 | 需要 KubeJS 2101 |
| FTB Quests / Library / Teams | CF 1.21.1 | 任务书 | 12 任务起步 |
| JEI | `19.70.0.469` | 查询 | |
| Jade | `15.10.6+neoforge` | 信息 | |
| Curios API | `9.5.1+1.21.1` | 遗物/饰品基础 | Artifacts 依赖候选 |
| Apothic Attributes / Placebo | `1.21.1-2.11.0` | 属性兼容 | |
| Lootr | `1.21.1-1.11.38.127` | 每玩家战利品 | 房间箱子 |
| Waystones | `21.1.47+neoforge-1.21.1` | 传送/回城辅助 | 可选但推荐 |
| Sophisticated Backpacks / Core | `3.26.21.2249` / `1.5.37.2482` | 背包 | |
| Farmer's Delight | `1.21.1-1.3.4` | 食物/休息 | |
| Mowzie's Mobs | `1.8.2` | Act 1 Boss/精英 | Wroughtnaut |
| Legendary Monsters | `2.2.4` | 普通/精英怪 | |
| L_Ender's Cataclysm | `3.33` | Act 2/3 Boss | |
| Bosses'Rise | `2.1.2` | Boss 变体 | |
| Aquamirae | `7.2.13` | 海洋/幽灵敌人 | |
| Bosses of Mass Destruction | `1.3.3` | Act 2/3 Boss | 新接入，先实测 |
| Mobs of Mythology | `3.1.0` | 怪物池 | 新接入 |
| Illager Invasion | `21.1.6` | 灾厄战斗房间 | 新接入，依赖待锁 |
| GeckoLib | `4.9.3` | 动画库 | 多个内容模组依赖 |
| Architectury API | `13.0.12+neoforge` | 库 | |
| Balm | `21.0.67+neoforge-1.21.1` | 库 | |
| Cloth Config | `15.0.140+neoforge` | 库 | |
| Lionfish API | `3.1` | Cataclysm 依赖 | |
| ModernFix / FerriteCore / Embeddium / Clumps | 已验证 | 性能 | Embeddium 客户端 |
| FancyMenu / Konkrete / Melody | `3.10.0` / `1.9.9` / `1.0.10` | 标题/加载 | 客户端 |
| Tips / Drippy Loading Screen | `21.1.3` / `3.1.5` | 加载提示 | 客户端 |
| Xaero's Minimap | `26.6.0` | 小地图 | 客户端 |
| Controlling / Searchables / Mouse Tweaks / IPN / Inventory HUD+ | 已验证/待锁 | QoL | 客户端 |

### 2.2 推荐新增（按优先级）

| 优先级 | 模组 | 版本 | 作用 | 代价/风险 |
|---|---|---|---|---|
| S | Artifacts | `13.2.5` | 遗物、房间奖励、Curios 构筑 | 需要 LootJS 接入；可能稀释 Inf 核心 |
| S | Easy NPC + Core + Config UI | `7.19.0`（依赖版本待锁） | 商人/休息/事件对话，替换 KubeJS 商店箱 | NPC/对话 schema 需要实测 |
| S | Pufferfish's Skills | `0.19.2` | 局外技能树/难度 | 与 `clears` 对接；避免与遭遇缩放重复 |
| S | Keystone Expansion | `1.1.0` | 技能树扩展 | 可后置 |
| A | LootJS | `1.21.1-3.7.0` | 战利品表脚本 | KubeJS 依赖 |
| A | MoreJS | `0.16.0` | KubeJS 交互/事件 | KubeJS 依赖 |
| A | Boss Checklist | `5.3.2` | Boss 图鉴 | 客户端 |
| A | Enhanced Boss Bars | `1.1.3` | Boss 血条 UI | 客户端 |
| A | Crimson Steve's Mutant Mobs | `1.6.0` | 精英变体 | 可选 |
| B | Dusty Decorations | `2.3.2` | 锻炉/港口/市场装饰 | 房间 NBT 需要新 palette |
| B | Fetzi's Asian Decoration | `1.8.2.1` | 东方锻炉/和风房间 | 与 Dusty 二选一 |
| B | XK's Decoration | `1.1.10` | 600+ 装饰方块 | 包体积/启动成本 |
| B | Dimensional Dungeons | `203` | 维度地牢事件房间 | 需要房间策略/桥接 |
| B | YUNG's Better Dungeons | `5.1.4` | 主世界地牢 | 不是节点房间核心 |
| B | When Dungeons Arise | `2.1.68` | 主世界大型结构 | 同上 |
| B | L2 Artifacts | `3.0.7` | 长线遗物 | 比 Artifacts 重 |
| C | Better Combat | `2.4.0` | 战斗手感 | 与 Inf 镰刀/匕首 reach/sweep 可能冲突，必须实测 |
| C | Grandia Dungeons | `0.3-beta` | 自带肉鸽层 | 与节点地图重叠，暂不接入 |
| C | Improved Mobs / Hostile Mobs Improve Over Time | — | 自动难度 | 与 EncounterScaler 重复，暂不接入 |

> 所有外部模组最终必须以 **CurseForge `project_id/file_id`** 进入 `pack/mods.yaml`；Modrinth 只用于版本核验。
> 未锁定依赖的条目（Easy NPC 依赖、Illager Invasion 的 Puzzles Lib、Bosses of Mass Destruction 的动画库等）在 M0 逐项解决。

---

## 3. 外部模组接入设计

### 3.1 Artifacts（房间遗物）

- 目的：让精英/Boss/事件房间的奖励从“材料”升级为“构筑选择”。
- 接入：
  - 新增 `pack/overrides/kubejs/server_scripts/expedition/loot_artifacts.js`，用 LootJS 往 `inf_expedition:chests/elite`、`boss`、`event` 追加 `artifacts:*` 随机条目；
  - 每局限制：精英 1 件、Boss 1 件，避免随机池失控；
  - 任务书新增“找到第一件遗物”“集齐 3 件”任务；
  - 遗物描述/来源进 `docs/pack/artifacts.md`。
- 验收：打开精英/Boss 箱能获得遗物；不会替代 Inf 武器；非 Curios 槽不冲突。

### 3.2 Easy NPC（商店/休息/事件）

- 目的：替换 `shop.js` 的“右键箱子买随机物品”，让商店、休息、事件有真正的对话。
- 接入：
  - 新增 NPC 预设：`shop_keeper`、`rest_attendant`、`event_guide`；
  - 房间 `RoomEnteredEvent` 时在 `shop`/`rest`/事件标记生成对应 NPC；
  - 对话/交易用 Easy NPC 的配置/数据文件；交易表用绿宝石换刀胚、锤子、食物、药水；
  - 可选 `EasyNPC x FTB Quests Compat` 将 NPC 步骤与任务绑定。
- 验收：商店 NPC 可交易；休息 NPC 可回血；事件 NPC 有分支；不会生成重复 NPC。

### 3.3 Pufferfish's Skills（元进度）

- 目的：把 `clears` 从“计数器”升级为可感知的局外成长。
- 接入：
  - `boss.js` 成功通关时解锁 `inf_expedition:difficulty_1..N` 技能；
  - 起始包根据技能等级增加刀胚/锤子/药水；
  - GUI/按键说明进任务书；
  - 与 `EncounterScaler` 的 Act 缩放分开：技能树只加玩家能力，不改怪物倍率。
- 验收：通关后技能点/解锁可见；下一局起始包变化；失败不扣。

### 3.4 怪物/Boss 内容池

- Act 1：Mowzie's Wroughtnaut + Legendary Monsters 精英 + Illager Invasion 灾厄。
- Act 2：Cataclysm 精英 + Bosses'Rise + Mobs of Mythology。
- Act 3：Cataclysm Boss + Bosses of Mass Destruction + Aquamirae。
- 每个 Act 至少 2 个 Boss 候选、6 个普通遭遇、3 个精英遭遇。
- 所有实体 id 进 `encounters.json` 前用 `/summon` 或注册表查询验证；冲突/缺失走 `EncounterFallback`。

### 3.5 装饰与房间模板

- 选 **Dusty Decorations** 作为主装饰包（锻炉/港口/市场），Fetzi 作为东方房 variant（二选一或仅小规模）。
- `tools/gen_rooms.py` 增加 `RoomPalette` 配置：base block set + decor block set；生成 3 套 Act 1 视觉变体（锻炉/墓穴/洞窟）。
- 新增房间必须保留 `spawn`/`exit` 标记；生成器自检会拒绝缺失。

### 3.6 事件房间扩展

- 短周期：宝箱伏击 / 献祭 / 赌徒转盘（KubeJS + Easy NPC 对话）。
- 中周期：Dimensional Dungeons 事件房间（需要桥接/传送策略；参考包同类）。
- 长周期：Act 2/3 专属事件，不在 v0.2 前做。

### 3.7 数据/脚本基础设施

- `pack/mods.yaml` 增加字段：
  ```yaml
  role: core | content | meta | client | library
  depends: [KubeJS, Curios API]
  required: true | false
  ```
- `tools/build_pack.py` 增加依赖存在性检查、`--check` 输出分组、`--zip` 门禁。
- 新增 `tools/verify_entity_ids.py`：从 encounters JSON 提取实体 id，在 pack 启动后通过日志/命令核对。
- 新增 `docs/pack/mod-matrix.md`：模组 × Act × 房间 × 依赖。

---

## 4. 版本路线图

### M0 — v0.1.0 可玩基线（当前阻塞）

**目标：** 把已有代码变成可启动、可完整跑一轮 Act 1 的整合包。

任务：
1. 锁定 39 个 CurseForge project/file id；把第三方 jar 放入 `pack/cache/<name>.jar`。
2. `python3 tools/build_pack.py --check` 零 error、零 warning。
3. `python3 tools/build_pack.py --local`，用 Prism/HMCL 启动，修复缺失依赖/mixin 冲突。
4. 跑 `docs/pack/release-checklist.md` 的 9 条验收。
5. 记录 Act 1 用时、死亡次数、最高阶位、Boss 尝试次数到 `docs/pack/act1-balance.md`。

退出标准：
- 包能启动到主菜单并进世界；
- 完整跑通 Act 1（开始→Boss→回大厅）；
- 无缺失依赖、无崩溃；
- 30 分钟稳定运行。

估算：2–4 人日（在 jar/启动器可用的前提下）。

### M1 — v0.2.0「遗物与商人」

**目标：** 用外部模组增强房间奖励和交互，不改 Inf 核心。

范围：
- Artifacts + LootJS 遗物掉落；
- Easy NPC 商店/休息/事件；
- Mobs of Mythology + Illager Invasion 进入遭遇池；
- Boss Checklist + Enhanced Boss Bars；
- 1 个装饰包（Dusty）用于 2–3 个房间。
- 任务书从 12 扩展到 18–20 个任务。

退出标准：
- 精英/Boss/事件箱能出遗物；
- 商店/休息由 NPC 完成；
- 新增 4 个普通/2 个精英遭遇；
- 现有 35 个单测 + 新增数据/脚本检查全绿；
- 手动跑 2 局 Act 1。

估算：4–6 人日。

### M2 — v0.3.0「锻途技能树」

**目标：** 把 `clears` 变成局外成长。

范围：
- Pufferfish's Skills + Keystone Expansion；
- 通关解锁难度技能/起始包升级；
- 任务书「难度」章节；
- 失败/重连/离线清包回归测试。

退出标准：
- 通关后可见技能解锁；
- 下一局起始包变化；
- 失败不扣进度；
- 2 名玩家各自获得 clears。

估算：3–5 人日。

### M3 — v0.5.0「三幕远征」

**目标：** 从 1 Act 扩到 3 Act。

范围：
- 地图参数回到 15 层 × 7 列 × 6 路径（参考包体量）；
- 每 Act 8–12 个房间，共 24–30 个 NBT 房间；
- Act 2/3 新房间调色板/装饰；
- 每 Act 2 个 Boss 候选；
- 新事件/商店/休息/锻造房；
- 任务书 35–45 个任务；
- 难度/奖励曲线。

退出标准：
- 完整 3 Act 通关；
- 每 Act 至少 3 个不同 Boss 组合；
- 失败/重连/重启路径；
- 单人 45–75 分钟一局；
- 双人 LAN 可选验证。

估算：8–12 人日。

### M4 — v1.0.0「发布」

**范围：**
- 50 个左右模组最终锁定；
- 完整中英文文本（FTB Quests 英文化另做）；
- FancyMenu 标题/加载/地图美术；
- CREDITS/许可证/第三方下载清单；
- 正式 CurseForge zip；
- 专用服务器 + 2 人 + 30 分钟稳定性；
- 发布页文案与已知问题。

退出标准：
- `build_pack.py --check/--zip` 零 error 零 warning；
- 全量测试 + 手动验收全绿；
- 发布 zip 可被启动器导入并启动；
- 无高危/中危复审发现。

估算：3–5 人日。

总估算（M0–M4）：20–32 人日，不含等待第三方 jar/网络/启动器的时间。

---

## 5. 工作流与任务拆分

### W1 — 模组锁定与包基线（M0）

- [ ] 逐个确认 CurseForge project/file id；写入 `pack/mods.yaml`。
- [ ] 建立 `pack/cache/` 命名规范：`<display name>.jar`。
- [ ] `build_pack.py` 增加 `role/depends` 校验。
- [ ] 启动核心 15 → 加内容 6 → 加 QoL/客户端；每步记录 `integration-log.md`。
- [ ] 版本锁定后生成 `docs/pack/mod-matrix.md`。

### W2 — Easy NPC 与交互（M1）

- [ ] 确认 Easy NPC Core/Config UI 版本与配置路径。
- [ ] 生成/导入 3 个 NPC 预设。
- [ ] 房间进入时生成 NPC；退出/完成时清理。
- [ ] 交易表：绿宝石 → 刀胚/锤子/食物/药水。
- [ ] 休息 NPC 回血每房间一次。
- [ ] 事件 NPC 对话分支（至少 2 个事件）。
- [ ] 与 FTB Quests 兼容（可选）。
- [ ] 测试：重复进入、多人、房间完成清理、NPC 不重复。

### W3 — Artifacts 与战利品（M1）

- [ ] 加入 Artifacts + Curios 依赖。
- [ ] LootJS 脚本：精英/Boss/事件箱各 1 件遗物候选池。
- [ ] 限制每局遗物数量，避免随机池失控。
- [ ] 任务书 3 个遗物任务。
- [ ] 平衡：遗物不能替代 Inf 武器；Boss 仍需锻刀。

### W4 — 遭遇/Boss 池（M1/M3）

- [ ] 验证每个新增实体 id（`/summon` 或注册表）。
- [ ] 每 Act 普通 6 / 精英 3 / Boss 2 的遭遇 JSON。
- [ ] 缺失/非法 id 走 `EncounterFallback`，不得自动通关。
- [ ] Boss 血条/音乐/奖励表。
- [ ] `verify_entity_ids.py` 检查。

### W5 — 元进度（M2）

- [ ] Pufferfish's Skills 分类/技能 JSON。
- [ ] `boss.js` 通关解锁技能/起始包加成。
- [ ] 难度 UI/按键说明。
- [ ] 与 `clears`/scoreboard 同步。
- [ ] 失败/重连/离线清包回归。

### W6 — Act 2/3（M3）

- [ ] 地图参数切到 15/7/6；Act 状态机扩展。
- [ ] 每 Act 房间池/权重/Boss 层。
- [ ] 新房间 NBT/遭遇/战利品。
- [ ] 新装饰 palette。
- [ ] 任务书 35–45 任务。
- [ ] 平衡模拟/实测。

### W7 — 表现/文本/美术（持续）

- [ ] 中英文文本补齐；FTB Quests 英文化单独评估。
- [ ] 地图节点/背景/标题/加载图。
- [ ] 房间主题（锻炉/墓穴/洞窟/竞技场）。
- [ ] 成就/任务图标。

### W8 — 验证/发布（M0/M4）

- [ ] `docs/pack/release-checklist.md` 全项。
- [ ] 自动：Gradle/Python/服务端/客户端。
- [ ] 手动：Act 1/3、死亡、重连、重启、双人。
- [ ] 性能：30 分钟、内存、tick。
- [ ] `--zip` + 启动器导入。
- [ ] CREDITS/许可证。

---

## 6. 数据与内容格式演进

- `RoomDefinition` 需要增加：
  - `npc`（NPC 预设 id）
  - `dialogue`（对话入口）
  - `event_script`（KubeJS 事件脚本 id）
  - `loot_tags`（Artifacts/Inf 战利品标签）
- `EncounterDefinition` 需要增加：
  - `tags`（普通/精英/首领/Boss 池）
  - `music`（可选）
  - `fallback`（每波 fallback 实体，覆盖默认）
- `pack/mods.yaml` 需要 `role/depends/required`，构建脚本按依赖拓扑排序并校验。
- 房间生成器支持 `palette_set`/`decor_set`，不把某个装饰模组的方块写死在代码里。

---

## 7. 测试矩阵

| 层级 | 内容 | 工具 |
|---|---|---|
| 纯逻辑 | 地图生成、RunState、缩放、SavedData、布局 | JUnit |
| 数据 | 房间/遭遇 JSON、SNBT、实体 id | Python + 服务端 reload |
| 集成 | 命令、结构放置、房间进入/完成、Boss 转 Act | RCON 服务端 |
| 客户端 | 地图屏、GUI、资源加载 | `xvfb-run` |
| 整合包 | 启动、模组依赖、mixin、配置 | Prism/HMCL |
| 手动 | 完整 Act、死亡/重连/双人 | 人 |

必须保留的回归：
- 重复点击节点不生成第二个房间；
- 缺失房间数据不推进地图；
- 0 怪遭遇不自动通关；
- 重启不残留旧房间/箱子；
- 离线/返回卷轴不保留本局物品；
- 旁观者只能在下个房间复活；
- `--zip` 缺 id 必须失败。

---

## 8. 风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| 第三方 jar/CF id 无法获取 | M0 无法开始 | 用户提供 `pack/cache`；先锁定核心 15 个 |
| Easy NPC schema/兼容性变化 | 商店/对话返工 | 先做 1 个 NPC 垂直切片；保留 KubeJS 商店箱回退 |
| Pufferfish API/技能 JSON 变化 | 元进度失败 | 先做 1 个技能；保留 `clears` 纯计数 |
| Boss 模组数值不适合 Inf | Boss 太难/太简单 | 遭遇 JSON 可调；保留 vanilla 回退 Boss |
| Better Combat 与 Inf reach/sweep 冲突 | 武器手感错误 | 单独测试；默认不加 |
| 模组过多导致启动/性能问题 | 包不稳定 | 分波次加入；每波跑启动/30 分钟 |
| Artifacts 稀释 Inf 核心 | 玩家不锻刀 | 遗物只做辅助；Boss 需要锻刀 |
| FTB Quests 英文化成本 | 英文玩家体验 | 先中文；M4 单独评估 |
| 3 Act 内容量超预期 | 延期 | M3 可先做 Act 2，Act 3 后置 |

---

## 9. 需要确认的选择

1. **v0.2 外部模组最小集**：是否接受 `Artifacts + Easy NPC + Mobs of Mythology + Illager Invasion + Boss Checklist + Enhanced Boss Bars`？还是先只加 Artifacts/Easy NPC？
2. **元进度**：Pufferfish's Skills 还是保持自研 `clears` 轻量系统？前者体验好，但多一套依赖/配置。
3. **装饰包**：Dusty / Fetzi / XK 三选一，还是先不加？
4. **战斗手感**：是否测试 Better Combat？默认建议不加。
5. **Act 2/3 节奏**：一次做 3 Act，还是先 Act 2 验证再扩 Act 3？
6. **发布目标**：CurseForge zip / Modrinth mrpack / 仅本地包？当前按 CF zip。

---

## 10. 立即下一步（M0）

1. 你提供第三方 jar 到 `pack/cache/<name>.jar`，或告诉我可用的 CurseForge API/网络方式。
2. 我锁定 `pack/mods.yaml` 的 project/file id，跑 `--check`。
3. 生成 `pack/export/local`，用启动器启动核心 15 模组。
4. 按 `docs/pack/release-checklist.md` 跑 Act 1；把结果写回 `docs/pack/act1-balance.md`。
5. M0 通过后，再按第 9 节的选择进入 M1。
