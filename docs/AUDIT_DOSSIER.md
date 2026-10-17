# FIB-Mod (Forge 1.20.1) 严格代码审查材料包与技术档案

---

## 0. 致代码审查员（Reviewer Prompt / 审查指令）

> **请复制以下指令直接提交给 Gemini 4（或其他高级模型）进行审查：**
>
> “你现在同时具备双重身份：
> 1. **资深 Minecraft Forge 模组架构师**：精通 Forge 1.20.1 (Forge 47.4.20 / Java 17) 底层原理、Mojang 渲染管线、GLSL Core Shader、SpongePowered Mixin 及大型整合包性能优化；
> 2. **资深技术导师 (Tech Lead / Mentor)**：擅长技术带人、降低认知负载、将复杂技术抽象为新手友好的模块，并指导初级开发者正确利用 AI 编程。
>
> 下面是一份名为 `FIB-Mod` 的 Forge 1.20.1 模组工程材料包。该模组主要服务于中大型整合包 *Fungal Infection Blasphemy*。
> 请阅读材料包中包含的【项目全景】、【对接人（合作开发者）背景画像】、【架构拓扑】及【核心源码】，并对我们刚完成的工程重构进行**极度严苛的代码审查（Code Review）与教学指导评估**。
>
> 请围绕以下六个维度展开深度审查并给出详尽报告：
> 1. **物理端与逻辑端隔离（Physical Sidedness & Networking）**：是否存在服务端加载客户端类导致的闪退风险？Menu 与 BlockEntity 数据同步是否健壮？
> 2. **渲染与着色器安全（Core Shaders & RenderSystem）**：自定义 Core Shader（`.vsh`/`.fsh`）在不同显卡/光影/优化模组环境下的兼容性、Uniform 传递时序、深度写入与混合模式是否正确？
> 3. **方块实体与性能瓶颈（Tick Efficiency & Capability）**：`IceMakerBlockEntity`、`SolarPanelBlockEntity`、`RTGBlockEntity` 的 Tick 计算量、NBT 保存反序列化成对性、Forge Capabilities 缓存失效（LazyOptional.invalidate）是否合规？
> 4. **Mixin 注入安全性与整合包冲突（Mixin Collision）**：`LivingEntityMixin` 和 `EntityMixin` 对 `isInPowderSnow` 与 `hurt` 的注入点是否稳健？与 Cold Sweat、Spore 等修改温度/冻结的模组是否存在冲突风险？
> 5. **教学体系与架构规范（Pedagogy & Architecture）**：面向新人和 AI 辅助编程的 `docs/` 教学文档是否通俗准确？重构后的工程结构是否符合现代 Forge 最佳实践？
> 6. ⭐ **【特邀专项】面向对接新人的教学与带人建议（Mentoring & Collaboration Guide）**：
>    - 结合材料包中详述的‘对接人背景与痛点’，评估当前代码架构是否足够友好？
>    - 给出具体的‘带人建议’：作为团队主导者，我该如何设计接口与规则，才能让一个只有基础视频知识、严重依赖 AI 的初学者，在接下来的内容制作中既能高效出活、少踩坑，又能真正学到规整的模组开发知识，降低团队沟通心智负担？”

---

## 1. 合作开发者（对接人）背景画像与合作痛点

为了让审查结论更具针对性和指导意义，特将本项目协作者的实际研发情况剖析如下：

### 1.1 技能基线与知识来源
* **编程基础**：缺乏系统的 Java 面向对象设计基础、计算机图形学与 Gradle 构建体系知识。
* **主要学习路径**：完整跟随着 B站 UP主 **【北山_Besson】的《1.20.1 Forge 长线模组开发教程》**（开源仓库 `BeiShanair/TutorialMod-1.20.1-Forge`）视频进行模组入门。掌握了基本的单方块注册、物品注册、食物、简单创造栏与数据生成器概念。

### 1.2 开发模式：“面向结果编程” + “深度依赖 AI 补全”
* **现状**：整合包玩法设计需要大量超出基础教程范围的高级机制（如：带有 FE 能量/流体管道交互的制冰机、GeckoLib 骨骼动画 Boss、逐像素撕裂计算的 Core Shader、原版细雪与冻结判定的 Mixin 注入、复杂村民货币交易池）。
* 遇到视频没讲的内容时，协作者选择向 AI（ChatGPT / Claude 等）提问并直接复制生成出的代码。

