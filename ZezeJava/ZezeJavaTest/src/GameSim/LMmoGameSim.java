package GameSim;

import java.util.Objects;
import harness.Bench;
import harness.MacroBench;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeSet;

/**
 * L 场景：MMO-lite 游戏模拟（真实游戏逻辑 + 虚拟线程玩家 + K=10 节奏；
 * 负载主旋钮=在线数，单玩家事务串行无自我并发，见 GameConfig 负载模型注记）。
 *
 * <p>时长窗口制：世界连续运行，窗口=测量区间（预热窗口不计）；玩家操作以发起时窗口归属，
 * 窗口结束翻转后留宽限期再收割。指标=总事务吞吐中位数（同日 A/B、3×噪声协议沿用）
 * + 每操作类型延迟分位（flat key 留档，BenchCompare 直接可配对）。
 *
 * <p>五条守恒律在停机后单只读事务全量对账（影子账本只记系统注入/销毁）。
 */
@SuppressWarnings("NewClassNamingConvention")
@Bench
@Tag("game")
public class LMmoGameSim {
	/** 窗口结束后的收割宽限（墙钟 ms）：让跨界在飞样本落完再收割旧槽 */
	private static final long HarvestGraceMs = 2_000;

	@Test
	@Timeout(3600) // K=4 窗口墙钟 150s×4轮≈10min，超 junit-platform.properties 全局 300s 护栏：
	// IDEA Run 不经 gradle 车道的 systemProperty 覆盖，注解优先级最高、两车道通吃；
	// 全局 300s 护栏（test 车道防挂死）不动。
	public void testBenchmark() throws Exception {
		var cfg = new GameConfig();
		OpRecorder.init(cfg.players);
		var world = new GameWorld(cfg);
		world.start();
		var threads = new Thread[cfg.players];
		try {
			try {
				for (int i = 0; i < cfg.players; i++)
					threads[i] = Thread.ofVirtual().name("gs-player-" + i).start(new PlayerSim(world, i));
				for (int w = 0; w < cfg.warmups; w++)
					runWindow(world, cfg, 0); // 预热：世界继续跑，统计静默
				// 后端对照内存口径：预热后驻留（数据装载完成的稳态基线）与测量轮 GC 压力
				long residentAfterWarmup = usedHeapAfterGc();
				var gcBefore = gcStats();
				var tps = new double[cfg.rounds];
				var opCountPerRound = new long[cfg.rounds][];
				// 每轮各 op 分位（轮中位数聚合进 JSON）
				var statsPerRound = new OpRecorder.OpStats[cfg.rounds][];
				for (int w = 1; w <= cfg.rounds; w++) {
					var before = OpRecorder.countsSnapshot();
					long t0 = System.nanoTime();
					runWindow(world, cfg, w);
					long tFlip = System.nanoTime();
					Thread.sleep(HarvestGraceMs);
					statsPerRound[w - 1] = OpRecorder.harvest(w);
					var after = OpRecorder.countsSnapshot();
					opCountPerRound[w - 1] = diff(before, after);
					long count = Arrays.stream(opCountPerRound[w - 1]).sum();
					tps[w - 1] = count / ((tFlip - t0) / 1e9);
					reportRound(w, count, tps[w - 1], statsPerRound[w - 1]);
				}
				var gcAfter = gcStats();
				long residentSteady = usedHeapAfterGc();
				// 冷装载回环（只读，不影响其后的守恒对账）：Memory 库无磁盘装载，回环即 PMap2 装载形态
				double coldLoadLastMs = coldLoad(world, cfg);
				writeJson(cfg, tps, opCountPerRound, statsPerRound,
						residentAfterWarmup, residentSteady, gcAfter[0] - gcBefore[0], gcAfter[1] - gcBefore[1], coldLoadLastMs);
				var errors = OpRecorder.errorTotals();
				long errTotal = Arrays.stream(errors).sum();
				Assertions.assertEquals(0, errTotal, "游戏事务非零返回/异常: " + Arrays.toString(errors));
			} finally {
				world.shutdown(); // 停世界但不停库：玩家登出/对账事务还需要它
				for (var t : threads)
					t.join(10_000);
			}
			verifyConservation(world, cfg);
			clearAll(world, cfg);
		} finally {
			world.stop();
		}
	}

