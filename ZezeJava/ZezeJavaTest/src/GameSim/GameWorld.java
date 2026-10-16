package GameSim;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import Zeze.Application;
import Zeze.Builtin.Game.Rank.BConcurrentKey;
import Zeze.Config;
import Zeze.Game.Rank;
import Zeze.Util.DaemonTimer;
import Zeze.Util.Action0;
import demo.Game.tMail;
import demo.Game.tRole;
import demo.Game.tSession;

/**
 * 游戏世界：裸 Application + Game 模块四表 + 共享设施（在线注册/匹配队列/邮件号段/心跳扫描）。
 *
 * <p>架构铁律（共识）：房间=内存态战斗状态机（BattleRoom 纯 Java，不经任何表），
 * 一切落库事务发生在大厅；心跳超时踢线是唯一 DaemonTimer 用法，扫描走内存镜像，
 * 仅在真踢人时产生事务（真实游戏形态：活跃度内存跟踪，避免每 5 秒 5000 行读事务）。
 *
 * <p>事务体纯化约定：随机数消耗、号段分配、影子账本全部在事务外/提交后——
 * 乐观锁重试重跑事务体不得产生任何外逸副作用。
 */
public final class GameWorld {
	private static final org.apache.logging.log4j.Logger logger =
			org.apache.logging.log4j.LogManager.getLogger(GameWorld.class);

	public final GameConfig cfg;
	public final Ledger ledger = new Ledger();
	public final tRole tRole;
	public final tMail tMail;
	public final tSession tSession;
	public final Application zeze;
	// 排行=内置 Zeze.Game.Rank 模块（真实组件口径：分片并发榜+快照合并+容量管理开箱即用）
	public final Rank rank;
	public final BConcurrentKey levelBoard;
	public final BConcurrentKey damageBoard;

	// 在线注册（roleId -> 玩家）；与 tSession 行集恒等（守恒#3）
	public final ConcurrentHashMap<Long, PlayerSim> online = new ConcurrentHashMap<>();
	// 匹配队列（冒险加入）
	final ConcurrentLinkedQueue<PlayerSim> matchmaking = new ConcurrentLinkedQueue<>();
	// 邮件号段（决策点事务外预分配；事务携带定参，重试不重耗）
	private final AtomicLong mailSeq = new AtomicLong();
	private final LongAdder mailsInserted = new LongAdder();
	private final LongAdder mailsClaimed = new LongAdder();

	public volatile boolean stopped;
	/** 当前测量窗口序号：0=非测量段（统计静默）。玩家 record 以操作发起时的窗口为准 */
	public volatile int windowId;

	private final long startNanos = System.nanoTime();
	private final DaemonTimer hbScanner;
	private final OpRecorder sysRecorder;
	private int roomSeq;

	/** 玩家循环异常首例打印门闩 */
	private final java.util.concurrent.atomic.AtomicBoolean loopErrorPrinted = new java.util.concurrent.atomic.AtomicBoolean();

	boolean markLoopError() {
		return loopErrorPrinted.compareAndSet(false, true);
	}