### 1.3 曾发生的核心事故与沟通障碍（痛点复盘）
1. **版本漂移与概念混淆**：
   * AI 经常混入 1.20.6 / 26.4 快照的 API（如 `DataComponents`、JEI 19+ 移除的方法）。协作者看到编译报错时无法分辨 Warning（警告）与 Error（报错），误认为“报错也是 1.20.6 弃用，不用管它”。
2. **“只给枪、不给子弹”的漏文件隐患**：
   * AI 在生成包含多个联动类的功能时（如 `ModEntities` 注册 + `IceBallItem` 物品 + `IceBall` 投射物实体），协作者往往只粘贴了一部分，漏掉了关键的实体实现类或着色器源文件（如只推了 `dimensional_crack.json`，遗漏了 `.vsh` 和 `.fsh`），导致本地或队友拉取代码后大面积报红（`无法解析符号`）。
3. **复制粘贴导致的样板代码失控**：
   * 之前为 8 个定制村民职业编写交易时，将一段长达 50 行的字符串数组循环解析逻辑，不加封装地硬复制了 8 遍，导致类库体积膨胀且难于维护。
4. **协作交流中的断层与心力交瘁**：
   * 协作者在遇到编译故障时，往往只截图局部报错的一行红字随口推测，缺乏工程排错思维（上下文、调用栈、版本对比），使得项目推进中的技术对接成本极高。

---

## 2. 外部联动依赖与技术栈清单

| 依赖模块 | 声明版本 / 来源 | 用途说明 |
| :--- | :--- | :--- |
| **Minecraft Forge** | `1.20.1-47.4.20` | 底层模组加载器 |
| **Create (机械动力)** | `6.0.8-289` | 工程师交易、方块护目镜 (IHaveGoggleInformation) 支持 |
| **GeckoLib** | `4.8.4` | 哈吉作者 Boss (`HajjiAuthorEntity`) 的骨骼动画与渲染 |
| **JEI / EMI** | `15.20.0.130` (API) | 制冰机配方在 JEI 及 EMI (JEI 兼容层) 中的图形化展示 |
| **MixinExtras** | `0.4.1` | 用于 `@ModifyExpressionValue` 精准修改原版细雪与冻结逻辑 |
| **TACZ (时装枪械)** | `1028108:8141310` | 军火商交易池兑换枪械通用弹药与战术近战武器 |
| **Refined Storage** | `243076:4844585` | 仓储商人交易池兑换线缆、升级卡与存储磁盘 |
| **Lightman's Currency** | 整合包集成 | 全局经济货币（铁币、金币、绿宝石币、下界合金币） |

---

## 3. 全局重构架构拓扑 (Architecture Layout)

```
com.fib.fib
├── FIBMod.java                      # 主生命周期司令部：注册 MOD 总线、配置加载，无冗余废料
├── Config.java                      # 规范化的模组配置（机器能耗、裂缝视觉粒子等）
├── ModDataGenerator.java            # 修正拼写后的数据生成器总入口（runData 专用）
│
├── client/                          # 🖥️ 客户端专属层 (value = Dist.CLIENT)
│   ├── ClientSetup.java             # 聚合注册中心：MenuScreens 绑定、实体/方块渲染器注册
│   ├── model/                       # GeckoLib 实体骨骼模型 (HajjiAuthorModel)
│   └── render/
│       ├── ModShaders.java          # 自定义 Core Shader 注册 (RegisterShadersEvent) 与 RenderType
│       ├── block/                   # 如梦玩偶方块实体渲染器 (RuMengDollBlockEntityRenderer)
│       └── entity/                  # 维度裂缝、冰球、Boss 渲染器
│
├── init/                            # 📋 注册中心（DeferredRegister 预约登记簿）
│   ├── ModBlocks.java               # 模组方块 (注册名保持不变，保证存档/KubeJS绝对兼容)
│   ├── ModItems.java                # 模组物品
│   ├── ModBlockEntities.java        # 方块实体类型
│   ├── ModEntities.java             # 实体类型 (ICE_BALL, HAJJI_AUTHOR, DIMENSIONAL_CRACK)
│   ├── ModMenuTypes.java            # 容器菜单数据类型
│   ├── ModEffects.java              # 状态效果 (FREEZING 冻结)
│   ├── ModPotions.java              # 药水注册
│   ├── ModRecipes.java              # 配方类型与序列化器
│   ├── ModSounds.java               # 声音事件
│   ├── item/custom/IceBallItem.java # 冰球投掷物品行为
│   ├── entity/custom/IceBall.java   # 冰球投射物实体行为
│   ├── trade/                       # 🛒 村民交易池系统
│   │   ├── TradeHelper.java         # 抽取的公共二维数组解析与 NBT 反序列化引擎
│   │   └── [8个交易类].java          # RS, TACZ, BEGGAR, DOCTOR, ENGINEER, SCIENTISTS 等精简配置
│   └── villager/ModVillager.java    # 8 大定制职业与工作台 POI 注册
│
├── blockentity/                     # ⚙️ 方块实体数据核心
│   ├── IceMakerBlockEntity.java     # 制冰机核心：流体罐、物品槽、FE能耗、配方轮询、回流机制
│   ├── RTGBlockEntity.java          # 放射性同位素温差发电机
│   └── SolarPanelBlockEntity.java   # 太阳能板逻辑
│
├── compat/jei/                      # 🔌 JEI / EMI 兼容
│   ├── ModJeiPlugin.java            # @JeiPlugin 注解入口（隔离在开发期与存在 JEI 时加载）
│   └── IceMakerRecipeCategory.java  # 制冰机配方页绘制
│
├── mixin/                           # 💉 字节码注入
│   ├── LivingEntityMixin.java       # 对齐冻结机制：Freezing 效果等同处于细雪不衰减 + 满冻结 1.5 倍伤
│   └── EntityMixin.java             # Freezing 效果下进入 baseTick 立即扑灭火焰
│
└── docs/                            # 📚 教学文档体系
    ├── 01_写给新人的Forge模组开发核心与生命周期.md
    ├── 02_项目规范结构与Git协作避坑指南.md
    ├── 03_方块_方块实体与数据存储完全拆解.md
    ├── 04_着色器与渲染管线小白手册.md
    └── 05_面向结果开发_如何让AI写出能用的Forge代码.md
```

