package Zeze.Dbh2.Master;

import java.util.Objects;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.Master.*;
import Zeze.Config;
import Zeze.Dbh2.Dbh2Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Database;
import Zeze.Transaction.DatabaseMySql;
import Zeze.Util.DaemonTimer;
import Zeze.Util.OutObject;
import Zeze.Util.PropertiesHelper;
import Zeze.Util.RocksDatabase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;
import org.jspecify.annotations.Nullable;

/**
 * Dbh2 Master 服务：管理数据库与表的元数据、manager 注册与负载调度、全局实例数据。
 */
public class Master extends AbstractMaster {
	private static final Logger logger = LogManager.getLogger(Master.class);
	public static final String MasterDbName = "__master__";

	// 主表陈旧宽拒绝的可重试错误码（区别于终局码 eSplittingBucketNotFound），保留给 MasterAgent 重试链待主表收敛后重发
	public static final int eSplittingStaleMain = 8;

	private final ConcurrentHashMap<String, MasterDatabase> databases = new ConcurrentHashMap<>();
	private final String home;

	public static class Manager {
		public final AsyncSocket socket;
		public final BRegister.Data data;
		public double load;
		public boolean ready;

		public Manager(AsyncSocket socket, BRegister.Data data) {
			this.socket = socket;
			this.data = data;
			this.ready = false;
		}
	}

	private final ArrayList<Manager> managers = new ArrayList<>();
	private final RocksDatabase masterDb;
	private final RocksDatabase.Table seedTable;
	private final RocksDatabase.Table zezeInstanceTable;
	private final RocksDatabase.Table zezeDataTable;
	private final Dbh2Config dbh2Config = new Dbh2Config();
	private final Config zezeConfig;

	// 周期扫描 splitting 条目：超龄且结构判死的回收孤儿 raft，超龄但存活的仅告警
	private final DaemonTimer splittingAgeMonitor = new DaemonTimer(
			"Zeze.Dbh2.Master.splittingAge", 60_000, this::scanSplittingAges);

	public Dbh2Config getDbh2Config() {
		return dbh2Config;
	}

	public Config getZezeConfig() {
		return zezeConfig;
	}

	public Master(String home, Config zezeConfig) throws RocksDBException {
		this.home = home;
		this.zezeConfig = zezeConfig;
		zezeConfig.parseCustomize(dbh2Config);
		masterDb = new RocksDatabase(Path.of(home, MasterDbName).toString(),
				RocksDatabase.DbType.eOptimisticTransactionDb);
		seedTable = masterDb.getOrAddTable("seed");
		zezeInstanceTable = masterDb.getOrAddTable("instance");
		zezeDataTable = masterDb.getOrAddTable("data");

		var dbs = new File(home).listFiles();
		if (null != dbs) {
			for (var db : dbs) {
				if (!db.isDirectory())
					continue;
				if (db.getName().equals(MasterDbName)) {
					continue;
				}
				databases.computeIfAbsent(db.getName(), dbName -> new MasterDatabase(this, dbName));
			}
		}
	}

	private void scanSplittingAges() {
		for (var db : databases.values())
			db.scanSplittingAge();
	}

	/**
	 * 启动 splitting 年龄扫描与孤儿回收守护任务（由 Main.start() 调用，构造期不自动启动）。
	 */
	public void startSplittingAgeMonitor() {
		splittingAgeMonitor.start();
	}

	public String getHome() {
		return home;
	}

	public void close() {
		splittingAgeMonitor.stop(); // 先停扫描再关库，避免扫描触达已关闭的rocks句柄
		for (var db : databases.values())
			db.close();
		databases.clear();
		masterDb.close();
	}

	public int nextBucketPortId(String acceptorName) throws RocksDBException {
		lock();
		try {
			// seedTable 不使用事务。
			var seed = PropertiesHelper.getInt("Dbh2MasterDefaultBucketPortId", 10000);
			var seedKey = acceptorName.getBytes(StandardCharsets.UTF_8);
			var seedValue = seedTable.get(RocksDatabase.getDefaultReadOptions(), seedKey);
			if (null != seedValue) {
				seed = ByteBuffer.Wrap(seedValue).ReadInt();
			}
			seed++;
			var bb = ByteBuffer.Allocate(5);
			bb.WriteInt(seed);
			// 端口一经用于建桶即成为持久身份，断电后不得回退并复用已有桶目录。
			seedTable.put(RocksDatabase.getSyncWriteOptions(), seedKey,
					0, seedKey.length, bb.Bytes, 0, bb.WriteIndex);
			return seed;
		} finally {
			unlock();
		}
	}

