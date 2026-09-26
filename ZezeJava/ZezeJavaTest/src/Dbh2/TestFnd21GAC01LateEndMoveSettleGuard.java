package Dbh2;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.EndMove;
import Zeze.Config;
import Zeze.Dbh2.Master.AbstractMaster;
import Zeze.Dbh2.Master.Master;
import Zeze.Dbh2.Master.MasterDatabase;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND21 GA-C01路径C回归：settleSplitting对from==null（endMove）的主表一致性校验。
 * endMove的完成通知（endMoveWithRetryAsync，30s重试）迟到时，move目标桶可能已因负载
 * 后续split并settle——主表同keyFirst条目被收窄（[F,M)）。迟到的endMove(to=[F,L))若被
 * 放行settle，会把过期的宽边界put进主表，覆盖收窄条目（宣称已deleteToEnd的键域仍归
 * 该桶），master元数据永久性错误且重试端因settle"成功"停止重试。
 * 修复=主表现存条目keyLast与to.keyFirst不等时拒绝settle（error日志+eSplittingBucketNotFound
 * 终局语义，MasterAgent按"已settle"停止重试），不动任何状态。
 * 正常move settle（同边界旧raft源桶→新raft）不受影响：move在主表只改写raftConfig，
 * 边界不动，keyLast必相等。splitting陈旧条目的生命周期收敛属四实体设计缺口（GA-D01），
 * 本案不消费。
 */
@Fast
public class TestFnd21GAC01LateEndMoveSettleGuard {

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

	// Master构造时扫描home目录自动注册db1（预建目录），反射取回实例（形态对齐TestFnd20GAC01）。
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
	public void testLateEndMoveRefusedOnNarrowedMainTable(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var table = new MasterTable.Data();
			db.getTables().put("t1", table);

			// move目标桶B先行split(M分界)并settle后的主表现状：[F,M)=[2,5)@B收窄条目。
			var narrowed = newBucketMeta(key(2), key(5), "raftB");
			table.getBuckets().put(key(2), narrowed);

			// 迟到的move1 endMove：to=[F,L)=[2,Empty)@B（createSplitBucket发布的move目标，
			// 宽边界），splitting[F]陈旧条目仍在（move1的settle通知因master连接空窗未送达）。
			var stale = newBucketMeta(key(2), Binary.Empty, "raftB");
			getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data())
					.getBuckets().put(stale.getKeyFirst(), stale);

			var r = new EndMove();
			r.Argument.setTo(stale);
			Assertions.assertEquals(master.errorCode(AbstractMaster.eSplittingBucketNotFound), db.endMove(r),
					"迟到endMove对已收窄主表必须拒绝settle（终局码让重试停止）");

			// bug时：to的put覆盖收窄条目，主表宣称[2,Empty)全归B（[5,Empty)键域元数据失真）。
			Assertions.assertSame(narrowed, table.getBuckets().get(key(2)),
					"拒绝settle不得改写主表收窄条目（bug：被过期宽边界覆盖）");
			// 拒绝路径不消费splitting陈旧条目：其生命周期收敛属GA-D01设计范围，本案不动。
			Assertions.assertSame(stale, getSplitting(db).get("t1").getBuckets().get(key(2)),
					"拒绝settle不动splitting表（陈旧条目清理由GA-D01承担）");
		} finally {
			master.close();
		}
	}

	@Test
	public void testNormalMoveSettleReplacesSameBoundarySource(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var table = new MasterTable.Data();
			db.getTables().put("t1", table);

			// 正常move settle形态：主表现存同边界旧raft源桶[F,L)@A，to=[F,L)@B。
			// move在主表只改写raftConfig、边界不动——校验必须放行（keyLast相等）。
			var source = newBucketMeta(key(2), Binary.Empty, "raftA");
			table.getBuckets().put(key(2), source);
			var to = newBucketMeta(key(2), Binary.Empty, "raftB");
			getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data())
					.getBuckets().put(to.getKeyFirst(), to);

			var r = new EndMove();
			r.Argument.setTo(to);
			Assertions.assertEquals(0, db.endMove(r), "同边界move settle必须照常成功");

			Assertions.assertSame(to, table.getBuckets().get(key(2)), "move settle必须以to替换旧raft源桶");
			Assertions.assertTrue(getSplitting(db).get("t1").getBuckets().isEmpty(), "settle必须消费splitting表条目");
		} finally {
			master.close();
		}
	}

	@Test
	public void testFreshKeyFirstEndMoveSettles(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var table = new MasterTable.Data();
			db.getTables().put("t1", table);

			// 主表同keyFirst无现存条目（防御分支）：校验不拦截，settle正常落表。
			var to = newBucketMeta(key(2), Binary.Empty, "raftB");
			getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data())
					.getBuckets().put(to.getKeyFirst(), to);

			var r = new EndMove();
			r.Argument.setTo(to);
			Assertions.assertEquals(0, db.endMove(r), "主表无现存条目时endMove settle必须照常成功");
			Assertions.assertSame(to, table.getBuckets().get(key(2)), "settle必须发布to");
		} finally {
			master.close();
		}
	}
}