---

## 4. 关键源码与技术实现全景

### 4.1 主类精净化：[`FIBMod.java`](file:///e:/McPack/Packmaking/FIB-Mod/FIB-mod/src/main/java/com/fib/fib/FIBMod.java)
```java
// 移除所有 MDK 模板废弃字段，统一挂载所有 DeferredRegister
@Mod(FIBMod.MOD_ID)
public class FIBMod {
    public static final String MOD_ID = "fib_mod";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final boolean DEBUG = true;

    public FIBMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModSounds.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModVillager.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModEntities.register(modEventBus);
        ModEffects.register(modEventBus);
        ModPotions.register(modEventBus);
        ModRecipes.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
```

### 4.2 统一客户端注册中心：[`ClientSetup.java`](file:///e:/McPack/Packmaking/FIB-Mod/FIB-mod/src/main/java/com/fib/fib/client/ClientSetup.java)
```java
// 彻底消除了原先在三个不同地方重复订阅 RegisterRenderers 的安全隐患
@Mod.EventBusSubscriber(modid = FIBMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 使用 enqueueWork 保证主线程安全
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.ICE_MAKER_MENU.get(), IceMakerScreen::new);
            MenuScreens.register(ModMenuTypes.CRATE_MENU.get(), CrateScreen::new);
            MenuScreens.register(ModMenuTypes.TRASH_CAN_MENU.get(), TrashCanScreen::new);
            MenuScreens.register(ModMenuTypes.CORPSE1_MENU.get(), Corpse1Screen::new);
            MenuScreens.register(ModMenuTypes.CORPSE2_MENU.get(), Corpse2Screen::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.RU_MENG_DOLL_BE.get(), RuMengDollBlockEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ICE_BALL.get(), IceBallRenderer::new);
        event.registerEntityRenderer(ModEntities.HAJJI_AUTHOR.get(), HajjiAuthorRenderer::new);
        event.registerEntityRenderer(ModEntities.DIMENSIONAL_CRACK.get(), DimensionalCrackRenderer::new);
    }
}
```

### 4.3 维度裂缝 Core Shader 架构全貌
#### (1) JSON 契约：[`dimensional_crack.json`](file:///e:/McPack/Packmaking/FIB-Mod/FIB-mod/src/main/resources/assets/fib_mod/shaders/core/dimensional_crack.json)
声明使用 3 个 Sampler 纹理槽（星空、水波纹、闪电）及 7 项 Uniform 动态矩阵与视差参数。