	public void tryRemoveManager(AsyncSocket manager) {
		lock();
		try {
			for (int i = 0; i < managers.size(); ++i) {
				var e = managers.get(i);
				if (e.socket == manager) {
					managers.remove(i);
					break;
				}
			}
		} finally {
			unlock();
		}
	}

	static class ManagerLoadComparator implements Comparator<Manager> {
		@Override
		public int compare(Manager o1, Manager o2) {
			return Double.compare(o1.load, o2.load);
		}
	}

	public Manager[] shadowReadyManager() {
		Manager[] shadow;
		lock();
		try {
			var readies = new ArrayList<Manager>();
			for (var manager : managers)
				if (manager.ready)
					readies.add(manager);
			shadow = new Manager[readies.size()];
			readies.toArray(shadow);
		} finally {
			unlock();
		}
		return shadow;
	}

	public ArrayList<Manager> choiceSmallLoadManagers() {
		// 排序和筛选会读manager.load/bucketCount，与写路径（ReportLoad/ReportBucketCount）同锁，对齐choiceManagers。
		lock();
		try {
			Manager[] shadow = shadowReadyManager();
			Arrays.sort(shadow, new ManagerLoadComparator());
			return choiceSmallLoadManagers(shadow);
		} finally {
			unlock();
		}
	}

	static class ManagerBucketCountComparator implements Comparator<Manager> {
		@Override
		public int compare(Manager o1, Manager o2) {
			return Integer.compare(o1.data.getBucketCount(), o2.data.getBucketCount());
		}
	}

	// 优先选择负载与桶数较小的 manager
	public ArrayList<Manager> choiceManagers() {
		lock();
		try {
			Manager[] shadow = shadowReadyManager();
			Arrays.sort(shadow, new ManagerBucketCountComparator());
			return choiceSmallLoadManagers(shadow);
		} finally {
			unlock();
		}
	}

	public ArrayList<Manager> choiceSmallLoadManagers(Manager[] managers) {
		var result = new ArrayList<Manager>();
		var count = 0;
		for (var manager : managers) {
			if (manager.load < dbh2Config.getSplitMaxManagerLoad()) {
				result.add(manager);
				if (++count >= dbh2Config.getRaftClusterCount())
					break;
			}
		}
		return result;
	}