	public GameWorld(GameConfig cfg) throws Exception {
		this.cfg = cfg;
		// 裸 Config 程序化构造（不 Config.load：xml 补默认值会点亮 Onz 服务，disable SM 下启动 NPE；
		// 先例 TestCacheDirLock.newConf）。单进程模式：无 GCM/无服务发现，本地锁。
		var config = new Config();
		config.setServiceManager("disable");
		// 独占 serverId（FastServerIds 单点登记）：bench 车道是串行共享 JVM，车道内 harness 环境
		// 与他类 Application 已持默认 serverId=0 的 zeze_cache_0.lock——裸 Config 默认 0 会撞
		// FileMutex 同 JVM 双取秒拒（2026-10-13b bench 5/5 确定性红首暴露）。
		config.setServerId(harness.FastServerIds.GAMESIM_WORLD);
		var tableConf = new Config.TableConf();
		// 真实服缓存充裕（G 场景专测 thrashing，L 不混入该形态）：工作集 ~2万记录，给 10 万
		tableConf.setCacheCapacity(100_000);
		config.setDefaultTableConf(tableConf);
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("");
		config.getDatabaseConfMap().put("", dbConf);
		// 检查点真实周期：核心基准配置 6000000=事实关闭；L 按 30 秒墙钟周期冲刷（checkpoint 交叠进画像）。
		// 休眠旋钮 game.checkpointPeriodMs（默认 30s=基线）：裁决反事实 A 拉长周期专用
		config.setCheckpointPeriod(cfg.checkpointPeriodMs);
		zeze = new Application("GameSim", config);
		tRole = new tRole();
		tMail = new tMail();
		tSession = new tSession();
		// Rank 模块初始化：基类直构（Rank.create 是 redirect 生成子类，裸环境写不进本地表）
		// ——GameRankFactory 同包工厂（先例 TestRankCountNeedKey.newRank）；
		// RegisterZezeTables 必须先于 start()（坑#2）
		rank = Zeze.Game.GameRankFactory.createLocal(zeze);
		// 榜分片并发 128→512：loot 每笔必写伤害榜，players/128 在数万在线时同分片数百人
		// 共写一行成 redo 主源；512 把同分片人数降到 1/4。concurrentLevel 变更=旧榜数据
		// 失效需清重建（Rank.getConcurrentLevel 注释警示）——GameSim 每轮空库起跑无迁移面。
		rank.setFuncConcurrentLevel(rankType -> 512);
		levelBoard = Rank.newRankKey(GameConfig.RankTypeLevel, BConcurrentKey.TimeTypeTotal);
		damageBoard = Rank.newRankKey(GameConfig.RankTypeDamage, BConcurrentKey.TimeTypeTotal);
		// 坑#2：addTable 必须先于 start()
		zeze.addTable("", tRole);
		zeze.addTable("", tMail);
		zeze.addTable("", tSession);
		sysRecorder = OpRecorder.createSystem();
		hbScanner = new DaemonTimer("GameSim.HbScan", cfg.wall(GameConfig.HbScanPeriodMs),
				(Action0)this::scanHeartbeats);
	}

	public void start() throws Exception {
		if (TimelineLog.enabled)
			TimelineLog.event("world start epochMs=" + System.currentTimeMillis()
					+ " players=" + cfg.players + " k=" + cfg.timeScaleK
					+ " cpPeriodMs=" + cfg.checkpointPeriodMs + " fanoutOff=" + cfg.fanoutOff);
		zeze.start();
		hbScanner.start();
	}

	/** 停世界（玩家线程退出、定时器停），数据库保持可用——对账事务在其后、stop() 之前跑 */
	public void shutdown() {
		stopped = true;
		if (TimelineLog.enabled)
			TimelineLog.event("world shutdown");
		hbScanner.stop();
	}

	public void stop() throws Exception {
		shutdown();
		zeze.stop();
	}

	/** 游戏时钟：世界启动以来游戏毫秒 = 墙钟 × K */
	public long gameNow() {
		return (System.nanoTime() - startNanos) / 1_000_000 * cfg.timeScaleK;
	}

	// ===== 事务包装：计时+按操作类型留档（窗口口径以发起时为准）=====

	public boolean call(OpRecorder rec, OpRecorder.OpType op, String name, Zeze.Util.FuncLong body) {
		int win = windowId;
		long t0 = System.nanoTime();
		long rc;
		try {
			rc = zeze.newProcedure(body, name).call();
		} catch (Throwable e) {
			// 过程体异常由 Procedure.call() 统一转错误码 Procedure.Exception（错误账本走下方
			// rc≠0 路径）；真正逃逸 call() 的只有 AssertionError（单测兼容重抛）与 GoBackZeze
			// （设计内传播）——此 catch 只兜这两类，非承重（首例打印堆栈）
			rec.record(op, win, t0);
			rec.error(op);
			if (markLoopError()) // 首例带栈，后续仅计数（防刷屏）
				logger.error("procedure escape exception (first)", e);
			return false;
		}
		rec.record(op, win, t0);
		if (TimelineLog.enabled) { // 诊断跑才开（game.slowLogMs），基线零开销
			long us = (System.nanoTime() - t0) / 1000;
			if (us >= TimelineLog.thresholdUs)
				TimelineLog.slowOp(op.name(), us, win);
		}
		if (rc != 0)
			rec.error(op);
		return rc == 0;
	}

