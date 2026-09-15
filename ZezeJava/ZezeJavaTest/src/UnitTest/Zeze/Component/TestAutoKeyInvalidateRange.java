package UnitTest.Zeze.Component;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AutoKey抬表水位（setSeed/setMinId/increaseSeed）必须同时失效内存号段：
 * 合服导数据后setMinId，后续nextId()必须从新水位批段、结果不小于minId
 * （教程《AutoKey 全局发号》§3.4承诺）；失败路径（不大于现水位）不失效，旧段继续连号。
 * 另：水位受id编码预算约束（serverId前缀+seed变长≤8字节），越界setSeed拒绝、发满明确报耗尽。
 */
@Fast
public class TestAutoKeyInvalidateRange {
	// Application并发需要不同serverId：本地RocksCache（zeze_cache_<serverId>）每serverId一份，
	// @Fast类并行时共用会撞RocksDB锁；Memory库按DatabaseUrl分桶，同样需要独占url。
	// 从200起避开TakeoverTestEnv的100段。
	private static final AtomicInteger NextServerId = new AtomicInteger(200);

	private Application app;

	@BeforeEach
	public void testInit() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		int serverId = NextServerId.getAndIncrement();
		conf.setServerId(serverId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("autokey_test_" + serverId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf); // Memory库，独立url=独立存储
		app = new Application("TestAutoKeyInvalidateRange", conf);
		app.start();
	}

	@AfterEach
	public void testCleanup() throws Exception {
		app.stop();
	}

	// 与TestAutoKey.makeId一致：按当前serverId的编码规则还原seed对应的id。
	private long makeId(long seed) {
		var bb = ByteBuffer.Allocate(8);
		var serverId = app.getConfig().getServerId();
		Assertions.assertTrue(serverId >= 0);
		if (serverId > 0)
			bb.WriteUInt(serverId);
		bb.WriteULong(seed);
		Assertions.assertTrue(bb.size() <= 8);
		return ByteBuffer.ToLongBE(bb.Bytes, bb.ReadIndex, bb.size());
	}

	@Test
	public void test1_setMinIdInvalidatesRange() {
		var autoKey = app.getAutoKey("test1");
		var id1 = autoKey.nextId(); // 批出第一段并消耗1个，此后内存持有未耗尽的旧段
		Assertions.assertTrue(id1 > 0);
		// 模拟合服导数据：老服存量id已远超本段，把minId抬到旧段可能发出的id之上
		var minId = makeId(1_000_000);
		Assertions.assertTrue(autoKey.setMinId(minId), "高于现水位的minId应成功");
		// 教程§3.4承诺：setMinId之后nextId()的结果不小于此值。
		// 修复前：内存旧段继续发号（本例会发出makeId(2)），与迁移进来的存量id重号。
		var id2 = autoKey.nextId();
		Assertions.assertTrue(id2 >= minId, "setMinId后nextId必须>=minId, actual=" + id2);
		var id3 = autoKey.nextId();
		Assertions.assertEquals(id2 + 1, id3, "新段内应连号");
		Assertions.assertTrue(autoKey.getSeed() > 1_000_000, "表水位应已抬到minId之上");
	}

	@Test
	public void test2_setSeedInvalidatesRange() {
		var autoKey = app.getAutoKey("test2");
		autoKey.nextId(); // 建立内存号段
		var watermark = autoKey.getSeed(); // 首段批出后的表水位（段尾）
		var newSeed = watermark + 5_000_000;
		Assertions.assertTrue(autoKey.setSeed(newSeed));
		// 修复前：继续消耗旧段（makeId(2)），与抬高的存量seed区间重号
		Assertions.assertEquals(makeId(newSeed + 1), autoKey.nextId(), "setSeed后应从新水位批段");
		Assertions.assertEquals(makeId(newSeed + 2), autoKey.nextId(), "新段内应连号");
	}