	@Override
	protected long ProcessCreateDatabaseRequest(CreateDatabase r) throws Exception {
		databases.computeIfAbsent(r.Argument.getDatabase(), dbName -> new MasterDatabase(this, dbName));
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessCreateTableRequest(CreateTable r) throws Exception {
		logger.info("CreateTable: db={}, table={}", r.Argument.getDatabase(), r.Argument.getTable());
		var database = databases.get(r.Argument.getDatabase());
		if (null == database)
			return errorCode(eDatabaseNotFound);
		var outIsNew = new OutObject<Boolean>();
		var table = database.createTable(r.Argument.getTable(), outIsNew);
		if (null == table)
			return errorCode(eTableNotFound);
		// 返回快照，避免 RPC 序列化遍历与分桶写路径并发修改 TreeMap
		r.Result = table.snapshot();
		if (Boolean.TRUE.equals(outIsNew.value))
			r.setResultCode(errorCode(eTableIsNew));
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessGetBucketsRequest(GetBuckets r) throws Exception {
		var database = databases.get(r.Argument.getDatabase());
		if (null == database)
			return errorCode(eDatabaseNotFound);
		var table = database.getTable(r.Argument.getTable());
		if (null == table)
			return errorCode(eTableNotFound);
		r.Result = table.snapshot();
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessLocateBucketRequest(LocateBucket r) throws Exception {
		var database = databases.get(r.Argument.getDatabase());
		if (null == database)
			return errorCode(eDatabaseNotFound);
		var table = database.getTable(r.Argument.getTable());
		if (null == table)
			return errorCode(eTableNotFound);
		// 持锁定位；桶 meta 写路径均为整体替换，锁外发送安全
		Zeze.Builtin.Dbh2.BBucketMeta.Data bucket;
		table.lock();
		try {
			bucket = table.locate(r.Argument.getKey());
		} finally {
			table.unlock();
		}
		r.Result = Objects.requireNonNull(bucket);
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessRegisterRequest(Register r) throws Exception {
		var managerHostPort = r.Argument.getDbh2RaftAcceptorName() + "_" + r.Argument.getPort();
		lock();
		try {
			// 按 socket 或 acceptor:port 幂等去重，替换旧条目防止断线重连后残留重复或死 socket
			for (int i = 0; i < managers.size(); ++i) {
				var e = managers.get(i);
				if (e.socket == r.getSender()
						|| (e.data.getDbh2RaftAcceptorName() + "_" + e.data.getPort()).equals(managerHostPort)) {
					managers.remove(i);
					break;
				}
			}
			managers.add(new Manager(Objects.requireNonNull(r.getSender()), r.Argument));
		} finally {
			unlock();
		}
		// 必须在 master 锁外逐表 snapshot()，避免与 createTable 的 table.lock → master.lock 构成 ABBA 死锁
		for (var db : databases.values()) {
			for (var table : db.getTables().entrySet()) {
				for (var bucket : table.getValue().snapshot().getBuckets().values()) {
					var raftName = bucket.getHost2Raft().get(managerHostPort);
					if (raftName != null) {
						var dbh2Config = new BDbh2Config.Data();
						dbh2Config.setDatabase(db.getDatabaseName());
						dbh2Config.setTable(table.getKey());
						dbh2Config.setRaftConfig(bucket.getRaftConfig().replace("RaftName", raftName));
						r.Result.getDbh2Configs().add(dbh2Config);
					}
				}
			}
		}
		r.SendResult();
		return 0;
	}

	private @Nullable Manager findManager(AsyncSocket sender) {
		for (var manager : managers)
			if (manager.socket == sender)
				return manager;
		return null;
	}

	// 按注册身份（acceptorName_port）查找 manager，供孤儿 raft 回收使用
	public @Nullable Manager findManagerByIdentity(String managerHostPort) {
		lock();
		try {
			for (var e : managers)
				if ((e.data.getDbh2RaftAcceptorName() + "_" + e.data.getPort()).equals(managerHostPort))
					return e;
			return null;
		} finally {
			unlock();
		}
	}

	@Override
	protected long ProcessReportLoadRequest(ReportLoad r) throws Exception {
		lock();
		try {
			var manager = findManager(Objects.requireNonNull(r.getSender()));
			if (null == manager)
				return errorCode(eManagerNotFound);
			manager.load = r.Argument.getLoad();
			r.SendResult();
			return 0;
		} finally {
			unlock();
		}
	}

	@Override
	protected long ProcessCreateSplitBucketRequest(CreateSplitBucket r) throws Exception {
		var database = databases.get(r.Argument.getDatabaseName());
		if (null == database)
			return errorCode(eDatabaseNotFound);
		return database.createSplitBucket(r);
	}

	@Override
	protected long ProcessEndMoveRequest(EndMove r) throws Exception {
		var database = databases.get(r.Argument.getTo().getDatabaseName());
		if (null == database)
			return errorCode(eDatabaseNotFound);
		return database.endMove(r);
	}

	@Override
	protected long ProcessEndSplitRequest(EndSplit r) throws Exception {
		var database = databases.get(r.Argument.getFrom().getDatabaseName());
		if (null == database)
			return errorCode(eDatabaseNotFound);
		return database.endSplit(r);
	}

	@Override
	protected long ProcessReportBucketCountRequest(ReportBucketCount r) throws Exception {
		lock();
		try {
			var manager = findManager(Objects.requireNonNull(r.getSender()));
			if (null == manager)
				return errorCode(eManagerNotFound);
			manager.data.setBucketCount(r.Argument.getCount());
			r.SendResult();
			return 0;
		} finally {
			unlock();
		}
	}

	@Override
	protected long ProcessCheckFreeManagerRequest(CheckFreeManager r) {
		var managers = choiceSmallLoadManagers();
		r.Result.setCount(managers.size());
		r.SendResult();
		return 0;
	}

	private final static byte[] emptyValue = new byte[0];

	@Override
	protected long ProcessSetInUseRequest(SetInUse r) throws Exception {
		lock();
		try (var trans = masterDb.beginOptimisticTransaction()) {
			r.setResultCode(errorCode(BSetInUse.eDefaultError));

			var bbKey = ByteBuffer.Allocate();
			bbKey.WriteInt(r.Argument.getLocalId());

			// insert instance
			var existInstance = zezeInstanceTable.get(bbKey.Bytes, bbKey.ReadIndex, bbKey.size());
			if (null != existInstance) {
				// finally 中直接 SendResult，早退路径须预置 resultCode 以确保发送准确错误码
				r.setResultCode(errorCode(BSetInUse.eInstanceAlreadyExists));
				return errorCode(BSetInUse.eInstanceAlreadyExists);
			}
			r.setResultCode(errorCode(BSetInUse.eInsertInstanceError));
			zezeInstanceTable.put(trans, bbKey.Bytes, bbKey.ReadIndex, bbKey.size(),
					emptyValue, 0, emptyValue.length);

			// check global
			var currentGlobal = zezeDataTable.get(emptyValue);
			if (null != currentGlobal) {
				if (0 != Arrays.compare(currentGlobal, r.Argument.getGlobal().getBytes(StandardCharsets.UTF_8))) {
					r.setResultCode(errorCode(BSetInUse.eGlobalNotSame));
					return errorCode(BSetInUse.eGlobalNotSame);
				}
			} else {
				r.setResultCode(errorCode(BSetInUse.eInsertGlobalError));
				zezeDataTable.put(trans, emptyValue, r.Argument.getGlobal().getBytes(StandardCharsets.UTF_8));
			}

			// check instance count
			try (var it = zezeInstanceTable.iterator()) {
				for (it.seekToFirst(); RocksDatabase.isValidOrThrow(it); it.next()) {
					var exist = it.key();
					if (0 == Arrays.compare(bbKey.Bytes, bbKey.ReadIndex, bbKey.WriteIndex, exist, 0, exist.length))
						continue;
					// 忽略本事务新写入的 key；若仍发现其他实例，则实例数 > 1（参见 DatabaseMySql _ZezeSetInUse_）
					if (r.Argument.getGlobal().isEmpty()) {
						r.setResultCode(errorCode(BSetInUse.eTooManyInstanceWithoutGlobal));
						return errorCode(BSetInUse.eTooManyInstanceWithoutGlobal);
					}
				}
			}
			// eSuccess 必须在 commit() 之后置位，防止提交异常时把失败事务误发为成功
			trans.commit();
			r.setResultCode(errorCode(BSetInUse.eSuccess));
		} finally {
			r.SendResult(); // 错误码已预先填写，异常时也能发送对应结果
			unlock();
		}
		return 0;
	}

	@Override
	protected long ProcessClearInUseRequest(ClearInUse r) throws Exception {
		lock();

		try (var trans = masterDb.beginOptimisticTransaction()) {
			r.setResultCode(errorCode(BClearInUse.eDefaultError));

			var bbKey = ByteBuffer.Allocate();
			bbKey.WriteInt(r.Argument.getLocalId());

			r.setResultCode(errorCode(BClearInUse.eInstanceNotExists));
			zezeInstanceTable.delete(trans, bbKey.Bytes, bbKey.ReadIndex, bbKey.size());

			// check instance count
			try (var it = zezeInstanceTable.iterator()) {
				for (it.seekToFirst(); RocksDatabase.isValidOrThrow(it); it.next()) {
					var exist = it.key();
					if (0 == Arrays.compare(bbKey.Bytes, bbKey.ReadIndex, bbKey.WriteIndex, exist, 0, exist.length))
						continue;
					// 忽略本事务删除的 key；仍有其他实例存在，保留 global 直接提交返回（参见 DatabaseMySql _ZezeClearInUse_）
					trans.commit();
					r.setResultCode(errorCode(BClearInUse.eSuccess));
					return 0;
				}
			}
			// 到达这里表示zezeInstanceTable为空或者只存在将被删除的key。
			zezeDataTable.delete(trans, emptyValue);
			trans.commit();
			r.setResultCode(errorCode(BClearInUse.eSuccess));
		} finally {
			r.SendResult();
			unlock();
		}
		return 0;
	}

	@Override
	protected long ProcessSaveDataWithSameVersionRequest(SaveDataWithSameVersion r) throws Exception {
		lock();

		try (var trans = masterDb.beginOptimisticTransaction()) {
			r.setResultCode(errorCode(BSaveDataWithSameVersion.eDefaultError));
			var id = r.Argument.getKey();
			var exist = zezeDataTable.get(id.bytesUnsafe(), id.getOffset(), id.size());
			var bbData = ByteBuffer.Allocate();
			var newVersion = r.Argument.getVersion();
			if (null != exist) {
				var dvExist = Database.DataWithVersion.decode(exist);
				if (dvExist.version != r.Argument.getVersion()) {
					r.setResultCode(errorCode(BSaveDataWithSameVersion.eVersionMismatch));
					return errorCode(BSaveDataWithSameVersion.eVersionMismatch);
				}
				newVersion++;
				dvExist.version = newVersion;
				dvExist.data = ByteBuffer.Wrap(r.Argument.getData());
				dvExist.encode(bbData);
			} else {
				var dvInsert = new Database.DataWithVersion();
				dvInsert.version = r.Argument.getVersion();
				dvInsert.data = ByteBuffer.Wrap(r.Argument.getData());
				dvInsert.encode(bbData);
			}
			r.setResultCode(errorCode(BSaveDataWithSameVersion.eUpdateError));
			zezeDataTable.put(trans, id.bytesUnsafe(), id.getOffset(), id.size(),
					bbData.Bytes, bbData.ReadIndex, bbData.size());
			trans.commit();
			r.setResultCode(errorCode(BSaveDataWithSameVersion.eSuccess));
			r.Result.setVersion(newVersion);
			return 0;
		} finally {
			r.SendResult();
			unlock();
		}
	}

	@Override
	protected long ProcessSetDbh2ReadyRequest(SetDbh2Ready r) throws Exception {
		// 设置dbh2进入准备好模式，可以接受新的bucket的分配等操作。
		lock();
		try {
			var manager = findManager(Objects.requireNonNull(r.getSender()));
			if (null == manager)
				return errorCode(eManagerNotFound);
			manager.ready = true;
			r.SendResult();
		} finally {
			unlock();
		}
		return 0;
	}

	@Override
	protected long ProcessGetDataWithVersionRequest(GetDataWithVersion r) throws Exception {
		lock();
		try {
			var id = r.Argument.getKey();
			var exist = zezeDataTable.get(id.bytesUnsafe(), id.getOffset(), id.size());
			if (null == exist)
				return errorCode(BGetDataWithVersion.eDataNotExists);
			var dv = Database.DataWithVersion.decode(exist);
			r.Result.setData(new Binary(Objects.requireNonNull(dv.data)));
			r.Result.setVersion(dv.version);
			r.SendResult(); // 这个流程的错误码都是预先填写好的，异常发生的时候可以正确的发送结果，框架捕捉的错误的发送操作会被忽略。
			return 0;
		} finally {
			unlock();
		}
	}

	@Override
	protected long ProcessTryLockRequest(TryLock r) throws Exception {
		lock();
		try {
			var exist = zezeDataTable.get(DatabaseMySql.keyOfLock);
			var bbData = ByteBuffer.Allocate();
			if (exist != null) {
				var dvExist = Database.DataWithVersion.decode(exist);
				if (dvExist.version != 0)
					return errorCode(TryLock.eLockNotExists);

				dvExist.version = 1;
				dvExist.encode(bbData);
			} else {
				var dvInsert = new Database.DataWithVersion();
				dvInsert.data = ByteBuffer.Allocate();
				dvInsert.version = 1;
				dvInsert.encode(bbData);
			}
			zezeDataTable.put(DatabaseMySql.keyOfLock, 0, DatabaseMySql.keyOfLock.length,
					bbData.Bytes, bbData.ReadIndex, bbData.size());
			r.SendResult();
			return 0;
		} finally {
			unlock();
		}
	}

	@Override
	protected long ProcessUnLockRequest(UnLock r) throws Exception {
		// 完全忽略错误的写法是直接put一个干净的记录(version==0)到表内。
		lock();
		try {
			var exist = zezeDataTable.get(DatabaseMySql.keyOfLock);
			if (exist == null)
				return errorCode(TryLock.eLockNotExists);

			var dv = Database.DataWithVersion.decode(exist);
			dv.version = 0;
			var bbData = ByteBuffer.Allocate();
			dv.encode(bbData);
			zezeDataTable.put(DatabaseMySql.keyOfLock, 0, DatabaseMySql.keyOfLock.length,
					bbData.Bytes, bbData.ReadIndex, bbData.size());
			r.SendResult();
			return 0;
		} finally {
			unlock();
		}
	}

}