	// ===== 心跳扫描：内存判过期，踢人才落事务 =====

	private void scanHeartbeats() throws Exception {
		if (stopped)
			return;
		long now = gameNow();
		for (var p : online.values()) {
			if (!p.loggedIn || now - p.lastActiveGameMs <= GameConfig.HeartbeatTimeoutMs)
				continue;
			// 过载（或滞留）玩家：移除会话行+注册，玩家线程下次 step 发现 kicked 重登
			if (call(sysRecorder, OpRecorder.OpType.hbScan, "GameSim.HbKick", () -> {
					tSession.remove(p.roleId);
					return 0L;
				})) {
				online.remove(p.roleId, p);
				p.kicked = true;
				if (TimelineLog.enabled)
					TimelineLog.event("kick role=" + p.roleId + " lastActive=" + p.lastActiveGameMs);
				}
		}
	}

	// ===== 匹配：凑满 4 人成房间（虚拟线程跑内存战斗）=====

	public void joinMatchmaking(PlayerSim p) {
		matchmaking.add(p);
		var room = new PlayerSim[GameConfig.PartySize];
		synchronized (matchmaking) {
			if (matchmaking.size() < GameConfig.PartySize)
				return;
			for (int i = 0; i < room.length; i++)
				room[i] = matchmaking.poll();
		}
		int seq = ++roomSeq;
		Thread.ofVirtual().name("gs-room-" + seq).start(new BattleRoom(this, room, seq));
	}

	// ===== 系统邮件 fan-out：每测量窗口内一次活动奖励（单个大事务发全员）=====

	public void systemMailFanout() {
		long base = mailSeq.getAndAdd(cfg.players); // 号段事务外预分配
		int win = windowId;
		long t0 = System.nanoTime();
		if (TimelineLog.enabled) // 相位证据：fanout 起止时刻（每窗口一次的全员邮件）
			TimelineLog.event("fanout start win=" + win);
		// 事务划分原则：每玩家一个微事务（插自己的邮件行+自己的账本/通知）——
		// 单大事务持有全表读写集数百 ms（p40k 实测 fanout 78ms→数百 ms），与全服
		// mailClaim/mailSend/tRole 写互撞推高 redo，且是 checkpoint 巨型 RRS 的物质基础
		// （flush 解剖：tMail 24.7k 行占脏记录 80%+）。拆分后每事务自洽：守恒两侧
		// （邮件行+ledger 注入）同事务，部分失败不影响其余（op 计 errors 总账）。
		long errors = 0;
		for (long roleId = 0; roleId < cfg.players; roleId++) {
			final long mailKey = base + roleId;
			final long r = roleId;
			long txnT0 = System.nanoTime();
			long rc = zeze.newProcedure(() -> {
				var m = tMail.getOrAdd(mailKey);
				m.setFrom(0);
				m.setMoney(50);
				m.getItems().clear();
				var item = new demo.Game.BItem();
				item.setItemId(GameConfig.ItemPotion);
				item.setCount(2);
				item.setAddHp(30);
				item.setPrice(5);
				m.getItems().put(0, item);
				// 外部副作用（计数/账本/邮箱通知）登记到提交点：redo 轮作废重登记，仅最终提交轮执行
				Zeze.Transaction.Transaction.whileCommit(() -> {
					mailsInserted.increment();
					ledger.money(50);
					ledger.item(GameConfig.ItemPotion, 2);
					onlineNotify(r, mailKey);
				});
				return 0L;
			}, "GameSim.MailFanout").call();
			// 统计按真实事务数：每微事务 record（计数+单事务延迟）——与其他 op 的
			// "单事务延迟分位"口径统一（旧单大事务记全程是特例）；全程耗时见 TimelineLog 相位打点
			if (rc == 0)
				sysRecorder.record(OpRecorder.OpType.mailFanout, win, txnT0);
			else
				++errors;
		}
		if (TimelineLog.enabled)
			TimelineLog.event("fanout end win=" + win + " us=" + (System.nanoTime() - t0) / 1000 + " errors=" + errors);
		if (errors != 0)
			sysRecorder.error(OpRecorder.OpType.mailFanout);
	}