	/** 一个窗口：翻窗口号、调度系统邮件、睡满窗口墙钟后翻到下一窗口 */
	private static void runWindow(GameWorld world, GameConfig cfg, int windowId) throws Exception {
		world.windowId = windowId;
		if (TimelineLog.enabled)
			TimelineLog.event("window start id=" + windowId);
		// 系统邮件 fan-out：窗口内偏移时刻触发（预热窗口也触发保持邮件经济；统计口径以 world.windowId 为准）。
		// 休眠旋钮 game.fanoutOff（默认 false=基线）：裁决反事实 B 静默 fanout 专用
		if (!cfg.fanoutOff) {
			Thread.ofVirtual().start(() -> {
				try {
					Thread.sleep(cfg.wall(GameConfig.MailFanoutAtMs));
					if (!world.stopped)
						world.systemMailFanout();
				} catch (InterruptedException ignored) {
				}
			});
		}
		// 系统批量补偿：邮件相位之后触发（7min 偏移，不交叠）；预热窗口同样触发保持容器形态一致。
		// 旋钮 game.bulkRewardSlots（默认 0=关）：持久化容器后端对照的批量写负载
		if (cfg.bulkRewardSlots > 0) {
			Thread.ofVirtual().start(() -> {
				try {
					Thread.sleep(cfg.wall(GameConfig.BulkRewardAtMs));
					if (!world.stopped)
						world.systemBulkReward(world.windowId);
				} catch (InterruptedException ignored) {
				}
			});
		}
		Thread.sleep(cfg.wall(cfg.windowMinutes * 60_000L));
		world.windowId = windowId + 1; // 立即开下一窗口（连续世界）；宽限期在收割侧
		if (TimelineLog.enabled)
			TimelineLog.event("window flip to=" + (windowId + 1));
	}

	private static long[] diff(long[] before, long[] after) {
		var out = new long[before.length];
		for (int i = 0; i < before.length; i++)
			out[i] = after[i] - before[i];
		return out;
	}

	// ===== 后端对照的内存/GC 采集与冷装载 =====

	/** 双 gc 稳定后的堆驻留（相位间调用，暂停不影响测量窗） */
	private static long usedHeapAfterGc() {
		System.gc();
		try {
			Thread.sleep(200);
		} catch (InterruptedException ignored) {
		}
		System.gc();
		return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
	}

	/** 全体 GC bean 的 {次数, 累计毫秒} */
	private static long[] gcStats() {
		long count = 0, time = 0;
		for (var gc : ManagementFactory.getGarbageCollectorMXBeans()) {
			count += Math.max(0, gc.getCollectionCount());
			time += Math.max(0, gc.getCollectionTime());
		}
		return new long[]{count, time};
	}

	/** 冷装载回环：全量角色 bean encode→decode，每轮一只读事务。返回末轮耗时 ms（0=未启用） */
	private static double coldLoad(GameWorld world, GameConfig cfg) throws Exception {
		if (cfg.coldLoadRounds <= 0)
			return 0;
		long bytes = 0;
		double lastMs = 0;
		for (int r = 0; r < cfg.coldLoadRounds; r++) {
			long t0 = System.nanoTime();
			bytes = world.zeze.newProcedure(() -> {
				long sum = 0;
				for (long roleId = 0; roleId < cfg.players; roleId++) {
					var role = world.tRole.get(roleId);
					if (role == null)
						continue;
					var bb = Zeze.Serialize.ByteBuffer.Allocate();
					role.encode(bb);
					sum += bb.size();
					var fresh = new demo.Game.BRole(); // 解码对象仅作汇聚，防死码消除
					fresh.decode(bb);
				}
				return sum;
			}, "GameSim.ColdLoad").call();
			lastMs = (System.nanoTime() - t0) / 1e6;
			System.out.printf("L_MmoGameSim coldLoad round=%d ms=%.0f bytes=%,d (%.0f B/role)%n",
					r + 1, lastMs, bytes, (double)bytes / cfg.players);
		}
		return lastMs;
	}

