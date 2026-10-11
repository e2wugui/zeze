package GameSim;

import Zeze.Util.PropertiesHelper;

/**
 * L 场景（LMmoGameSim）游戏数值与节奏配置。
 *
 * <p>数值静态配置表 + 真实形态公式（共识：不搞外部配置文件）。所有时间参数为游戏时间毫秒，
 * 休眠/周期处统一 ÷K 折算墙钟（K=时间缩放旋钮，K=1 即真人节奏档）。
 *
 * <p>旋钮经 system property 透传（gradle benchGame -PgamePlayers=… 映射为 game.players）：
 * game.players（默认 5000 在线）、game.k（默认 20）、game.minutes（每轮游戏分钟，默认 10）。
 */
public final class GameConfig {
	// ===== 基准旋钮 =====
	public final int players;        // 在线玩家数（角色 id 0..players-1）
	public final int timeScaleK;     // 时间缩放：游戏时间 = 墙钟 × K（即所有节奏 ÷K）
	public final int windowMinutes;  // 每测量窗口的游戏时长（分钟）
	public final int warmups;
	public final int rounds;

	// ===== 节奏（游戏时间）=====
	public static final long HeartbeatPeriodMs = 15_000;   // 心跳周期
	public static final long HeartbeatTimeoutMs = 45_000;  // 超时踢线阈值（超过即过载信号）
	public static final long HbScanPeriodMs = 5_000;       // 会话扫描周期
	public static final long ThinkMinMs = 2_000, ThinkMaxMs = 6_000; // 大厅发呆间隔
	public static final long LoginRampMs = 60_000;         // 开服登录潮摊开窗口
	public static final long MailFanoutAtMs = 5 * 60_000;  // 每测量窗口内的系统邮件时刻（窗口起点偏移）

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
		timeScaleK = PropertiesHelper.getInt("game.k", 20);
		windowMinutes = PropertiesHelper.getInt("game.minutes", 10);
		warmups = PropertiesHelper.getInt("game.warmups", 1);
		rounds = PropertiesHelper.getInt("game.rounds", 3);
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
