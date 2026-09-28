package Zeze.Dbh2;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.EndSplit;
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
 * FND20 GA-C01回归：settleSplitting不变量守卫——合法split恒from.keyFirst&lt;to.keyFirst，
 * from.keyFirst&gt;=to.keyFirst仅出现在move被recoverSplitting的data[0]==keyFirst启发式误判为
 * split时（from=[M,M)空区间，to即move目标）。bug时settleSplitting对from无条件put，
 * 用死源桶把刚发布的新桶从master主表覆盖掉，[M,L)键域读写永久失效且无自动收敛路径。
 * 守卫按endMove语义跳过from的put，误判路径收敛为正确的move完成终态。
 */
@Fast
public class TestFnd20GAC01SettleSplittingGuard {

	private static Binary key(int i) {
		return new Binary(new byte[]{(byte)i});
	}

	private static BBucketMeta.Data newBucketMeta(Binary keyFirst, Binary keyLast) {
		var bucket = new BBucketMeta.Data();
		bucket.setDatabaseName("db1");
		bucket.setTableName("t1");
		bucket.setRaftConfig("");
		bucket.setKeyFirst(keyFirst);
		bucket.setKeyLast(keyLast);
		return bucket;
	}

	// Master构造时扫描home目录自动注册db1（预建目录），反射取回实例（形态对齐TestFnd19GA05）。
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

	@Test
	public void testMisjudgedMoveSplitKeepsNewBucket(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var table = new MasterTable.Data();
			db.getTables().put("t1", table);

			// 误判move经endSplit1构造出的参数形态：to=[5,Empty)即move目标（raft已持有全部数据），
			// from=[5,5)（源meta.keyLast被置为to.keyFirst=源桶keyFirst自身）。
			var to = newBucketMeta(key(5), Binary.Empty);
			var from = newBucketMeta(key(5), key(5));
			// splitting表中存在目标桶（createSplitBucket已发布过）。
			getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data())
					.getBuckets().put(to.getKeyFirst(), to);

			var r = new EndSplit();
			r.Argument.setFrom(from);
			r.Argument.setTo(to);
			Assertions.assertEquals(0, db.endSplit(r));

			// bug时：from的put在to之后覆盖同keyFirst条目，主表指向死源桶[5,5)。
			Assertions.assertSame(to, table.getBuckets().get(key(5)),
					"误判move的settle必须保留新桶（from不得覆盖to）");
			Assertions.assertTrue(getSplitting(db).get("t1").getBuckets().isEmpty(), "settle必须消费splitting表条目");
		} finally {
			master.close();
		}
	}

	@Test
	public void testLegalSplitPublishesBothBuckets(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var table = new MasterTable.Data();
			db.getTables().put("t1", table);

			// 合法split形态：from=[2,5)（源桶收窄），to=[5,Empty)（新桶），from.keyFirst<to.keyFirst。
			var from = newBucketMeta(key(2), key(5));
			var to = newBucketMeta(key(5), Binary.Empty);
			getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data())
					.getBuckets().put(to.getKeyFirst(), to);

			var r = new EndSplit();
			r.Argument.setFrom(from);
			r.Argument.setTo(to);
			Assertions.assertEquals(0, db.endSplit(r));

			Assertions.assertSame(from, table.getBuckets().get(key(2)), "合法split必须发布收窄后的源桶");
			Assertions.assertSame(to, table.getBuckets().get(key(5)), "合法split必须发布新桶");
		} finally {
			master.close();
		}
	}
}
