package Zeze.Dbh2.Master;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.CreateBucket;
import Zeze.Builtin.Dbh2.Master.CreateSplitBucket;
import Zeze.Builtin.Dbh2.Master.DestroyBucket;
import Zeze.Builtin.Dbh2.Master.EndMove;
import Zeze.Builtin.Dbh2.Master.EndSplit;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.IModule;
import Zeze.Net.Binary;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.OutObject;
import Zeze.Util.RocksDatabase;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;

/**
 * Master 侧单数据库的元数据存储：主表与 splitting 分桶表的内存映射及 RocksDB 持久化。
 */
public class MasterDatabase {
	private static final Logger logger = LogManager.getLogger(MasterDatabase.class);
	private final String databaseName;
	private final RocksDatabase rocksDb;

	// tables
	private final ConcurrentHashMap<String, MasterTable.Data> tables = new ConcurrentHashMap<>();
	private final RocksDatabase.Table rocksTables;

	// tables 包含分桶目标。
	private final ConcurrentHashMap<String, MasterTable.Data> splitting = new ConcurrentHashMap<>();
	private final RocksDatabase.Table rocksSplitting;
	// splitting条目年龄副表：key=tableName+keyFirst→创建时间戳。独立副表
	// 不动MasterTable.Data手写编码格式（旧数据decode兼容零成本）。只观测不动作：超龄error
	// 告警（阈值Dbh2Config.SplittingAgeWarnMs，默认10min量级），消费必须结构驱动（INV1）。
	private final RocksDatabase.Table rocksSplittingAge;
	private final Master master;