	@Test
	public void test3_increaseSeedInvalidatesRange() {
		var autoKey = app.getAutoKey("test3");
		autoKey.nextId(); // 建立内存号段
		var watermark = autoKey.getSeed();
		var delta = 1000;
		Assertions.assertTrue(autoKey.increaseSeed(delta));
		// 修复前：继续消耗旧段（makeId(2)），与抬高的存量seed区间重号
		Assertions.assertEquals(makeId(watermark + delta + 1), autoKey.nextId(), "increaseSeed后应从新水位批段");
	}

	@Test
	public void test4_failPathKeepsRange() {
		var autoKey = app.getAutoKey("test4");
		var id1 = autoKey.nextId();
		var id2 = autoKey.nextId();
		Assertions.assertEquals(id1 + 1, id2);
		// 反推不出超过现表水位（段尾16）的seed：setMinId算出seed=0、setSeed给现水位本身，
		// 都只能失败返回false，不得失效现役号段
		Assertions.assertFalse(autoKey.setMinId(makeId(0)), "推不出更高seed的minId应返回false");
		Assertions.assertFalse(autoKey.setSeed(autoKey.getSeed()), "等于现水位的seed应返回false");
		var id3 = autoKey.nextId();
		Assertions.assertEquals(id2 + 1, id3, "失败路径不失效号段：旧段继续连号");
	}

	// 与AutoKey.maxSeedForServerId同一公式：serverId前缀+seed变长编码（每字节7位）总长≤8字节；
	// serverId==0或≥0x80时8字节总长首字节≥0x80会被ToLongBE读成负数，保守少用1字节保id恒正
	private static long maxSeedFor(int serverId) {
		int n = 8 - (serverId > 0 ? ByteBuffer.WriteUIntSize(serverId) : 0);
		if (serverId == 0 || serverId >= 0x80)
			n--;
		return (1L << (7 * n)) - 1;
	}

