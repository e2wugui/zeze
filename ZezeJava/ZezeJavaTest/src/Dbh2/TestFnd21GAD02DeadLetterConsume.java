package Dbh2;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.CreateSplitBucket;
import Zeze.Builtin.Dbh2.Master.EndMove;
import Zeze.Builtin.Dbh2.Master.EndSplit;
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
 * FND21 GA-D01 A2回归：splitting死信条目的结构性消费（INV1：条目[k,K)活⟺主表floor(k)
 * 的keyLast==K——主表只会收窄不会变宽、splitting消费与主表收窄同批落盘，keyLast不等且
 * 更窄的唯一构造路径是"更晚的settle已越过该keyFirst收窄主表"=死信）。
 * 消费点一（settle拒绝）：迟到endMove/endSplit对主表会更窄方向拒绝时同步消费死信——
 * 该条目永远不可能再合法settle，滞留只会被后续同边界操作收养（(B)自搬运）或永久拒绝
 * （换主窗口变体）。方向性：主表**更宽**（本迁移之前另有settle丢失、主表陈旧，条目仍活）
 * 不消费，留给pending-settle补发（A1）收敛——拒绝码自FND22 GA-C01起为可重试码
 * eSplittingStaleMain（终局码停链+清标志与"仍活"矛盾，见testLateEndMoveStaleWideKept）。
 * 消费点二（createSplitBucket碰撞）：同keyFirst四元组不等时按INV1判死——死信删除后按新
 * 请求重建（变体的永久拒绝转一次自愈）；真在途维持eSplittingBucketExist；floor缺失/更宽
 * 不判死（结构证明不足时保守拒绝）。
 * endSplit from侧对称守卫（增量审留注#1，随本案INV1落地）：主表现存from.keyFirst条目比
 * from更窄=过期快照，put会重新变宽主表——只跳过from的put（to的put仍是正确发布），settle
 * 成功消费条目。
 * 形态：Master+MasterDatabase反射直构（无网络）；重供建桶路径断言以eTooFewManager
 * （无manager可建）证明"已消费并进入重建"，与eSplittingBucketExist（维持拒绝）可区分。
 */
@Fast
public class TestFnd21GAD02DeadLetterConsume {

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
	 * 消费点一（endMove，主表更窄=INV1死信）：move1(to=[2,Empty))的settle迟到，主表已被
	 * move目标桶的后续split收窄为[2,5)——拒绝且同步消费。bug（旧契约）只拒绝不消费：
	 * 条目滞留将被后续同边界操作收养（(B)）。
	 */
	@Test
	public void testLateEndMoveNarrowedConsumed(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var table = new MasterTable.Data();
			db.getTables().put("t1", table);
			table.getBuckets().put(key(2), newBucketMeta(key(2), key(5), "raftB")); // 已收窄
			var stale = newBucketMeta(key(2), Binary.Empty, "raftB"); // 迟到的宽边界
			getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data())
					.getBuckets().put(key(2), stale);