	// ===== 系统批量补偿：每测量窗口一次，bulkRewardPlayers 名玩家各一笔单事务 putAll 批量入库 =====
	// 持久化容器后端对照的批量写负载（LogMap1/2.putAll → 底层 plusAll 单发，kt 循环/ktt builder
	// 的分岔点）；物品注入走影子账本（同 mailFanout 的 whileCommit 口径），槽位 WarehouseSlotBase+
	// 逐窗口递增，背包随之增长形成大容器档（常规 op 只碰 0..BagSlots-1，互不干扰）。

	public void systemBulkReward(int windowId) {
		int win = windowId;
		long t0 = System.nanoTime();
		int slots = cfg.bulkRewardSlots;
		int players = Math.min(cfg.bulkRewardPlayers, cfg.players);
		int base = GameConfig.WarehouseSlotBase + win * slots;
		long errors = 0;
		for (int i = 0; i < players; i++) {
			final long roleId = i * (long)cfg.players / players; // 均匀散布
			// 定参事务外构建（重试不重耗；bean 未挂接前 putAll 进包合法）
			final var items = new java.util.HashMap<Integer, demo.Game.BItem>(slots * 2);
			final var inject = new java.util.HashMap<Integer, Long>();
			for (int s = 0; s < slots; s++) {
				int itemId = s % 3 + 1; // 药水/武器/护甲轮转
				var item = new demo.Game.BItem();
				item.setItemId(itemId);
				item.setCount(1);
				item.setPrice(5);
				items.put(base + s, item);
				inject.merge(itemId, 1L, Long::sum);
			}
			long txnT0 = System.nanoTime();
			long rc = zeze.newProcedure(() -> {
				var role = tRole.get(roleId);
				if (role != null)
					role.getBag().putAll(items);
				Zeze.Transaction.Transaction.whileCommit(() -> {
					for (var e : inject.entrySet())
						ledger.item(e.getKey(), e.getValue());
				});
				return 0L;
			}, "GameSim.BulkReward").call();
			if (rc == 0)
				sysRecorder.record(OpRecorder.OpType.bulkReward, win, txnT0);
			else
				++errors;
		}
		if (TimelineLog.enabled)
			TimelineLog.event("bulkReward end win=" + win + " us=" + (System.nanoTime() - t0) / 1000 + " errors=" + errors);
		if (errors != 0)
			sysRecorder.error(OpRecorder.OpType.bulkReward);
	}

	/** P2P 发信方提交后调用；系统邮件同样走这里 */
	public void onlineNotify(long roleId, long mailKey) {
		var p = online.get(roleId);
		if (p != null)
			p.inbox.add(new PlayerSim.MailNotify(mailKey));
	}

	public void addMailClaimed() {
		mailsClaimed.increment();
	}

	/** P2P 发信提交后计数（fanout 已内部计数） */
	public void addMailInserted() {
		mailsInserted.increment();
	}

	/** 邮件对账口径：inserted − claimed == 存量行（守恒#5） */
	public long mailsInserted() {
		return mailsInserted.sum();
	}

	public long mailsClaimed() {
		return mailsClaimed.sum();
	}

	/** 已分配邮件号段上界（验证扫表用；含分配后放弃的空洞） */
	public long mailSeqValue() {
		return mailSeq.get();
	}

	public long allocMailKey() {
		return mailSeq.getAndIncrement();
	}
}
