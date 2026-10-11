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
		if (TimelineLog.enabled) // 相位证据：fanout 起止时刻（每窗口一次的大事务）
			TimelineLog.event("fanout start win=" + win);
		long rc = zeze.newProcedure(() -> {
			for (long roleId = 0; roleId < cfg.players; roleId++) {
				var m = tMail.getOrAdd(base + roleId);
				m.setFrom(0);
				m.setMoney(50);
				m.getItems().clear();
				var item = new demo.Game.BItem();
				item.setItemId(GameConfig.ItemPotion);
				item.setCount(2);
				item.setAddHp(30);
				item.setPrice(5);
				m.getItems().put(0, item);
			}
			// 外部副作用（计数/账本/邮箱通知）登记到提交点：redo 轮作废重登记，仅最终提交轮执行
			Zeze.Transaction.Transaction.whileCommit(() -> {
				mailsInserted.add(cfg.players);
				ledger.money(50L * cfg.players);
				ledger.item(GameConfig.ItemPotion, 2L * cfg.players);
				for (long roleId = 0; roleId < cfg.players; roleId++)
					onlineNotify(roleId, base + roleId); // 键 base+roleId
			});
			return 0L;
		}, "GameSim.MailFanout").call();
		sysRecorder.record(OpRecorder.OpType.mailFanout, win, t0);
		if (TimelineLog.enabled)
			TimelineLog.event("fanout end win=" + win + " us=" + (System.nanoTime() - t0) / 1000 + " rc=" + rc);
		if (rc != 0)
			sysRecorder.error(OpRecorder.OpType.mailFanout);
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