			var r = new EndMove();
			r.Argument.setTo(stale);
			Assertions.assertEquals(master.errorCode(AbstractMaster.eSplittingBucketNotFound), db.endMove(r),
					"INV1死信必须拒绝settle（终局码停止重试）");
			Assertions.assertNull(getSplitting(db).get("t1").getBuckets().get(key(2)),
					"主表更窄方向的拒绝必须同步消费死信条目（滞留=收养面）");
			Assertions.assertEquals(0, key(5).compareTo(table.getBuckets().get(key(2)).getKeyLast()),
					"拒绝不得改写主表收窄条目");
		} finally {
			master.close();
		}
	}

	/**
	 * 方向性反向钉住（endMove，主表更宽=非死信）：主表[F]=[2,Empty)比to=[2,5)宽——这是
	 * 本迁移之前另有settle丢失、主表陈旧（条目比主表新，仍活），不得消费：pending-settle
	 * 补发（A1）收敛主表后，本settle重试可过。
	 * 【FND22 GA-C01契约更新】拒绝码由终局码eSplittingBucketNotFound改为可重试码
	 * eSplittingStaleMain：终局码在重试端停链+onSettled清标志，与"仍活、A1补发收敛后
	 * 重试可过"的注释契约直接矛盾（活迁移被终局拒绝永久杀死）。
	 */
	@Test
	public void testLateEndMoveStaleWideKept(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var table = new MasterTable.Data();
			db.getTables().put("t1", table);
			table.getBuckets().put(key(2), newBucketMeta(key(2), Binary.Empty, "raftA")); // 陈旧宽
			var entry = newBucketMeta(key(2), key(5), "raftB");
			getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data())
					.getBuckets().put(key(2), entry);

			var r = new EndMove();
			r.Argument.setTo(entry);
			Assertions.assertEquals(master.errorCode(Master.eSplittingStaleMain), db.endMove(r),
					"主表更宽方向仍拒绝但必须用可重试码（FND22 GA-C01：终局码停重试+清标志，"
							+ "与'仍活、补发收敛后重试可过'矛盾）");
			Assertions.assertSame(entry, getSplitting(db).get("t1").getBuckets().get(key(2)),
					"主表更宽方向不得消费条目（条目比主表新，仍活——A1补发收敛后重试可过）");
		} finally {
			master.close();
		}
	}

	/**
	 * 消费点一（endSplit to侧，主表更窄）：迟到split1(to=[5,8))到达时主表[5]已被后续世代
	 * 收窄为[5,6)——to的put会重新变宽主表，整笔拒绝且消费死信。
	 */
	@Test
	public void testLateEndSplitToSideNarrowedConsumed(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var table = new MasterTable.Data();
			db.getTables().put("t1", table);
			table.getBuckets().put(key(2), newBucketMeta(key(2), Binary.Empty, "raftA"));
			table.getBuckets().put(key(5), newBucketMeta(key(5), key(6), "raftC")); // 后续世代已收窄[5]
			var to = newBucketMeta(key(5), key(8), "raftB");
			getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data())
					.getBuckets().put(key(5), to);

			var r = new EndSplit();
			r.Argument.setFrom(newBucketMeta(key(2), key(5), "raftA"));
			r.Argument.setTo(to);
			Assertions.assertEquals(master.errorCode(AbstractMaster.eSplittingBucketNotFound), db.endSplit(r),
					"to侧变宽必须拒绝且消费死信");
			Assertions.assertNull(getSplitting(db).get("t1").getBuckets().get(key(5)),
					"死信条目必须消费");
			Assertions.assertEquals(0, key(6).compareTo(table.getBuckets().get(key(5)).getKeyLast()),
					"主表收窄条目不得被过期宽边界覆盖");
		} finally {
			master.close();
		}
	}

	/**
	 * endSplit from侧对称守卫（增量审留注#1）：split1的settle丢失→split2先行settle收窄
	 * 主表[F]=[2,3)→迟到split1(from=[2,5))补发——from的put会把主表重新变宽到[2,5)
	 * （宣称源桶仍持有[3,5)已迁走键域，键域静默失联）。修复：跳过from的put，to的put
	 * 仍是正确发布（to桶确持有[5,8)数据，恰补齐主表缺口），settle成功消费条目。
	 * bug：from直接put，主表[F]被覆盖回[2,5)（过期快照）。
	 */
	@Test
	public void testLateEndSplitFromSideSkipPut(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var table = new MasterTable.Data();
			db.getTables().put("t1", table);
			// split2已settle的主表现状：[F,M2)=[2,3)，[M2,M1)=[3,5)。
			var narrowedFrom = newBucketMeta(key(2), key(3), "raftA");
			table.getBuckets().put(key(2), narrowedFrom);
			table.getBuckets().put(key(3), newBucketMeta(key(3), key(5), "raftC"));
			// 迟到split1：from=[2,5)（过期宽快照），to=[5,8)（正确发布）。
			var to = newBucketMeta(key(5), key(8), "raftB");
			getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data())
					.getBuckets().put(key(5), to);

			var r = new EndSplit();
			r.Argument.setFrom(newBucketMeta(key(2), key(5), "raftA"));
			r.Argument.setTo(to);
			Assertions.assertEquals(0, db.endSplit(r), "from侧过期不得拒绝整个settle（to是正确发布）");
			Assertions.assertSame(narrowedFrom, table.getBuckets().get(key(2)),
					"主表已收窄条目不得被过期from重新变宽（bug：[2,5)过期快照覆盖[2,3)）");
			Assertions.assertEquals(0, narrowedFrom.getKeyLast().compareTo(key(3)),
					"主表[F].keyLast必须保持split2的收窄边界3");
			Assertions.assertSame(to, table.getBuckets().get(key(5)),
					"to的put必须照常发布（补齐[M1,L)=[5,8)主表缺口）");
			Assertions.assertTrue(getSplitting(db).get("t1").getBuckets().isEmpty(),
					"settle成功必须消费splitting条目");
		} finally {
			master.close();
		}
	}

	/**
	 * 消费点二（createSplitBucket碰撞，INV1死信→删除重建）：换主窗口变体——孤儿条目
	 * [2,Empty)滞留，源桶已收窄、主表floor(2)=[2,5)。新move请求[2,5)碰撞：bug时恒
	 * eSplittingBucketExist（该桶每120s一轮move永久失败）；修复后判死、删除、按新请求
	 * 重建——无manager环境断言以eTooFewManager证明已进入重建路径。
	 */
	@Test
	public void testCollisionDeadEntryConsumedAndRebuild(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var table = new MasterTable.Data();
			db.getTables().put("t1", table);
			table.getBuckets().put(key(2), newBucketMeta(key(2), key(5), "raftA")); // 主表已收窄
			// 孤儿条目[2,Empty)：同keyFirst、四元组不等（keyLast不同）。
			getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data())
					.getBuckets().put(key(2), newBucketMeta(key(2), Binary.Empty, "raftOld"));

			var r = new CreateSplitBucket();
			r.Argument = newBucketMeta(key(2), key(5), "");
			Assertions.assertEquals(master.errorCode(AbstractMaster.eTooFewManager), db.createSplitBucket(r),
					"死信必须消费并按新请求重建（bug：eSplittingBucketExist永久拒绝；重建进入选manager段=无manager可建）");
			Assertions.assertNull(getSplitting(db).get("t1").getBuckets().get(key(2)),
					"死信条目必须删除");
		} finally {
			master.close();
		}
	}

	/**
	 * 碰撞反向钉住（真在途）：主表floor(5).keyLast==exist.keyLast（INV1活）——维持
	 * eSplittingBucketExist不消费（在途保护语义不变）。
	 */
	@Test
	public void testCollisionInFlightKept(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var table = new MasterTable.Data();
			db.getTables().put("t1", table);
			table.getBuckets().put(key(2), newBucketMeta(key(2), key(8), "raftA")); // floor(5)=[2,8)
			var inFlight = newBucketMeta(key(5), key(8), "raftB");
			getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data())
					.getBuckets().put(key(5), inFlight);

			var r = new CreateSplitBucket();
			r.Argument = newBucketMeta(key(5), key(9), "");
			Assertions.assertEquals(master.errorCode(AbstractMaster.eSplittingBucketExist), db.createSplitBucket(r),
					"真在途条目必须维持拒绝（不得误删在途迁移）");
			Assertions.assertSame(inFlight, getSplitting(db).get("t1").getBuckets().get(key(5)),
					"在途条目不得消费");
		} finally {
			master.close();
		}
	}

	/**
	 * 碰撞保守钉住（主表更宽=非死信）：主表floor(2)=[2,Empty)比exist[2,5)宽——另有settle
	 * 丢失、主表陈旧，条目比主表新仍活，不判死（A1补发收敛后再论）。
	 */
	@Test
	public void testCollisionStaleWideKept(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var table = new MasterTable.Data();
			db.getTables().put("t1", table);
			table.getBuckets().put(key(2), newBucketMeta(key(2), Binary.Empty, "raftA")); // 陈旧宽
			var entry = newBucketMeta(key(2), key(5), "raftB");
			getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data())
					.getBuckets().put(key(2), entry);

			var r = new CreateSplitBucket();
			r.Argument = newBucketMeta(key(2), key(9), "");
			Assertions.assertEquals(master.errorCode(AbstractMaster.eSplittingBucketExist), db.createSplitBucket(r),
					"主表更宽方向不得判死（条目仍活，结构证明不足保守拒绝）");
			Assertions.assertSame(entry, getSplitting(db).get("t1").getBuckets().get(key(2)),
					"条目不得消费");
		} finally {
			master.close();
		}
	}
}