	// 契约压力验证：任何在setSeed成功返回之后发起的nextId()，结果必须不小于该次水位。
	// clock内的tick单调递增；floor为“已完成抬水位”的门槛makeId(watermark+1)。发号线程在发起
	// 调用前于clock内快照(tick,floor)，只有t0晚于某次抬水位完成时刻的调用才受其约束检查。
	@Test
	public void test5_concurrentRaiseNoStaleId() throws Exception {
		var autoKey = app.getAutoKey("test5");
		autoKey.nextId(); // 建立内存号段
		final var clock = new Object();
		final var tick = new long[1];
		final var floor = new long[1]; // 0=尚无已完成的水位
		var stop = new AtomicBoolean(false);
		var failures = new ConcurrentLinkedQueue<String>();
		var threads = new Thread[4];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(() -> {
				try {
					while (!stop.get()) {
						long floorId;
						synchronized (clock) {
							++tick[0];
							floorId = floor[0];
						}
						var id = autoKey.nextId();
						if (floorId > 0 && id < floorId) {
							failures.add("nextId取到旧段号: id=" + id + " < 水位门槛" + floorId);
							return;
						}
					}
				} catch (Throwable e) {
					failures.add("unexpected: " + e);
				}
			});
			threads[i].start();
		}
		// 抬水位步长要小：每次setSeed成功会失效号段，压测线程随即走慢路径重批段，
		// 自适应批大小(fund)会倍增，水位被“抬升+重批”快速推高；并设保护上限远离编码预算
		var raiseLimit = maxSeedFor(app.getConfig().getServerId()) - 1_000_000_000L;
		for (int i = 0; i < 20 && failures.isEmpty(); i++) {
			long w = autoKey.getSeed() + 1_000_000L;
			while (w <= raiseLimit && !autoKey.setSeed(w))
				w = autoKey.getSeed() + 1_000_000L;
			if (w > raiseLimit)
				break;
			synchronized (clock) {
				++tick[0];
				floor[0] = makeId(w + 1);
			}
			//noinspection BusyWait
			Thread.sleep(2);
		}
		stop.set(true);
		for (var t : threads)
			t.join(10_000);
		Assertions.assertTrue(failures.isEmpty(), () -> "契约被打破: " + String.join("; ", failures));
	}

	// 编码预算：越界setSeed拒绝（旧代码setSeed(Long.MAX_VALUE-10)会成功，下次批段
	// end=start+count回绕成负数持久化进表，之后nextId()永久异常——水位投毒）；
	// 段尾夹在预算内，最后一个可编码号发完后明确报耗尽。
	@Test
	public void test6_budgetClampAndExhaustion() {
		var autoKey = app.getAutoKey("test6");
		var maxSeed = maxSeedFor(app.getConfig().getServerId());
		Assertions.assertTrue(maxSeed > 1_000_000, "测试环境的serverId应留出足够的seed预算");
		Assertions.assertThrows(IllegalStateException.class, () -> autoKey.setSeed(maxSeed + 1));
		Assertions.assertThrows(IllegalStateException.class, () -> autoKey.setSeed(Long.MAX_VALUE));
		var id1 = autoKey.nextId(); // 投毒被拒后发号应完全正常
		Assertions.assertTrue(id1 > 0);
		Assertions.assertTrue(autoKey.setSeed(maxSeed - 3));
		Assertions.assertEquals(makeId(maxSeed - 2), autoKey.nextId(), "段尾应夹在maxSeed内");
		Assertions.assertEquals(makeId(maxSeed - 1), autoKey.nextId());
		Assertions.assertEquals(makeId(maxSeed), autoKey.nextId(), "最后一个可编码号");
		Assertions.assertThrows(IllegalStateException.class, autoKey::nextId, "发满预算应明确报耗尽");
	}

	// 与nextByteBuffer同一编码：serverId前缀+seed变长，大端读long；总长>8时返回Long.MIN_VALUE
	private static long encode(int serverId, long seed) {
		int size = ByteBuffer.WriteULongSize(seed);
		if (serverId > 0)
			size += ByteBuffer.WriteUIntSize(serverId);
		if (size > 8)
			return Long.MIN_VALUE;
		var bb = ByteBuffer.Allocate(size);
		if (serverId > 0)
			bb.WriteUInt(serverId);
		bb.WriteULong(seed);
		return ByteBuffer.ToLongBE(bb.Bytes, 0, bb.WriteIndex);
	}

	// maxSeed公式的边界穷举核对（对代表性serverId，在每个变长档位边界2^7..2^56邻域+maxSeed±1）：
	// (a)一切 s<=maxSeed 编码总长<=8 且 ToLongBE>0（正确性）；
	// (b)一切 s>maxSeed 越界或转负（公式恰好取在合法区间上，无浪费也没越界——紧贴性）。
	// 合法性对seed向下封闭（size单调增、符号只在总长恰8时由首字节决定），双向断言即钉死整个边界。
	@Test
	public void test7_maxSeedFormulaTight() {
		int[] serverIds = {0, 1, 2, 0x7f, 0x80, 0x81, 200, 0x3fff, 0x4000, 0x1f_ffff, 0x20_0000,
				0x0fff_ffff, 0x1000_0000, Integer.MAX_VALUE};
		for (int serverId : serverIds) {
			var maxSeed = maxSeedFor(serverId);
			Assertions.assertTrue(maxSeed > 0, "serverId=" + serverId);
			var probes = new java.util.TreeSet<Long>();
			probes.add(1L); // seed=0不会被发放（Range发[start+1,end]），不参与“恒正”断言
			for (int k = 1; k <= 8; k++) { // 每个变长档位边界及其邻域
				long p = 1L << (7 * k);
				probes.add(p - 2);
				probes.add(p - 1);
				probes.add(p);
			}
			probes.add(maxSeed - 1);
			probes.add(maxSeed);
			probes.add(maxSeed + 1);
			for (var s : probes) {
				if (s < 0)
					continue;
				var id = encode(serverId, s);
				if (s <= maxSeed)
					Assertions.assertTrue(id > 0,
							"正确性破坏: serverId=" + serverId + ", seed=" + s + ", id=" + id);
				else
					Assertions.assertTrue(id < 0,
							"紧贴性破坏: serverId=" + serverId + ", seed=" + s + " 应越界或为负, id=" + id);
			}
		}
	}
}