	private static void reportRound(int w, long count, double tps, OpRecorder.OpStats[] stats) {
		System.out.printf("L_MmoGameSim round=%d txns=%d txn/s=%.0f | loot p50/p95=%.0f/%.0fus hb p95=%.0fus fq p95=%.0fus%n",
				w, count, tps, stats[OpRecorder.OpType.loot.ordinal()].p50,
				stats[OpRecorder.OpType.loot.ordinal()].p95,
				stats[OpRecorder.OpType.heartbeat.ordinal()].p95,
				stats[OpRecorder.OpType.friendQuery.ordinal()].p95);
	}

	// ===== JSON 留档（flat key：BenchCompare 按文件名配对、jsonDouble 直读）=====

	private void writeJson(GameConfig cfg, double[] tps, long[][] opCountPerRound,
						   OpRecorder.OpStats[][] statsPerRound,
						   long residentAfterWarmup, long residentSteady,
						   long gcCount, long gcTimeMs, double coldLoadLastMs) throws Exception {
		double median = MacroBench.median(tps);
		double mean = Arrays.stream(tps).average().orElseThrow();
		double variance = Arrays.stream(tps).map(v -> (v - mean) * (v - mean)).sum() / tps.length;
		double stdev = Math.sqrt(variance);
		// 后端入档名（bench 档案按文件名配对，防三后端互覆）；pcollections 档延续历史档名
		// L_MmoGameSim（与 2026-10-16 前档案可直接配对），kt/ktt（含默认）带后缀
		var backend = Zeze.Collections.Persistents.BACKEND;
		var benchName = "L_MmoGameSim" + (backend.equals("pcollections") ? "" : "_" + backend);
		var sb = new StringBuilder(4096);
		sb.append("{\n");
		sb.append("  \"name\": \"").append(benchName).append("\",\n");
		sb.append("  \"backend\": \"").append(backend).append("\",\n");
		sb.append("  \"git\": \"").append(MacroBench.git()).append("\",\n");
		sb.append("  \"timestamp\": \"").append(Instant.now()).append("\",\n");
		sb.append("  \"jdk\": \"").append(System.getProperty("java.version")).append("\",\n");
		sb.append("  \"processors\": ").append(Runtime.getRuntime().availableProcessors()).append(",\n");
		sb.append("  \"game\": {\"players\": ").append(cfg.players)
				.append(", \"k\": ").append(cfg.timeScaleK)
				.append(", \"windowMinutes\": ").append(cfg.windowMinutes)
				.append(", \"warmups\": ").append(cfg.warmups)
				.append(", \"rounds\": ").append(cfg.rounds)
				.append(", \"bulkRewardSlots\": ").append(cfg.bulkRewardSlots)
				.append(", \"bulkRewardPlayers\": ").append(cfg.bulkRewardPlayers)
				.append(", \"coldLoadRounds\": ").append(cfg.coldLoadRounds).append("},\n");
		sb.append(String.format("  \"residentAfterWarmupBytes\": %d,%n", residentAfterWarmup));
		sb.append(String.format("  \"residentSteadyBytes\": %d,%n", residentSteady));
		sb.append(String.format("  \"gcCountRounds\": %d,%n", gcCount));
		sb.append(String.format("  \"gcTimeMsRounds\": %d,%n", gcTimeMs));
		sb.append(String.format("  \"coldLoadLastMs\": %.1f,%n", coldLoadLastMs));
		sb.append("  \"rounds\": [").append(Arrays.toString(tps)).append("],\n");
		sb.append("  \"median\": ").append(median).append(",\n");
		sb.append("  \"mean\": ").append(mean).append(",\n");
		sb.append("  \"min\": ").append(Arrays.stream(tps).min().orElseThrow()).append(",\n");
		sb.append("  \"max\": ").append(Arrays.stream(tps).max().orElseThrow()).append(",\n");
		sb.append("  \"stdev\": ").append(stdev).append(",\n");
		// 每 op：轮中位数的分位/均值 + 总次数/速率（flat key，op.<name>.<field>；
		// 显式 \n 而非 %n——Windows 下 %n=\r\n 会破坏尾部去逗号的两字符裁剪）
		for (var op : OpRecorder.OpType.values()) {
			int i = op.ordinal();
			double cntMedian = medianOfCounts(opCountPerRound, i);
			sb.append(String.format("  \"op.%s.count\": %.0f,\n", op, cntMedian));
			sb.append(String.format("  \"op.%s.tps\": %.1f,\n", op, cntMedian / (cfg.windowMinutes * 60.0)));
			sb.append(String.format("  \"op.%s.mean_us\": %.1f,\n", op, medianOf(statsPerRound, i, s -> s.mean)));
			sb.append(String.format("  \"op.%s.p50_us\": %.1f,\n", op, medianOf(statsPerRound, i, s -> s.p50)));
			sb.append(String.format("  \"op.%s.p95_us\": %.1f,\n", op, medianOf(statsPerRound, i, s -> s.p95)));
			sb.append(String.format("  \"op.%s.p99_us\": %.1f,\n", op, medianOf(statsPerRound, i, s -> s.p99)));
			sb.append(String.format("  \"op.%s.max_us\": %.1f,\n", op, medianOf(statsPerRound, i, s -> s.max)));
		}
		sb.setLength(sb.length() - 2); // 去尾逗号换行
		sb.append("\n}\n");
		MacroBench.writeRawJson(benchName, sb.toString());
		System.out.printf("L_MmoGameSim txns/s median=%.0f mean=%.0f stdev=%.0f cv=%.4f%n",
				median, mean, stdev, stdev / mean);
	}

