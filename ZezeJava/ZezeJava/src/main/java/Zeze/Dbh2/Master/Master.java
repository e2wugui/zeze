package Zeze.Dbh2.Master;

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
import Zeze.Util.OutObject;
import Zeze.Util.PropertiesHelper;
import Zeze.Util.RocksDatabase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;

public class Master extends AbstractMaster {
	private static final Logger logger = LogManager.getLogger(Master.class);
	public static final String MasterDbName = "__master__";

	private final ConcurrentHashMap<String, MasterDatabase> databases = new ConcurrentHashMap<>();
	private final String home;

	public static class Manager {
		public final AsyncSocket socket;
		public final BRegister.Data data;
		public double load;

		public Manager(AsyncSocket socket, BRegister.Data data) {
			this.socket = socket;
			this.data = data;
		}
	}

	private final ArrayList<Manager> managers = new ArrayList<>();
	private final RocksDatabase masterDb;
	private final RocksDatabase.Table seedTable;
	private final RocksDatabase.Table zezeInstanceTable;
	private final RocksDatabase.Table zezeDataTable;
	private final Dbh2Config dbh2Config = new Dbh2Config();
	private final Config zezeConfig;

	public Dbh2Config getDbh2Config() {
		return dbh2Config;
	}

	public Config getZezeConfig() {
		return zezeConfig;
	}

	public Master(String home) throws RocksDBException {
		this.home = home;
		zezeConfig = Config.load();
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

	public String getHome() {
		return home;
	}

	public void close() {
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
			seedTable.put(RocksDatabase.getDefaultWriteOptions(), seedKey,
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

	public Manager[] shadowManager() {
		Manager[] shadow;
		lock();
		try {
			shadow = new Manager[managers.size()];
			managers.toArray(shadow);
		} finally {
			unlock();
		}
		return shadow;
	}

	public ArrayList<Manager> choiceSmallLoadManagers() {
		Manager[] shadow = shadowManager();
		Arrays.sort(shadow, new ManagerLoadComparator());
		return choiceSmallLoadManagers(shadow);
	}

	static class ManagerBucketCountComparator implements Comparator<Manager> {
		@Override
		public int compare(Manager o1, Manager o2) {
			return Integer.compare(o1.data.getBucketCount(), o2.data.getBucketCount());
		}
	}

	// small load & small bucket count
	public ArrayList<Manager> choiceManagers() {
		lock();
		try {
			Manager[] shadow = shadowManager();
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
		r.Result = table;
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
		r.Result = table;
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
		r.Result = table.locate(r.Argument.getKey()); // 初始桶保证肯定找得到。
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessRegisterRequest(Register r) throws Exception {
		lock();
		try {
			managers.add(new Manager(r.getSender(), r.Argument));
			r.SendResult();
			return 0;
		} finally {
			unlock();
		}
	}

	private Manager findManager(AsyncSocket sender) {
		for (var manager : managers)
			if (manager.socket == sender)
				return manager;
		return null;
	}

	@Override
	protected long ProcessReportLoadRequest(ReportLoad r) throws Exception {
		lock();
		try {
			var manager = findManager(r.getSender());
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
			var manager = findManager(r.getSender());
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
			if (null != zezeInstanceTable.get(bbKey.Bytes, bbKey.ReadIndex, bbKey.size()))
				return errorCode(BSetInUse.eInstanceAlreadyExists);
			r.setResultCode(errorCode(BSetInUse.eInsertInstanceError));
			zezeInstanceTable.put(trans, bbKey.Bytes, bbKey.ReadIndex, bbKey.size(),
					emptyValue, 0, emptyValue.length);

			// check global
			var currentGlobal = zezeDataTable.get(emptyValue);
			if (null != currentGlobal) {
				if (0 != Arrays.compare(currentGlobal, r.Argument.getGlobal().getBytes(StandardCharsets.UTF_8)))
					return errorCode(BSetInUse.eGlobalNotSame);
			} else {
				r.setResultCode(errorCode(BSetInUse.eInsertGlobalError));
				zezeDataTable.put(trans, emptyValue, r.Argument.getGlobal().getBytes(StandardCharsets.UTF_8));
			}

			// check instance count
			try (var it = zezeInstanceTable.iterator()) {
				for (it.seekToFirst(); it.isValid(); it.next()) {
					var exist = it.key();
					if (0 == Arrays.compare(bbKey.Bytes, bbKey.ReadIndex, bbKey.WriteIndex, exist, 0, exist.length))
						continue;
					// instance count 初始为1，需要计算事务中（上面的zezeInstanceTable.put）刚刚加入的instance。
					// 上面的compare严格的忽略掉刚刚事务中加入的（实际上应该看不到）
					// 这样到达这里instance count已经大于1，可以直接返回错误了。
					// 这个流程请参考 DatabaseMySql procedure _ZezeSetInUse_。
					if (r.Argument.getGlobal().isEmpty())
						return errorCode(BSetInUse.eTooManyInstanceWithoutGlobal);
				}
			}
			r.setResultCode(errorCode(BSetInUse.eSuccess));
			trans.commit();
		} finally {
			r.SendResult(); // 这个流程的错误码都是预先填写好的，异常发生的时候可以正确的发送结果，框架捕捉的错误的发送操作会被忽略。
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
				for (it.seekToFirst(); it.isValid(); it.next()) {
					var exist = it.key();
					if (0 == Arrays.compare(bbKey.Bytes, bbKey.ReadIndex, bbKey.WriteIndex, exist, 0, exist.length))
						continue;
					// instance count 初始为1，需要忽略事务中（上面的zezeInstanceTable.delete(）刚刚删除的。
					// 这样到达这里instance count表示有剩下的。可以直接返回结果了。
					// 这个流程请参考 DatabaseMySql procedure _ZezeClearInUse_。
					r.setResultCode(errorCode(BClearInUse.eSuccess));
					trans.commit();
					return 0; // done;
				}
			}
			// 到达这里表示zezeInstanceTable为空或者只存在将被删除的key。
			zezeDataTable.delete(trans, emptyValue);
			r.setResultCode(errorCode(BClearInUse.eSuccess));
			trans.commit();
		} finally {
			r.SendResult(); // 这个流程的错误码都是预先填写好的，异常发生的时候可以正确的发送结果，框架捕捉的错误的发送操作会被忽略。
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
				if (dvExist.version != r.Argument.getVersion())
					return errorCode(BSaveDataWithSameVersion.eVersionMismatch);
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
			r.setResultCode(errorCode(BSaveDataWithSameVersion.eSuccess));
			r.Result.setVersion(newVersion);
			trans.commit();
			return 0;
		} finally {
			r.SendResult(); // 这个流程的错误码都是预先填写好的，异常发生的时候可以正确的发送结果，框架捕捉的错误的发送操作会被忽略。
			unlock();
		}
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
			r.Result.setData(new Binary(dv.data));
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
