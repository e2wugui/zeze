package GameSim;

import Zeze.Util.PropertiesHelper;

/**
 * L 场景（LMmoGameSim）游戏数值与节奏配置。
 *
 * <p>数值静态配置表 + 真实形态公式（共识：不搞外部配置文件）。所有时间参数为游戏时间毫秒，
 * 休眠/周期处统一 ÷K 折算墙钟（K=时间缩放旋钮，K=1 即真人节奏档）。
 *
 * <p>旋钮经 system property 透传（gradle benchGame -PgamePlayers=… 映射为 game.players）：
 * game.players（默认 5000 在线）、game.k（默认 10）、game.minutes（每轮游戏分钟，默认 10）。
 *
 * <p>负载模型（2026-10-14 终版，K=10）：单用户高频的 redo 顾虑实证解除——同一玩家的
 * 事务在其虚拟线程内串行（同步 call 返回才进下一步），不存在自我并发；redo 由全局密度
 * ×共享行提交率决定，主源 rank 分片行已 512 化缓解（K=14 实测人均 5.09 txn/s、延迟反降）。
 * 容量以在线数为主旋钮（-PgamePlayers），K 调整单玩家操作强度；旧 K=20/80/160 梯度
 * 容量档废弃不变。
 */
public final class GameConfig {
	// ===== 基准旋钮 =====
	public final int players;        // 在线玩家数（角色 id 0..players-1）
	public final int timeScaleK;     // 时间缩放：游戏时间 = 墙钟 × K（即所有节奏 ÷K）
	public final int windowMinutes;  // 每测量窗口的游戏时长（分钟）
	public final int warmups;
	public final int rounds;

	// ===== 诊断休眠旋钮（默认值=基线行为，裁决协议专用，见 BENCHMARK.md L 节）=====
	public final int checkpointPeriodMs; // game.checkpointPeriodMs：反事实 A（拉长周期看 p99 尖峰是否消失）
	public final boolean fanoutOff;      // game.fanoutOff：反事实 B（系统邮件 fan-out 静默）
	public final int slowLogMs;          // game.slowLogMs：慢操作时间线阈值（0=关，见 TimelineLog）
	public final boolean listener;       // game.listener：全表轻量计数监听器（默认 false=NONE）

	// ===== 持久化容器后端对照专用旋钮（默认 0/关闭=基线行为，见 BENCHMARK.md 容器后端节）=====
	public final int bulkRewardSlots;   // game.bulkRewardSlots：每窗口"批量补偿"物品种类数（0=关；槽位 WarehouseSlotBase+ 起逐窗口递增不覆盖）
	public final int bulkRewardPlayers; // game.bulkRewardPlayers：每窗口领取批量补偿的玩家数（默认 1000，均匀散布）
	public final int coldLoadRounds;    // game.coldLoadRounds：停机后全量角色 encode→decode 冷装载回环轮数（0=关）
	public final int plistWeight;       // game.plistWeight：大厅"PList 追加"活动权重（0=关=基线；PList 内存对照负载，append-only 不做中间插删）

	// ===== 节奏（游戏时间）=====
	public static final long HeartbeatPeriodMs = 15_000;   // 心跳周期
	public static final long HeartbeatTimeoutMs = 45_000;  // 超时踢线阈值（超过即过载信号）
	public static final long HbScanPeriodMs = 5_000;       // 会话扫描周期
	public static final long ThinkMinMs = 2_000, ThinkMaxMs = 6_000; // 大厅发呆间隔
	public static final long LoginRampMs = 60_000;         // 开服登录潮摊开窗口
	public static final long MailFanoutAtMs = 5 * 60_000;  // 每测量窗口内的系统邮件时刻（窗口起点偏移）
	public static final long BulkRewardAtMs = 7 * 60_000;  // 每测量窗口内的系统批量补偿时刻（邮件之后，相位不交叠）
	/** 批量补偿的仓库槽位基址：常规背包只占 0..BagSlots-1，仓库占此基址起（逐窗口递增） */
	public static final int WarehouseSlotBase = 1000;
	/** PList 对照负载的单列表封顶（每角色 4 列表：Tracks/QuestLog/GiftLog/Warehouse） */
	public static final int PlistCapPerList = 150;

	// ===== 背包 =====
	public static final int BagSlots = 16;
	public static final int ItemPotion = 1;   // 药水：可堆叠、使用回血
	public static final int ItemWeapon = 2;   // 武器：占装备栏
	public static final int ItemArmor = 3;    // 护甲：占装备栏

	// ===== 战斗（房间内存态数值）=====
	public static final int PartySize = 4;
	public static final int MonsterCount = 8;
	public static final int MonsterHp = 120;
	public static final int MonsterAtk = 12;
	public static final int MonsterDef = 5;
	public static final int PlayerBaseAtk = 20;
	public static final int PlayerMaxHp = 100;
	public static final long RoomTickMs = 1_500;

	// 榜类型（内置 Zeze.Game.Rank 模块的 rankType，app 内自定义号段）
	public static final int RankTypeLevel = 1001;
	public static final int RankTypeDamage = 1002;

	// ===== 大厅活动权重（发呆时按 RNG 挑选；战斗 loot 等事件驱动不在此列）=====
	// loot 降频（用户口径）：冒险权重 30→10（补给好友查询）——怪血加倍是伪杠杆：
	// 房间时长同比拉长、在房 DPS 等比上升，击杀率闭环自平衡（实测 hp×2 仅 -37%）；
	// 权重才是正杠杆（进房率直接决定总战斗 DPS）
	public static final int WFriendQuery = 40;
	public static final int WUsePotion = 8;
	public static final int WEquipChange = 5;
	public static final int WSellShop = 10;
	public static final int WMailClaim = 12;
	public static final int WMailSend = 4;
	public static final int WTrade = 6;
	public static final int WAdventure = 10;
	public static final int WIdleSum = WFriendQuery + WUsePotion + WEquipChange + WSellShop
			+ WMailClaim + WMailSend + WTrade + WAdventure;

	public GameConfig() {
		players = PropertiesHelper.getInt("game.players", 5000);
		timeScaleK = PropertiesHelper.getInt("game.k", 10);
		windowMinutes = PropertiesHelper.getInt("game.minutes", 10);
		warmups = PropertiesHelper.getInt("game.warmups", 1);
		rounds = PropertiesHelper.getInt("game.rounds", 3);
		checkpointPeriodMs = PropertiesHelper.getInt("game.checkpointPeriodMs", 30_000);
		fanoutOff = PropertiesHelper.getBool("game.fanoutOff", false);
		slowLogMs = PropertiesHelper.getInt("game.slowLogMs", 0);
		listener = PropertiesHelper.getBool("game.listener", false);
		bulkRewardSlots = PropertiesHelper.getInt("game.bulkRewardSlots", 0);
		bulkRewardPlayers = PropertiesHelper.getInt("game.bulkRewardPlayers", 1000);
		coldLoadRounds = PropertiesHelper.getInt("game.coldLoadRounds", 0);
		plistWeight = PropertiesHelper.getInt("game.plistWeight", 0);
	}

	/** 游戏毫秒 → 墙钟毫秒（休眠用） */
	public long wall(long gameMs) {
		return Math.max(1, gameMs / timeScaleK);
	}

	/** 升级所需累计经验：level n → n+1 需 100×n（等级曲线，真实形态） */
	public static long expToNext(int level) {
		return 100L * level;
	}
}
