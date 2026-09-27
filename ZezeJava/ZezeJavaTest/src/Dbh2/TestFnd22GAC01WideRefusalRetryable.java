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
import Zeze.IModule;
import Zeze.Net.Binary;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND22 GA-C01回归：settleSplitting宽方向（主表陈旧宽、条目仍活）拒绝必须返回**可重试**码，
 * 不得返回终局码eSplittingBucketNotFound。
 * bug机制：宽方向拒绝的注释契约自述"本迁移之前另有settle丢失、主表陈旧（条目比主表新，仍活）
 * ——拒绝不消费，pending-settle补发收敛主表后重试可过"，但eSplittingBucketNotFound在
 * MasterAgent重试端是"已结算证据"终局语义：停重试+runSettled(onSettled)→appendClearPending
 * Settle清标志——"重试可过"的前提被同一错误码消灭，且补发源被亲手拆除：条目保留+链终止+
 * 标志清除三者组合无任何自愈路径，活迁移永不结算。
 * 修复=宽方向改用可重试码eSplittingStaleMain(8)（Master.java手写常量），死信方向（主表更窄=
 * INV1）与幂等完成证据维持终局码不变。
 * 反向验证说明：测试不静态引用Master.eSplittingStaleMain（未修基线上该常量不存在，静态引用
 * 会把红因变成编译红）；期望码以IModule.errorCode(ModuleId, 8)合成——基线红因=返回码行为差异
 *（4→8），保持"真实行为红"。
 */
@Fast
public class TestFnd22GAC01WideRefusalRetryable {

	// 修复码eSplittingStaleMain=8（模块错误码生成侧1-7已用，8为空闲位）。
	private static final long eSplittingStaleMainCode = IModule.errorCode(AbstractMaster.ModuleId, 8);

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

	/**
	 * 主用例（红）：主表陈旧宽（exist=[2,8)@A 比 to=[2,5)@B 宽——本迁移之前另有settle丢失、
	 * 主表未收敛）。迟到的endMove必须拒绝但用**可重试码**：条目仍活，重试链必须存活等待
	 * "补发收敛主表后重试可过"。bug时返回eSplittingBucketNotFound（终局停重试+清标志，
	 * 活迁移被永久杀死）。
	 */
	@Test
	public void testWideRefusalReturnsRetryableCodeAndKeepsEntry(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var table = new MasterTable.Data();
			db.getTables().put("t1", table);

			// 主表陈旧宽：[2,8)@A（split1的settle丢失，主表尚未收窄到[2,5)）。
			var staleWide = newBucketMeta(key(2), key(8), "raftA");
			table.getBuckets().put(key(2), staleWide);

			// 迟到的move endMove：to=[2,5)@B（窄于主表现存条目）。
			var to = newBucketMeta(key(2), key(5), "raftB");
			getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data())
					.getBuckets().put(to.getKeyFirst(), to);

			var r = new EndMove();
			r.Argument.setTo(to);
			Assertions.assertEquals(eSplittingStaleMainCode, db.endMove(r),
					"宽方向拒绝必须返回可重试码eSplittingStaleMain（bug：终局码eSplittingBucketNotFound"
							+ "停重试+清标志，与自身注释'仍活、补发收敛后重试可过'直接矛盾，活迁移永不结算）");

			// 拒绝不消费条目、不改写主表（两形态在bug与修复下一致，钉住拒绝的现场不变式）。
			Assertions.assertSame(to, getSplitting(db).get("t1").getBuckets().get(key(2)),
					"宽方向拒绝不得消费仍活的splitting条目");
			Assertions.assertSame(staleWide, table.getBuckets().get(key(2)),
					"宽方向拒绝不得改写主表现存条目");
		} finally {
			master.close();
		}
	}

	/**
	 * 注释契约钉住（绿）：同一宽形态拒绝后，主表被补发/在途settle链收敛（[2,8)@A收窄为同界
	 * [2,5)@A），重试的同一endMove必须照常settle通过——"补发收敛主表后重试可过"的master侧
	 * 落点。可重试码让重试链活到这一刻正是修复目的（agent侧重试契约由
	 * TestFnd22GAC01AgentRetriesOnStaleMain钉住）。
	 */
	@Test
	public void testWideRefusalSettlesAfterMainConverges(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var table = new MasterTable.Data();
			db.getTables().put("t1", table);

			var staleWide = newBucketMeta(key(2), key(8), "raftA");
			table.getBuckets().put(key(2), staleWide);
			var to = newBucketMeta(key(2), key(5), "raftB");
			getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data())
					.getBuckets().put(to.getKeyFirst(), to);

			var r = new EndMove();
			r.Argument.setTo(to);
			Assertions.assertNotEquals(0, db.endMove(r), "宽形态首次到达必须拒绝");

			// A1补发收敛主表：丢失的settle把[2,8)收窄为同界旧raft源桶[2,5)@A。
			table.getBuckets().put(key(2), newBucketMeta(key(2), key(5), "raftA"));

			// 重试的同一endMove：cmp==0（同边界旧raft源桶）——正常move settle，必须通过。
			Assertions.assertEquals(0, db.endMove(r), "主表收敛后重试的endMove必须照常settle（注释契约）");
			Assertions.assertSame(to, table.getBuckets().get(key(2)), "settle必须以to替换同界源桶");
			Assertions.assertTrue(getSplitting(db).get("t1").getBuckets().isEmpty(), "settle必须消费splitting条目");
		} finally {
			master.close();
		}
	}

	/**
	 * 死信方向钉住（绿）：主表更窄（INV1死信，更晚的settle已越过）维持终局码eSplittingBucketNotFound
	 * 并同步消费死信条目——宽方向改可重试码不得外溢到终局语义（FND21 GA-C01/GA-D01 A2既有契约）。
	 */
	@Test
	public void testDeadLetterDirectionStillTerminalAndConsumed(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var table = new MasterTable.Data();
			db.getTables().put("t1", table);

			var narrowed = newBucketMeta(key(2), key(3), "raftB");
			table.getBuckets().put(key(2), narrowed);
			var stale = newBucketMeta(key(2), key(8), "raftB");
			getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data())
					.getBuckets().put(stale.getKeyFirst(), stale);

			var r = new EndMove();
			r.Argument.setTo(stale);
			Assertions.assertEquals(master.errorCode(AbstractMaster.eSplittingBucketNotFound), db.endMove(r),
					"死信方向（主表更窄）必须维持终局码停止重试");
			Assertions.assertNull(getSplitting(db).get("t1").getBuckets().get(key(2)),
					"死信方向必须同步消费splitting条目（INV1）");
			Assertions.assertSame(narrowed, table.getBuckets().get(key(2)), "死信拒绝不得改写主表收窄条目");
		} finally {
			master.close();
		}
	}
}