#### (2) 顶点着色器：[`dimensional_crack.vsh`](file:///e:/McPack/Packmaking/FIB-Mod/FIB-mod/src/main/resources/assets/fib_mod/shaders/core/dimensional_crack.vsh)
```glsl
#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec4 vertexColor;
out vec2 crackCoord;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    crackCoord = UV0; // 局部归一化坐标：单位圆作为撕裂边缘
}
```

#### (3) 片元着色器：[`dimensional_crack.fsh`](file:///e:/McPack/Packmaking/FIB-Mod/FIB-mod/src/main/resources/assets/fib_mod/shaders/core/dimensional_crack.fsh)
* 实现了 `CrackMode = 0`（实心星空层，写深度防透视）与 `CrackMode = 1`（透明光晕与闪电层，不写深度防穿模切边）的双 Pass 绘制模式。
* 实现了相机视角视差（Parallax）三维纵深感算法与噪声撕裂轮廓极坐标计算。

### 4.4 村民交易引擎：[`TradeHelper.java`](file:///e:/McPack/Packmaking/FIB-Mod/FIB-mod/src/main/java/com/fib/fib/init/trade/TradeHelper.java)
```java
// 将所有繁重的 12 元组参数反序列化与 SNBT 转换逻辑统一封装，消除了 8 个子类的所有样板复制
public class TradeHelper {
    public static void registerTradePool(VillagerTradesEvent event, String professionKey, String[][] tradeConfigs) {
        ModVillager.RegEntry regEntry = ModVillager.VILLAGERS.get(professionKey);
        if (regEntry == null || !event.getType().equals(regEntry.profession().get())) return;
        Int2ObjectMap<List<VillagerTrades.ItemListing>> tierTradePool = event.getTrades();
        for (String[] cfg : tradeConfigs) {
            // 解析等级、数量、经验、SNBT标签并注入 MerchantOffer...
        }
    }
}
```

### 4.5 冻结与细雪机制 Mixin：[`LivingEntityMixin.java`](file:///e:/McPack/Packmaking/FIB-Mod/FIB-mod/src/main/java/com/fib/fib/mixin/LivingEntityMixin.java)
```java
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @ModifyExpressionValue(
            method = "aiStep",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lnet/minecraft/world/entity/LivingEntity;isInPowderSnow:Z")
    )
    private boolean fib$freezingCountsAsPowderSnow(boolean original) {
        return original || ((LivingEntity) (Object) this).hasEffect(ModEffects.FREEZING.get());
    }

    @Redirect(
            method = "aiStep",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                    ordinal = 0)
    )
    private boolean fib$boostFreezeDamage(LivingEntity self, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FREEZING) && self.hasEffect(ModEffects.FREEZING.get())) {
            amount *= 1.5F;
        }
        return self.hurt(source, amount);
    }
}
```

---

## 5. 审查焦点检查清单 (Reviewer Focus Checklist)

请审查员针对以下疑难重点展开专项排查：

1. **制冰机 (`IceMakerBlockEntity`)**：
   - 机器中既有物品存储（`ItemStackHandler`），又有流体存储（`FluidTank`）和能量存储（`EnergyStorage`）。
   - 在方块破坏（`setRemoved` / `invalidateCaps`）时，`LazyOptional` 的注销时机是否正确？
   - 产物自动回流输入槽的判断逻辑（`handleRecycling`）是否存在物品刷取或吃物品的边界漏洞？
2. **维度裂缝着色器 (`ModShaders` & `DimensionalCrackRenderer`)**：
   - `RenderSystem.setShaderTexture(1, RIPPLE_TEXTURE)` 和 `(2, LIGHTNING_TEXTURE)` 直接写入纹理槽。在装有 Iris / Oculus / Embeddium 等光影/优化模组时，是否有槽位被覆盖导致崩溃的风险？
   - `immediate.endBatch(RenderType)` 的强制打断批处理是否会引发严重的 DrawCall 性能衰退？
3. **Mixin 侵入性**：
   - `LivingEntityMixin` 中拦截了 `isInPowderSnow` 的读取。如果有其他模组（如 Cold Sweat、Environment、Spore）同样修改了细雪逻辑，该 Mixin 是否具备容错性？
4. **面向新人的教学架构与带人策略 (`docs/01` ~ `05`)**：
   - 当前的封装（如 `TradeHelper`、统一的 `ClientSetup`）是否真正降低了该开发者的心智门槛？
   - 针对他“面向结果 + 依赖 AI”的特质，如何进一步优化协作流程，避免未来继续发生漏文件和沟通疲劳？

---
*(材料包整理完毕，请直接复制本档案提交审查)*