	private static double medianOfCounts(long[][] perRound, int opIdx) {
		var xs = new double[perRound.length];
		for (int r = 0; r < perRound.length; r++)
			xs[r] = perRound[r][opIdx];
		return MacroBench.median(xs);
	}

	private interface StatField {
		double of(OpRecorder.OpStats s);
	}

	private static double medianOf(OpRecorder.OpStats[][] perRound, int opIdx, StatField f) {
		var xs = new double[perRound.length];
		for (int r = 0; r < perRound.length; r++)
			xs[r] = f.of(perRound[r][opIdx]);
		return MacroBench.median(xs);
	}

	// ===== 五条守恒律：停机后单只读事务全量对账 =====

	private void verifyConservation(GameWorld world, GameConfig cfg) {
		var moneyRole = new long[1];
		var moneyMail = new long[1];
		var itemsRole = new HashMap<Integer, Long>();
		var itemsMail = new HashMap<Integer, Long>();
		var damageKeys = new ArrayList<long[]>(); // [damageTotal, roleId]
		var levelKeys = new ArrayList<long[]>();  // [level, roleId]（仅升过级的）
		var mailRows = new long[1];
		var sessionRows = new long[1];
		var roleMismatches = new ArrayList<String>();
		long rc = world.zeze.newProcedure(() -> {
			for (long r = 0; r < cfg.players; r++) {
				var role = world.tRole.get(r);
				var session = world.tSession.get(r);
				if (session != null)
					++sessionRows[0];
				if (role == null)
					continue;
				moneyRole[0] += role.getMoney();
				// 逐角色对账（建号补贴+全流净变动==DB；含转移流）。曾出现全局差 100 的罕见
				// 竞态一次（未复现），此网复发时带 roleId 定位
				long roleExpect = world.ledger.roleMoneyOf(r);
				if (roleExpect != role.getMoney() && roleMismatches.size() < 5)
					roleMismatches.add("role=" + r + " ledger=" + roleExpect + " db=" + role.getMoney());
				mergeItem(itemsRole, role.getBag());
				mergeItem(itemsRole, role.getEquip());
				if (role.getDamageTotal() > 0)
					damageKeys.add(new long[]{role.getDamageTotal(), r});
				if (role.getLevel() > 1)
					levelKeys.add(new long[]{role.getLevel(), r});
			}
			for (long k = 0; k < world.mailSeqValue(); k++) {
				var m = world.tMail.get(k);
				if (m == null)
					continue;
				++mailRows[0];
				moneyMail[0] += m.getMoney();
				for (var e : m.getItems().entrySet())
					itemsMail.merge(e.getValue().getItemId(), (long)e.getValue().getCount(), Long::sum);
			}
			return 0L;
		}, "GameSim.Verify").call();
		Assertions.assertEquals(0, rc, "对账事务失败");
		var ledger = world.ledger;

		// #0 逐角色货币对账（比全局更强的网）
		Assertions.assertTrue(roleMismatches.isEmpty(), "角色货币对账失配: " + roleMismatches);
		// #1 货币守恒
		Assertions.assertEquals(ledger.moneyNet(), moneyRole[0] + moneyMail[0],
				"货币守恒: ledger=" + ledger.moneyNet() + " role+mail=" + (moneyRole[0] + moneyMail[0]));
		// #2 物品守恒（逐物品 id）
		var ids = new TreeSet<Integer>();
		ids.addAll(itemsRole.keySet());
		ids.addAll(itemsMail.keySet());
		ids.addAll(ledger.items().keySet());
		for (int id : ids) {
			long expect = ledger.itemNet(id);
			long actual = itemsRole.getOrDefault(id, 0L) + itemsMail.getOrDefault(id, 0L);
			Assertions.assertEquals(expect, actual, "物品守恒 itemId=" + id);
		}
		// #3 会话无泄漏（全部玩家已登出）
		Assertions.assertEquals(0, sessionRows[0], "会话残留");
		Assertions.assertTrue(world.online.isEmpty(), "在线注册残留");
		// #5 邮件对账
		Assertions.assertEquals(world.mailsInserted() - world.mailsClaimed(), mailRows[0],
				"邮件对账: inserted-claimed=" + (world.mailsInserted() - world.mailsClaimed()) + " rows=" + mailRows[0]);

		// #4 榜单一致（Rank 模块口径：伤害榜=DamageTotal、等级榜=升过级角色的 level，容量 100）
		assertRank(world, world.damageBoard, damageKeys, "damage");
		assertRank(world, world.levelBoard, levelKeys, "level");
	}

