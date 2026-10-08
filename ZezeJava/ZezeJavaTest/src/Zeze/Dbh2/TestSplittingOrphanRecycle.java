package Zeze.Dbh2;

import harness.Extra;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Config;
import Zeze.Dbh2.Master.Master;
import Zeze.Dbh2.Master.MasterDatabase;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * splitting孤儿条目的世代登记与年龄扫描回收（FND29 dbh2-04）：
 * 准备段中断且重试中位键漂移后，旧世代条目与已建raft成为永久孤儿（死信消费仅同keyFirst碰撞
 * 触发、年龄扫描只观测）。修复=①创建时登记世代（时间戳+源桶raft身份，master可判定"谁的孩子"）；
 * ②年龄扫描升格为回收：超龄且结构判死（世代/INV1双保险）→ DestroyBucket回收managers侧raft
 * （幂等）+tombstone条目；超龄但结构存活（在途拷贝可超任何阈值）只告警不动作。
 * 本用例锁死master侧确定性可直构的形态：条目host2Raft为空时回收不触网（DestroyBucket空集即确认）。
 * 用例：①中位键漂移的旧世代孤儿（源桶区间已收窄、INV1方向不可见的M1>M2形态）被回收；
 * ②在途条目（源桶区间完整包含）超龄不回收；③无世代身份的存量条目按INV1判死回收；
 * ④回收幂等（重扫无残留）。
 */
@Fast
@Extra
public class TestSplittingOrphanRecycle {

	private static final String RaftA = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="testOrphanRecycleA">
				<node Host="127.0.0.1" Port="29101"/>
			</raft>
			""";
	private static final String RaftB = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="testOrphanRecycleB">
				<node Host="127.0.0.1" Port="29102"/>
			</raft>
			""";
	private static final String RaftC = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="testOrphanRecycleC">
				<node Host="127.0.0.1" Port="29103"/>
			</raft>
			""";

	private static Binary key(int i) {
		return new Binary(new byte[]{(byte)i});
	}

	private static BBucketMeta.Data newBucketMeta(Binary keyFirst, Binary keyLast, String raftConfig) {
		var bucket = new BBucketMeta.Data();
		bucket.setDatabaseName("db1");
		bucket.setTableName("t1");
		bucket.setRaftConfig(raftConfig);
		bucket.setKeyFirst(keyFirst);
		bucket.setKeyLast(keyLast);
		return bucket;
	}

	@SuppressWarnings("unchecked")
	private static MasterDatabase getDatabase(Master master) throws Exception {
		Field field = Master.class.getDeclaredField("databases");
		field.setAccessible(true);
		return ((ConcurrentHashMap<String, MasterDatabase>)field.get(master)).get("db1");
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<String, MasterTable.Data> getSplitting(MasterDatabase db) throws Exception {
		Field field = MasterDatabase.class.getDeclaredField("splitting");
		field.setAccessible(true);
		return (ConcurrentHashMap<String, MasterTable.Data>)field.get(db);
	}

	private static MasterTable.Data splittingTable(MasterDatabase db) throws Exception {
		return getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data());
	}

	// ①漂移旧世代孤儿（FND29 dbh2-04主案）：条目[k7,∞)创建时源桶=raftA[k2,∞)；后续世代
	// （raftB[k5,∞)）settle后源桶收窄为[k2,k5]——条目区间越出源桶现行区间=结构不可达。
	// INV1方向（floor更窄）对M1>M2形态不可见（floor(k7)=raftB[k5,∞)与条目同上界），
	// 只有世代判据能发现。
	@Test
	public void testDriftedOldGenerationRecycled(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var main = new MasterTable.Data();
			db.getTables().put("t1", main);
			main.getBuckets().put(key(2), newBucketMeta(key(2), Binary.Empty, RaftA));
			var orphan = newBucketMeta(key(7), Binary.Empty, RaftC);
			splittingTable(db).getBuckets().put(key(7), orphan);

			master.getDbh2Config().setSplittingAgeWarnMs(0);
			Assertions.assertEquals(0, db.scanSplittingAge(),
					"首扫只立基线+世代登记（源=raftA），不告警");
			Assertions.assertNotNull(db.getSplittingAgeCreateTime("t1", key(7)), "基线已立");

			// 主表演进为新世代settle后的形态：源桶raftA收窄到[k2,k5]，新桶raftB接管[k5,∞)。
			main.getBuckets().put(key(5), newBucketMeta(key(5), Binary.Empty, RaftB));
			main.getBuckets().get(key(2)).setKeyLast(key(5));

			Assertions.assertEquals(1, db.scanSplittingAge(), "超龄1条");
			Assertions.assertNull(splittingTable(db).getBuckets().get(key(7)),
					"漂移旧世代孤儿必须被回收（世代判死，INV1不可见形态）");
			Assertions.assertNull(db.getSplittingAgeCreateTime("t1", key(7)),
					"回收时年龄记录同步清理");
			Assertions.assertEquals(0, db.scanSplittingAge(), "重扫无残留（回收幂等收敛）");
		} finally {
			master.close();
		}
	}

	// ②在途保护：源桶现行区间完整包含条目区间（真实在途拷贝形态，超大桶可拷贝超任何阈值）
	// ——超龄只告警，永不按年龄回收。
	@Test
	public void testInFlightEntrySurvives(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var main = new MasterTable.Data();
			db.getTables().put("t1", main);
			main.getBuckets().put(key(2), newBucketMeta(key(2), key(9), RaftA));
			var inFlight = newBucketMeta(key(5), key(9), RaftC);
			splittingTable(db).getBuckets().put(key(5), inFlight);

			master.getDbh2Config().setSplittingAgeWarnMs(0);
			Assertions.assertEquals(0, db.scanSplittingAge(), "首扫立基线（源=raftA[k2,k9]包含条目）");
			Assertions.assertEquals(1, db.scanSplittingAge(), "超龄告警1条");
			Assertions.assertSame(inFlight, splittingTable(db).getBuckets().get(key(5)),
					"结构存活的条目不得回收（判死必须结构驱动，年龄只是门槛）");
			Assertions.assertNotNull(db.getSplittingAgeCreateTime("t1", key(5)), "条目与年龄记录都保留");
			Assertions.assertEquals(1, db.scanSplittingAge(), "再次扫描仍只告警");
			Assertions.assertSame(inFlight, splittingTable(db).getBuckets().get(key(5)),
					"反复扫描不得动作存活条目");
		} finally {
			master.close();
		}
	}

	// ③无世代身份的存量条目：登记时源不包含（或旧格式8字节记录）→退回INV1判据——
	// floor比条目更窄=更晚的settle已收窄（consumeDeadSplittingOnCollision同判据）。
	@Test
	public void testLegacyInv1DeadRecycled(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var main = new MasterTable.Data();
			db.getTables().put("t1", main);
			main.getBuckets().put(key(2), newBucketMeta(key(2), key(5), RaftA));
			var dead = newBucketMeta(key(5), key(9), RaftC);
			splittingTable(db).getBuckets().put(key(5), dead);

			master.getDbh2Config().setSplittingAgeWarnMs(0);
			Assertions.assertEquals(0, db.scanSplittingAge(),
					"首扫立基线（floor[k2,k5]不包含[k5,k9]，不登记世代身份）");
			Assertions.assertEquals(1, db.scanSplittingAge(), "超龄1条");
			Assertions.assertNull(splittingTable(db).getBuckets().get(key(5)),
					"INV1判死（floor.keyLast=k5<k9）的存量条目必须被回收");
			Assertions.assertEquals(0, db.scanSplittingAge());
		} finally {
			master.close();
		}
	}
}