	public MasterDatabase(Master master, String databaseName) {
		try {
			this.master = master;
			this.databaseName = databaseName;
			rocksDb = new RocksDatabase(Path.of(master.getHome(), databaseName).toString());
			rocksTables = rocksDb.getOrAddTable("tables");
			rocksSplitting = rocksDb.getOrAddTable("splitting");
			rocksSplittingAge = rocksDb.getOrAddTable("splittingAge");

			try (var it = rocksTables.iterator()) {
				it.seekToFirst();
				while (it.isValid()) {
					var tableName = new String(it.key(), StandardCharsets.UTF_8);
					var bTable = new MasterTable.Data();
					var bb = ByteBuffer.Wrap(it.value());
					bTable.decode(bb);
					tables.put(tableName, bTable);
					it.next();
					logger.info("table: {}", bTable);
				}
			}

			try (var it = rocksSplitting.iterator()) {
				it.seekToFirst();
				while (it.isValid()) {
					var tableName = new String(it.key(), StandardCharsets.UTF_8);
					var bTable = new MasterTable.Data();
					var bb = ByteBuffer.Wrap(it.value());
					bTable.decode(bb);
					splitting.put(tableName, bTable);
					it.next();
					logger.info("bucket: {}", bTable);
				}
			}
		} catch (RocksDBException ex) {
			throw new RuntimeException(ex);
		}
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public MasterTable.Data getTable(String tableName) {
		return tables.get(tableName);
	}

	public BBucketMeta.Data locateBucket(String tableName, Binary key) {
		var bTable = getTable(tableName);
		if (null == bTable)
			return null;
		return bTable.locate(key);
	}

	public void close() {
		logger.info("closeDb: {}, {}", master.getHome(), databaseName);
		rocksDb.close();
	}

	public ConcurrentHashMap<String, MasterTable.Data> getTables() {
		return tables;
	}

	public MasterTable.Data createTable(String tableName, OutObject<Boolean> outIsNew) throws Exception {
		outIsNew.value = false;
		while (true) {
			var table = tables.computeIfAbsent(tableName, __ -> new MasterTable.Data());
			if (table.created) {
				logger.info("create table exist: {}.{}", databaseName, tableName);
				return table;
			}

			table.lock();
			try {
				// 加锁后再次检查一次。
				if (table.created) {
					logger.info("create table exist: {}.{}", databaseName, tableName);
					return table;
				}
				// 失败路径（见下方两处tables.remove两参调用）会摘除自己放入map的空条目：持陈旧引用的
				// 并发创建者必须在此重检身份，失配即弃锁重试新条目——否则在未映射实例上建表成功但对
				// getTable不可见，且与新条目的并发创建各建一套raft。身份检查与摘除同持本锁，互斥成立。
				if (tables.get(tableName) != table)
					continue;

				outIsNew.value = true;

				var bucket = new BBucketMeta.Data();
				bucket.setDatabaseName(databaseName);
				bucket.setTableName(tableName);
				bucket.setKeyFirst(Binary.Empty);
				bucket.setKeyLast(Binary.Empty);

				// allocate first bucket service and setup table
				var managers = master.choiceManagers();
				if (managers.size() < master.getDbh2Config().getRaftClusterCount()) {
					logger.warn("managers.size({}) < raftClusterCount({})",
							managers.size(), master.getDbh2Config().getRaftClusterCount());
					// 摘除本次新建的空表（两参remove仅当映射仍为本实例）：残留的created=false空表让
					// GetBuckets返回rc=0空桶表，客户端/服务端locate的floorEntry==null以NPE面目取代
					// 可重试的eTableNotFound（getTable为null的既有路径）。
					tables.remove(tableName, table);
					return null;
				}

				// 持久化滞后窗口（次序约束，非可随意对调）：saveRocks必须排在
				// createBucketRafts/setBucketMeta成功之后——提前落盘会把未经 rpc 验证可用
				// 的raft发布进主表（窗口内崩溃重启后created=true但桶未初始化/半建，客户端
				// 路由到坏桶且无重建路径，劣于孤儿）。现序的残余窗口：三者成功而saveRocks
				// 未完成时崩溃，重启后created=false走重建——choiceManagers分配新端口建全
				// 新raft，旧raft成为无主孤儿（manager扫描raft.xml仍加载，仅占端口/内存/
				// 磁盘目录，无数据丢失），需人工清理。孤儿收养（重入时查询manager既有
				// raftName幂等复用）或Register对账回收需跨进程协议，不在本处内联。
				var raftNames = buildRaftConfig(bucket, managers);
				table.buckets.put(bucket.getKeyFirst(), bucket);
				try {
					createBucketRafts(managers, bucket, raftNames);
					setBucketMeta(bucket);
					table.created = true;
					saveRocks(rocksTables, tableName, table);
				} catch (Exception e) {
					// 失败回滚：摘内存表并请各manager销毁刚建的raft——只回滚内存会留下永久孤儿
					//（进程/端口/磁盘）。
					table.buckets.remove(bucket.getKeyFirst());
					table.created = false;
					// 同上：摘除空表条目，不留locate-NPE窗口（重试经computeIfAbsent重建）。
					tables.remove(tableName, table);
					destroyBucketRafts(managers, bucket, raftNames);
					throw e;
				}
			} finally {
				table.unlock();
			}
			logger.info("create table new: {}.{}", databaseName, tableName);
			return table;
		}
	}

	// 构建raft-config，基本的用于客户端，用于manager服务器的需要replace RaftName.
	private ArrayList<String> buildRaftConfig(BBucketMeta.Data bucket, ArrayList<Master.Manager> managers) throws RocksDBException {
		var sbRaft = new StringBuilder();
		sbRaft.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		sbRaft.append("\n");
		sbRaft.append("<raft Name=\"RaftName\">\n");
		var raftNames = new ArrayList<String>(managers.size());
		for (var e : managers) {
			sbRaft.append("    <node Host=\"");
			sbRaft.append(e.data.getDbh2RaftAcceptorName()).append("\"");

			sbRaft.append(" Port=\"");
			var portId = master.nextBucketPortId(e.data.getDbh2RaftAcceptorName());
			sbRaft.append(portId).append("\"");

			sbRaft.append(" ProxyHost=\"").append(e.data.getDbh2RaftAcceptorName()).append("\"");
			sbRaft.append(" ProxyPort=\"").append(e.data.getPort()).append("\"");

			sbRaft.append("/>\n");
			var raftName = e.data.getDbh2RaftAcceptorName() + "_" + portId;
			raftNames.add(raftName);
			bucket.getHost2Raft().put(e.data.getDbh2RaftAcceptorName() + "_" + e.data.getPort(), raftName);
		}
		sbRaft.append("</raft>");
		bucket.setRaftConfig(sbRaft.toString());
		return raftNames;
	}

	private static void createBucketRafts(ArrayList<Master.Manager> managers,
										  BBucketMeta.Data bucket, ArrayList<String> raftNames) {
		var rpcs = new ArrayList<CreateBucket>();
		var futures = new ArrayList<TaskCompletionSource<?>>();
		var i = 0;
		for (var e : managers) {
			var r = new CreateBucket();
			r.Argument.assign(bucket);
			// 用于manager服务器的需要replace RaftName.
			//noinspection DynamicRegexReplaceableByCompiledPattern
			r.Argument.setRaftConfig(r.Argument.getRaftConfig().replaceAll("RaftName", raftNames.get(i++)));
			rpcs.add(r);
			futures.add(r.SendForWait(e.socket, 30_000));
		}
		for (var j = 0; j < futures.size(); ++j) {
			futures.get(j).await();
			// rpc失败不能静默：继续走saveRocks会让Master认为桶创建成功。
			var rc = rpcs.get(j).getResultCode();
			if (rc != 0)
				throw new RuntimeException("CreateBucket fail. manager=" + managers.get(j).data.getDbh2RaftAcceptorName()
						+ " rc=" + IModule.getErrorCode(rc));
		}
	}

	// 建桶半失败的尽力回收：对配置内全部manager发DestroyBucket（幂等，未建过即成功；与
	// CreateBucket同socket且派发串行，销毁必在建之后处理）。只记日志不上抛——回收失败不得
	// 掩盖原始异常，残留raft依赖人工核查。
	private static void destroyBucketRafts(ArrayList<Master.Manager> managers,
										   BBucketMeta.Data bucket, ArrayList<String> raftNames) {
		var rpcs = new ArrayList<DestroyBucket>();
		var futures = new ArrayList<TaskCompletionSource<?>>();
		var i = 0;
		for (var e : managers) {
			var r = new DestroyBucket();
			r.Argument.assign(bucket);
			// 与createBucketRafts同款：发给该manager的配置须替换成它的raftName（manager按名字解析portId）。
			//noinspection DynamicRegexReplaceableByCompiledPattern
			r.Argument.setRaftConfig(r.Argument.getRaftConfig().replaceAll("RaftName", raftNames.get(i++)));
			rpcs.add(r);
			futures.add(r.SendForWait(e.socket, 30_000));
		}
		for (var j = 0; j < futures.size(); ++j) {
			try {
				futures.get(j).await();
				var rc = rpcs.get(j).getResultCode();
				if (rc != 0)
					logger.error("DestroyBucket fail. manager={} rc={}",
							managers.get(j).data.getDbh2RaftAcceptorName(), IModule.getErrorCode(rc));
			} catch (Exception ex) {
				logger.error("DestroyBucket await fail. manager={}",
						managers.get(j).data.getDbh2RaftAcceptorName(), ex);
			}
		}
	}

	private static void setBucketMeta(BBucketMeta.Data bucket) throws Exception {
		// 第一条Dbh2桶协议，桶必须初始化以后才能使用。
		logger.info("setBucketMeta: new Dbh2Agent: {}", bucket.getRaftConfig());
		var agent = new Dbh2Agent(bucket.getRaftConfig());
		try {
			agent.setBucketMeta(bucket);
		} finally {
			agent.close();
		}
	}

	public long endMove(EndMove r) throws Exception {
		var to = r.Argument.getTo();
		var tableName = to.getTableName();
		var table = tables.get(tableName);
		if (null == table)
			return master.errorCode(Master.eTableNotFound);

		table.lock();
		try {
			var splitting = this.splitting.computeIfAbsent(tableName, __ -> new MasterTable.Data());
			return settleSplitting(table, splitting, tableName, to, null, r);
		} finally {
			table.unlock();
		}
	}

	public long endSplit(EndSplit r) throws Exception {
		var from = r.Argument.getFrom();
		var tableName = from.getTableName();
		var table = tables.get(tableName);
		if (null == table)
			return master.errorCode(Master.eTableNotFound);

		table.lock();
		try {
			var splitting = this.splitting.computeIfAbsent(tableName, __ -> new MasterTable.Data());
			return settleSplitting(table, splitting, tableName, r.Argument.getTo(), from, r);
		} finally {
			table.unlock();
		}
	}

	// 确认splitting桶与决策一致后原子搬迁（endMove/endSplit的公共主体）：新桶移出splitting、
	// 入主表，from非null时替换主表源桶（endSplit分裂后源桶边界收窄；endMove传null），
	// 双表同批落库。splitting的TreeMap必须持它自己的锁访问：createSplitBucket持splitting锁
	// get/put，只持主表锁remove/encode的话两锁互不互斥，TreeMap并发读写可CME/死循环。
	// 锁序固定主表→splitting（createSplitBucket碰撞判定按同序两阶段，无环）。
	// 调用方须已持主表锁；新桶不在splitting或四元组不等值时不动任何状态，返回错误码。
	private long settleSplitting(MasterTable.Data table, MasterTable.Data splitting, String tableName,
								 BBucketMeta.Data to, BBucketMeta.Data from, Rpc<?, ?> r) throws Exception {
		splitting.lock();
		try {
			var bucket = splitting.buckets.get(to.getKeyFirst());
			if (bucket == null || !sameBucketMeta(bucket, to))
				return master.errorCode(Master.eSplittingBucketNotFound);

			var exist = table.buckets.get(to.getKeyFirst());
			// already-settled守卫：主表同keyFirst现存条目与to
			// 四元组+raftConfig全等 = 本迁移已结算过（重试/leader-ready补发的幂等重复，或settle
			// 成功而响应丢失后的重发）。返回已结算终局码且**不消费splitting条目**：此刻表内的
			// 条目可能是在途的新世代条目（同四元组、不同raftConfig——master重启/补发间隙内
			// createSplitBucket重建），被旧迁移的补发抢占消费会让新迁移永不settle。raftConfig
			// 全等比较在此合法：两侧都是master已知的完整meta（与resume场景请求方raftConfig=""
			// 不同）。
			if (null != exist && sameBucketMeta(exist, to) && exist.getRaftConfig().equals(to.getRaftConfig())) {
				logger.info("settleSplitting already settled, keep in-flight entry. to={}", to);
				return master.errorCode(Master.eSplittingBucketNotFound);
			}

			// 迟到settle守卫：from==null即endMove。
			// 不变式：主表只会收窄不会变宽，合法的move settle时刻主表同keyFirst现存条目只能是
			// 同边界旧raft源桶（move在主表只改写raftConfig），keyLast必相等。不等按方向分治：
			//  - 更窄（INV1死信）：更晚的settle已越过该keyFirst收窄主表——to的宽边界是过期快照，
			//    put会覆盖收窄后的主表（宣称已deleteToEnd的键域仍归本桶），永久元数据谎言。拒绝
			//    且**同步消费死信条目**（remove+落盘）：该条目永远不可能再合法settle，滞留只会被
			//    后续同边界操作收养或永久拒绝（变体）。
			//  - 更宽：本迁移**之前**另有settle丢失、主表陈旧（条目比主表新，仍活）——拒绝不消费，
			//    pending-settle补发或在途settle链收敛主表后重试可过。
			// 错误码按方向分离：死信与幂等完成证据用终局码eSplittingBucketNotFound
			//（MasterAgent重试端按"已settle"停止重试+onSettled清标志）；宽方向用**可重试码**
			// eSplittingStaleMain——仍活的迁移不得被终局码杀死（停重试+清标志后条目虽保留却无人
			// 再结算，[F,L)键域永久失联），非终局码让30s重试链存活，"收敛后重试可过"由此成立。
			if (from == null && null != exist) {
				var cmp = compareKeyLast(exist.getKeyLast(), to.getKeyLast());
				if (cmp < 0) {
					logger.error("settleSplitting late endMove refused, dead entry consumed (INV1). exist={} to={}",
							exist, to);
					return consumeDeadSplitting(splitting, tableName, to.getKeyFirst());
				}
				if (cmp > 0) {
					logger.error("settleSplitting late endMove refused retryable, main stale-wide, keep entry. exist={} to={}",
							exist, to);
					return master.errorCode(Master.eSplittingStaleMain);
				}
				// cmp==0：同边界旧raft源桶——正常move settle。
			}

			// endSplit to侧变宽守卫（INV1）：同款单调性
			// 论证对from!=null成立：主表现存同keyFirst条目比to更窄 = 更晚的settle已越过，to的
			// put会重新变宽主表——本迁移的目标键域已被后续世代接管，整笔拒绝且消费死信。
			if (from != null && null != exist && compareKeyLast(exist.getKeyLast(), to.getKeyLast()) < 0) {
				logger.error("settleSplitting late endSplit refused, dead entry consumed (INV1). exist={} to={}",
						exist, to);
				return consumeDeadSplitting(splitting, tableName, to.getKeyFirst());
			}

			// 内存先行、磁盘后写的失败回滚：下面的remove/put先于batch.commit执行，commit抛出
			//（磁盘错误）时内存已是"已结算"形态而磁盘是旧值——重试按已修改的内存返回
			// eSplittingBucketNotFound，重试端按"已结算终局证据"停链并触发源桶清除补发源
			//（appendClearPendingSettle），master若在此刻崩溃重启，磁盘splitting条目复活且
			// 无人消费（主表旧边界指向已deleteToEnd的源桶，键域失联）。捕获后按捕获的旧值
			// 精确还原内存再上抛：持主表+splitting双锁期间无并发写者（全部写路径同序持锁），
			// 回滚即还原到guard判定时的状态，重试按未结算语义正常进行。
			var settledSplitting = splitting.buckets.remove(to.getKeyFirst());
			var oldTo = table.buckets.put(to.getKeyFirst(), to);
			var fromReplaced = false;
			BBucketMeta.Data oldFrom = null;
			// 不变量：合法split恒from.keyFirst<to.keyFirst（locateMiddle取的中位key严格大于源桶首key）。
			// >=仅出现在move被recoverSplitting的data[0]==keyFirst启发式误判为split时
			//（from=[M,M)空区间，to即move目标）：此时按endMove语义不put from——to的put已是
			// move完成的正确终态，from的put会把刚发布的新桶覆盖回死源桶，[M,L)键域在master表永久丢失。
			if (from != null && from.getKeyFirst().compareTo(to.getKeyFirst()) < 0) {
				// from侧对称守卫：主表现存from.keyFirst条目比from更窄 = 更晚的
				// settle已收窄（本from是过期快照），put会重新变宽主表（宣称源桶仍持有已迁走/已删
				// 的键域，键域静默失联）。只跳过from的put：to的put仍是正确发布——迟到settle的to桶
				// 确持有该键域数据（典型形态：split1的settle丢失→split2先行settle→迟到split1补发，
				// 跳过from1、发布to1，主表恰补齐[M1,L)缺口）。settle成功，条目正常消费。
				// 宽/等界维持put：更宽=正常split settle的必经形态（主表
				// [F,L2)收窄为from[F,L)），跳过即破坏一切正常split发布；堆叠形态（源桶已被move
				// 置死、迟到split settle发布死源桶）的发布是暂态——move侧重试链（宽方向改可重试码
				// 后存活）在≤30s内以同界to替换之；等界不同raft的主表形态经推演不可达（需settle
				// 终局后重复迟到，而终局即停链+清标志），不为此加防御分支。
				var existFrom = table.buckets.get(from.getKeyFirst());
				if (null != existFrom && compareKeyLast(existFrom.getKeyLast(), from.getKeyLast()) < 0)
					logger.error("settleSplitting late from skipped (main narrower). existFrom={} from={}",
							existFrom, from);
				else {
					oldFrom = table.buckets.put(from.getKeyFirst(), from); // replace
					fromReplaced = true;
				}
			} else if (from != null)
				logger.error("settleSplitting from.keyFirst>=to.keyFirst, skip from. from={} to={}", from, to);

			try (var batch = rocksDb.newBatch()) {
				var bbTable = table.encode();
				var bbSplitting = splitting.encode();
				var key = tableName.getBytes(StandardCharsets.UTF_8);
				rocksTables.put(batch, key, 0, key.length,
						bbTable.Bytes, bbTable.ReadIndex, bbTable.size());
				rocksSplitting.put(batch, key, 0, key.length,
						bbSplitting.Bytes, bbSplitting.ReadIndex, bbSplitting.size());
				batch.commit();
			} catch (RocksDBException ex) {
				splitting.buckets.put(to.getKeyFirst(), settledSplitting);
				if (null != oldTo)
					table.buckets.put(to.getKeyFirst(), oldTo);
				else
					table.buckets.remove(to.getKeyFirst());
				if (fromReplaced) {
					if (null != oldFrom)
						table.buckets.put(from.getKeyFirst(), oldFrom);
					else
						table.buckets.remove(from.getKeyFirst());
				}
				throw ex;
			}
			splittingAgeRemoveQuietly(tableName, to.getKeyFirst());
			r.SendResult();
			return 0;
		} finally {
			splitting.unlock();
		}
	}

	// 死信条目消费（INV1）：remove+落盘+年龄记录回收，返回终局错误码。调用方须已持主表锁
	//（endMove/endSplit路径）与splitting锁。
	private long consumeDeadSplitting(MasterTable.Data splitting, String tableName, Binary keyFirst)
			throws RocksDBException {
		var dead = splitting.buckets.remove(keyFirst);
		try {
			saveRocks(rocksSplitting, tableName, splitting);
		} catch (RocksDBException ex) {
			// 同settleSplitting的失败回滚：save失败时磁盘仍是旧值，内存先行不还原会让重试按
			// 已修改的内存误判"已消费"（get返回null→eSplittingBucketNotFound终局停链），master
			// 崩溃重启后磁盘条目复活无人消费。
			if (null != dead)
				splitting.buckets.put(keyFirst, dead);
			throw ex;
		}
		splittingAgeRemoveQuietly(tableName, keyFirst);
		return master.errorCode(Master.eSplittingBucketNotFound);
	}

	// 桶身份四元组等值：endMove/endSplit确认splitting桶与决策一致、createSplitBucket
	// 幂等重试识别同一桶用的同一判据。
	private static boolean sameBucketMeta(BBucketMeta.Data a, BBucketMeta.Data b) {
		return a.getDatabaseName().equals(b.getDatabaseName())
				&& a.getTableName().equals(b.getTableName())
				&& a.getKeyFirst().equals(b.getKeyFirst())
				&& a.getKeyLast().equals(b.getKeyLast());
	}

	// keyLast的域序：Binary.Empty表示+∞（无上界），其余按字典序。主表边界比较的统一序
	//（"主表只会收窄"的单调性在此序上成立）。
	private static int compareKeyLast(Binary a, Binary b) {
		if (a.size() == 0)
			return b.size() == 0 ? 0 : 1;
		if (b.size() == 0)
			return -1;
		return a.compareTo(b);
	}

	public long createSplitBucket(CreateSplitBucket r) throws Exception {
		var bucket = r.Argument;
		String tableName = bucket.getTableName();
		var mainTable = tables.get(tableName);
		if (null == mainTable) {
			logger.error("createBucket but table not found. database={} table={}", databaseName, tableName);
			return master.errorCode(Master.eTableNotFound);
		}

		var table = splitting.computeIfAbsent(tableName, __ -> new MasterTable.Data());
		while (true) {
			table.lock();
			try {
				var exist = table.buckets.get(bucket.getKeyFirst());
				if (exist == null) {
					// allocate first bucket service and setup table
					var managers = master.choiceSmallLoadManagers();
					if (managers.size() < master.getDbh2Config().getRaftClusterCount()) {
						logger.info("too few small load manager. database={} table={}", databaseName, tableName);
						return master.errorCode(Master.eTooFewManager);
					}

					var raftNames = buildRaftConfig(bucket, managers);
					table.buckets.put(bucket.getKeyFirst(), bucket);
					try {
						createBucketRafts(managers, bucket, raftNames);
						saveRocks(rocksSplitting, tableName, table);
					} catch (Exception e) {
						// 失败回滚内存表并销毁manager侧刚建的raft：脏entry让之后所有重试被
						// eSplittingBucketExist拒绝分桶卡死，孤儿raft永占进程/端口/磁盘。
						table.buckets.remove(bucket.getKeyFirst());
						destroyBucketRafts(managers, bucket, raftNames);
						throw e;
					}
					splittingAgeCreateQuietly(tableName, bucket.getKeyFirst());

					r.Result = bucket;
					r.SendResult();
					return 0;
				}
				if (sameBucketMeta(exist, bucket)) {
					// 桶已经存在。响应丢失/日志截断后manager重试时走这里：必须把已存在的桶
					// 幂等返回（对齐createTable"存在即返回"），否则eSplittingBucketExist让
					// agent端抛异常，源桶splittingMeta为null永远到不了endSplit，分桶永久卡死。
					logger.info("bucket exist, resume. database={} table={}", databaseName, tableName);
					r.Result = exist;
					r.SendResult();
					return 0;
				}
				// 同keyFirst四元组不等（碰撞）。持splitting锁期间不能嵌套取主表锁判INV1（锁序
				// 固定主表→splitting）——放锁后按序两阶段复查（consumeDeadSplittingOnCollision）。
			} finally {
				table.unlock();
			}
			if (!consumeDeadSplittingOnCollision(tableName, mainTable, table, bucket)) {
				logger.info("bucket exist but mismatch (in-flight). database={} table={}", databaseName, tableName);
				return master.errorCode(Master.eSplittingBucketExist);
			}
			// 死信已消费（或条目已消失/已变）：循环重查——另一并发creator可能在两阶段间隙
			// 重建了条目，重查按同四元组resume或再碰撞（真在途则维持拒绝）。每轮不返回即
			// 必消费一条死信，循环有进展保证。
		}
	}

	/**
	 * 碰撞条目的INV1死信判定与消费。判死依据（不变式INV1）：条目[k,K)活⟺
	 * 主表floor(k).keyLast==K——主表只会收窄不会变宽、splitting消费与主表收窄同批落盘，
	 * keyLast不等且**更窄**的唯一构造路径是"更晚的settle已越过该keyFirst收窄主表"=死信
	 * （变体的eSplittingBucketExist永久拒绝由此转一次自愈：删除后按新请求重建）。
	 * 更宽方向不判死：那是本迁移之前另有settle丢失、主表陈旧（条目比主表新，仍活），
	 * 留给pending-settle补发收敛；floor不存在（生产不可达：主表首桶keyFirst=Empty
	 * 覆盖一切key）也不判死——结构证明不足时保守拒绝，不为不可达形态引入误删面。
	 * 锁序固定主表→splitting（settleSplitting同款）：两阶段复查防消费窗口内条目被并发
	 * 消费/重建。
	 * @return true=条目已消费，或已消失/已变为本请求的幂等重试形态（调用方重查）；
	 *         false=真在途，维持eSplittingBucketExist。
	 */
	private boolean consumeDeadSplittingOnCollision(String tableName, MasterTable.Data mainTable,
													MasterTable.Data splittingTable, BBucketMeta.Data request)
			throws RocksDBException {
		mainTable.lock();
		try {
			splittingTable.lock();
			try {
				var exist = splittingTable.buckets.get(request.getKeyFirst());
				if (null == exist || sameBucketMeta(exist, request))
					return true; // 条目已消失，或并发下已变为本请求的同四元组——交回主流程按原语义处理
				var floor = mainTable.buckets.floorEntry(request.getKeyFirst());
				if (null == floor || compareKeyLast(floor.getValue().getKeyLast(), exist.getKeyLast()) >= 0)
					return false; // 结构证明不足或真在途：维持拒绝
				logger.error("dead splitting entry consumed on collision (INV1): database={} table={} entry={}",
						databaseName, tableName, exist);
				var dead = splittingTable.buckets.remove(request.getKeyFirst());
				try {
					saveRocks(rocksSplitting, tableName, splittingTable);
				} catch (RocksDBException ex) {
					// 同settleSplitting的失败回滚（save失败内存还原，重试按未消费语义进行）。
					if (null != dead)
						splittingTable.buckets.put(request.getKeyFirst(), dead);
					throw ex;
				}
				splittingAgeRemoveQuietly(tableName, request.getKeyFirst());
				return true;
			} finally {
				splittingTable.unlock();
			}
		} finally {
			mainTable.unlock();
		}
	}

	// 年龄副表key：长度前缀tableName + keyFirst（同表内条目唯一）。
	private static byte[] splittingAgeKey(String tableName, Binary keyFirst) {
		var tn = tableName.getBytes(StandardCharsets.UTF_8);
		var key = new byte[4 + tn.length + keyFirst.size()];
		key[0] = (byte)(tn.length >>> 24);
		key[1] = (byte)(tn.length >>> 16);
		key[2] = (byte)(tn.length >>> 8);
		key[3] = (byte)tn.length;
		System.arraycopy(tn, 0, key, 4, tn.length);
		System.arraycopy(keyFirst.bytesUnsafe(), keyFirst.getOffset(), key, 4 + tn.length, keyFirst.size());
		return key;
	}

	// 观测专用写入：失败只记error不阻断协议路径——年龄基线在下一轮扫描按首扫起点重建。
	private void splittingAgeCreateQuietly(String tableName, Binary keyFirst) {
		try {
			var key = splittingAgeKey(tableName, keyFirst);
			var bb = ByteBuffer.Allocate(8);
			bb.WriteLong(System.currentTimeMillis());
			rocksSplittingAge.put(key, 0, key.length, bb.Bytes, 0, bb.WriteIndex);
		} catch (RocksDBException ex) {
			logger.error("splittingAgeCreate fail (observation only). database={} table={}", databaseName, tableName, ex);
		}
	}

	private void splittingAgeRemoveQuietly(String tableName, Binary keyFirst) {
		try {
			var key = splittingAgeKey(tableName, keyFirst);
			rocksSplittingAge.delete(key, 0, key.length);
		} catch (RocksDBException ex) {
			// 残留记录只多占空间，不被扫描读取（扫描以splitting现存条目为驱动），无正确性影响。
			logger.error("splittingAgeRemove fail (observation only). database={} table={}", databaseName, tableName, ex);
		}
	}

	/** 条目创建时间戳（年龄观测用）；无记录（存量条目未立基线）返回null。 */
	public Long getSplittingAgeCreateTime(String tableName, Binary keyFirst) {
		try {
			var ts = rocksSplittingAge.get(splittingAgeKey(tableName, keyFirst));
			return null != ts ? ByteBuffer.Wrap(ts).ReadLong() : null;
		} catch (RocksDBException ex) {
			logger.error("getSplittingAgeCreateTime fail. database={} table={}", databaseName, tableName, ex);
			return null;
		}
	}

	private record AgedEntry(BBucketMeta.Data entry, long ageMs) {
	}

	/**
	 * splitting年龄扫描：超龄（≥SplittingAgeWarnMs）条目error告警
	 * （含条目与源桶信息），返回告警条数。**只观测不动作**——观测可时间驱动，消费必须
	 * 结构驱动；无时间戳的存量条目按首次扫描起点起算（首扫只立基线不告警）。
	 * 告警取源桶信息需读主表floor：锁序主表→splitting，故在splitting锁外逐条取主表锁
	 * （splitting锁内嵌套取主表锁违反锁序）；条目为锁内copy快照，与消费并发时最多告警
	 * 一条已消失的条目（观测无害）。
	 */
	public int scanSplittingAge() {
		var now = System.currentTimeMillis();
		var warnMs = master.getDbh2Config().getSplittingAgeWarnMs();
		var aged = new ArrayList<AgedEntry>();
		try {
			for (var e : splitting.entrySet()) {
				var tableName = e.getKey();
				var splittingTable = e.getValue();
				splittingTable.lock(); // 与settle/createSplitBucket/死信消费的splitting写互斥
				try {
					for (var b : splittingTable.buckets.entrySet()) {
						var keyFirst = b.getKey();
						var ts = rocksSplittingAge.get(splittingAgeKey(tableName, keyFirst));
						if (null == ts) {
							splittingAgeCreateQuietly(tableName, keyFirst); // 存量条目：首扫起点起算
							continue;
						}
						var ageMs = now - ByteBuffer.Wrap(ts).ReadLong();
						if (ageMs >= warnMs)
							aged.add(new AgedEntry(b.getValue().copy(), ageMs));
					}
				} finally {
					splittingTable.unlock();
				}
			}
		} catch (RocksDBException ex) {
			logger.error("scanSplittingAge fail. database={}", databaseName, ex);
		}
		for (var a : aged) {
			BBucketMeta.Data source = null;
			var mainTable = tables.get(a.entry().getTableName());
			if (null != mainTable) {
				mainTable.lock();
				try {
					var floor = mainTable.buckets.floorEntry(a.entry().getKeyFirst());
					source = null != floor ? floor.getValue() : null;
				} finally {
					mainTable.unlock();
				}
			}
			logger.error("splitting entry aged ({}ms >= {}ms), entry={} source={}",
					a.ageMs(), warnMs, a.entry(), source);
		}
		return aged.size();
	}

	private static void saveRocks(RocksDatabase.Table rocksTable,
								  String tableName, MasterTable.Data table) throws RocksDBException {
		// master数据马上存数据库。
		var bbValue = table.encode();
		var key = tableName.getBytes(StandardCharsets.UTF_8);
		rocksTable.put(key, 0, key.length, bbValue.Bytes, 0, bbValue.WriteIndex);
	}
}
