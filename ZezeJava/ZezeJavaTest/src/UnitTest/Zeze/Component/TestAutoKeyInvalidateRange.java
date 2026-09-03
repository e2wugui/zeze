package UnitTest.Zeze.Component;

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
}
