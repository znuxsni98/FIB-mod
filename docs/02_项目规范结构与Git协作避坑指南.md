# 02. 项目规范结构与 Git 协作避坑指南

当你和队友一起开发模组时，最怕的不是代码写得慢，而是**“互相猜心思、满屏红字找不到原因、漏交文件导致对方一拉代码就炸”**。这份文档总结了我们项目的规范和日常开发习惯。

---

## 一、标准包结构与代码归宿

以前项目中各种类随便散落在不同的包里，找一个东西要在好几个包之间来回跳。重构后的标准结构如下：

```
com.fib.fib
├── FIBMod.java                      # 主入口：只做两件事：给各模块分发总线、加载配置文件
├── Config.java                      # 真实配置文件类
├── client/                          # 客户端专属目录（只有玩家屏幕看得到的东西）
│   ├── ClientSetup.java             # 唯一的客户端注册聚合中心（屏幕、实体渲染器、方块渲染器）
│   ├── render/                      # 自定义渲染逻辑（方块实体、实体、Core Shader）
│   └── model/                       # 实体模型（GeckoLib、Java模型）
├── init/                            # 注册中心（所有物品、方块、实体的登记簿）
│   ├── ModBlocks.java               # 登记所有方块
│   ├── ModItems.java                # 登记所有物品
│   ├── ModBlockEntities.java        # 登记所有方块实体
│   ├── ModEntities.java             # 登记所有生物/投射物实体
│   ├── ModMenuTypes.java            # 登记所有容器界面数据类型
│   ├── ModEffects.java              # 登记所有药水/状态效果
│   ├── ModRecipes.java              # 登记所有自定义合成配方类型
│   ├── ModSounds.java               # 登记所有自定义声音
│   └── villager/                    # 村民职业与工作台 POI 注册
├── block/                           # 方块的行为逻辑（比如右键打开GUI、碰撞箱大小）
├── blockentity/                     # 方块实体的数据与计算（存储电量、流体、计时器）
├── item/                            # 物品的行为逻辑（比如右键扔出冰球）
├── entity/                          # 实体/生物的具体逻辑（移动、碰撞、伤害）
├── gui/                             # 容器界面系统（Menu 负责两端同步，Screen 负责画按钮贴图）
├── compat/                          # 外部模组兼容（JEI、EMI配方展示）
├── trade/                           # 村民交易池数据
└── mixin/                           # 深度修改 Minecraft 原版逻辑的注入类
```

---

## 二、命名规范（远离野鸡代码）

好的命名让别人一眼看懂你的代码，不需要问你。

### 1. Java 类名：大驼峰（PascalCase），不要下划线，不要全大写
* ❌ 错误示范：`Ice_Maker.java`、`RS.java`、`BEGGAR.java`、`Bowl_Block.java`
* ✅ 正确规范：`IceMakerBlock.java`、`RsTrade.java`、`BeggarTrade.java`、`BowlBlock.java`

> **注意区分**：
> * **Java 类名** 是给程序员看的：`IceMakerBlock`
> * **Forge 注册名（Registry ID）** 是给 Minecraft 引擎和 JSON 资源引用的，一律小写蛇形：`fib_mod:ice_maker`

### 2. 包名：一律全小写
* ❌ 错误示范：`com.fib.fib.init.Villager`（V 大写了）
* ✅ 正确规范：`com.fib.fib.init.villager`

---

## 三、Git 提交前必检清单（黄金三步法）

为什么之前会出现“漏了 `IceBall.java`”和“漏了 `dimensional_crack.vsh`”？
因为你在提交前**没有看 Git 的待提交状态**。请养成以下三步肌肉记忆：

### 第一步：在终端敲 `git status`
查看输出：
* 是不是有红色字体的 **Untracked files**（未追踪文件）？
* 你新创建的 `.java`、`.json`、`.png`、`.vsh`、`.fsh` 是不是赫然躺在未追踪列表里？
* 必须把它们全部用 `git add` 加进去！

### 第二步：本地先跑一次 `./gradlew compileJava`
在终端输入：
```bash
./gradlew compileJava
```
* 如果输出 **`BUILD SUCCESSFUL`**：恭喜你，代码没有语法和引用缺失，可以放心提交！
* 如果输出 **`BUILD FAILED`**：**绝对不要推给队友！** 仔细看屏幕上的报错行号，把红字修干净后再试。

### 第三步：写人话 Commit Message
* ❌ 别再一律写“上传”、“补充”、“得”。
* ✅ 写清楚：“添加冰球投射物与渲染器”、“补齐维度裂缝核心着色器源码”。

---

## 四、团队沟通技巧（怎么让沟通不心累）

* **别只截半截报错框**：
  * 报错截图如果只有一行 `无法解析符号 'xxx'`，别人根本不知道你改了哪个文件。请把报错上面的文件路径、行号一起带上，或者直接复制日志文字发出来。
* **分清“警告”和“报错”**：
  * 编译如果没通过，那叫**报错（Error）**，每一个报错都必须解决；
  * 如果项目是 1.20.1，**不要猜测说“这是高版本弃用”**，如果在 1.20.1 找不到该方法，说明你参考的资料版本错了。
* **多做本地完整测试**：
  * 编辑器里不报红 $\neq$ 游戏里不会崩。最好启动一次游戏（Run Client），看到能进单人世界、拿在手上不崩溃，再跟队友说“好了”。