	private static void mergeItem(HashMap<Integer, Long> into,
								  Zeze.Transaction.Collections.PMap2<Integer, demo.Game.BItem> bag) {
		for (var e : bag.entrySet())
			into.merge(e.getValue().getItemId(), (long)e.getValue().getCount(), Long::sum);
	}

	/** Rank 模块榜单语义验证：榜条数==min(100,候选)；榜内分值==角色表现值；
	 * 高于截断分的条数与重算一致（同分按模块插入序进榜，不做集合级比较） */
	private static void assertRank(GameWorld world, Zeze.Builtin.Game.Rank.BConcurrentKey board,
								   ArrayList<long[]> scoreRole, String what) {
		scoreRole.sort((a, b) -> {
			int c = Long.compare(b[0], a[0]); // 分数降序
			return c != 0 ? c : Long.compare(a[1], b[1]);
		});
		int expectCount = Math.min(100, scoreRole.size());
		long cutoff = scoreRole.isEmpty() ? 0 : scoreRole.get(expectCount - 1)[0];
		var entries = new ArrayList<long[]>(); // [roleId, score]
		world.zeze.newProcedure(() -> {
			for (var e : Objects.requireNonNull(world.rank.getRankTotal(board).getTableValue()).getRankListReadOnly()) {
				var score = ((Zeze.Builtin.Game.Rank.BValueLongReadOnly)e.getDynamicReadOnly().getBean()).getValue();
				entries.add(new long[]{e.getRoleId(), score});
			}
			return 0L;
		}, "GameSim.VerifyRank").call();
		Assertions.assertEquals(expectCount, entries.size(), what + " 榜条数");
		var roleScore = new HashMap<Long, Long>();
		for (var kv : scoreRole)
			roleScore.put(kv[1], kv[0]);
		long aboveExpect = 0;
		for (var sr : scoreRole)
			if (sr[0] > cutoff)
				++aboveExpect;
		long aboveActual = 0;
		for (var e : entries) {
			Assertions.assertEquals(roleScore.get(e[0]), e[1],
					what + " 榜内分值须等于角色表现值 role=" + e[0]);
			if (e[1] > cutoff)
				++aboveActual;
		}
		Assertions.assertEquals(aboveExpect, aboveActual, what + " 高于截断分条数");
	}

	/** 清场（对齐 MacroBench 轮自包含精神：下场测试零残留） */
	private static void clearAll(GameWorld world, GameConfig cfg) {
		world.zeze.newProcedure(() -> {
			for (long r = 0; r < cfg.players; r++) {
				world.tRole.remove(r);
				world.tSession.remove(r);
			}
			for (long k = 0; k < world.mailSeqValue(); k++)
				world.tMail.remove(k);
			return 0L;
		}, "GameSim.Clear").call();
	}
}
