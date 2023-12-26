package Zeze.Dbh2.Master;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.Master.BRegister;
import Zeze.Builtin.Dbh2.Master.CheckFreeManager;
import Zeze.Builtin.Dbh2.Master.ClearInUse;
import Zeze.Builtin.Dbh2.Master.CreateDatabase;
import Zeze.Builtin.Dbh2.Master.CreateSplitBucket;
import Zeze.Builtin.Dbh2.Master.CreateTable;
import Zeze.Builtin.Dbh2.Master.EndMove;
import Zeze.Builtin.Dbh2.Master.EndSplit;
import Zeze.Builtin.Dbh2.Master.GetBuckets;
import Zeze.Builtin.Dbh2.Master.GetDataWithVersion;
import Zeze.Builtin.Dbh2.Master.LocateBucket;
import Zeze.Builtin.Dbh2.Master.Register;
import Zeze.Builtin.Dbh2.Master.ReportBucketCount;
import Zeze.Builtin.Dbh2.Master.ReportLoad;
import Zeze.Builtin.Dbh2.Master.SaveDataWithSameVersion;
import Zeze.Builtin.Dbh2.Master.SetInUse;
import Zeze.Builtin.Dbh2.Master.TryLock;
import Zeze.Builtin.Dbh2.Master.UnLock;
import Zeze.Config;
import Zeze.Dbh2.Dbh2Config;
import Zeze.Net.AsyncSocket;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;
import Zeze.Util.OutObject;
import Zeze.Util.PropertiesHelper;
import Zeze.Util.RocksDatabase;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class Master extends AbstractMaster {
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
	private final RocksDB masterDb;
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
		masterDb = RocksDatabase.open(Path.of(home, MasterDbName).toString());

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

	public synchronized int nextBucketPortId(String acceptorName) throws RocksDBException {
		var seed = PropertiesHelper.getInt("Dbh2MasterDefaultBucketPortId", 10000);
		var seedKey = acceptorName.getBytes(StandardCharsets.UTF_8);
		var seedValue = masterDb.get(RocksDatabase.getDefaultReadOptions(), seedKey);
		if (null != seedValue) {
			seed = ByteBuffer.Wrap(seedValue).ReadInt();
		}
		seed++;
		var bb = ByteBuffer.Allocate(5);
		bb.WriteInt(seed);
		masterDb.put(RocksDatabase.getDefaultWriteOptions(), seedKey, 0, seedKey.length, bb.Bytes, 0, bb.WriteIndex);
		return seed;
	}

	public synchronized void tryRemoveManager(AsyncSocket manager) {
		for (int i = 0; i < managers.size(); ++i) {
			var e = managers.get(i);
			if (e.socket == manager) {
				managers.remove(i);
				break;
			}
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
		synchronized (this) {
			shadow = new Manager[managers.size()];
			managers.toArray(shadow);
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
	public synchronized ArrayList<Manager> choiceManagers() {
		Manager[] shadow = shadowManager();
		Arrays.sort(shadow, new ManagerBucketCountComparator());
		return choiceSmallLoadManagers(shadow);
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
	protected synchronized long ProcessRegisterRequest(Register r) throws Exception {
		managers.add(new Manager(r.getSender(), r.Argument));
		r.SendResult();
		return 0;
	}

	private Manager findManager(AsyncSocket sender) {
		for (var manager : managers)
			if (manager.socket == sender)
				return manager;
		return null;
	}

	@Override
	protected synchronized long ProcessReportLoadRequest(ReportLoad r) throws Exception {
		var manager = findManager(r.getSender());
		if (null == manager)
			return errorCode(eManagerNotFound);
		manager.load = r.Argument.getLoad();
		r.SendResult();
		return 0;
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
	protected synchronized long ProcessReportBucketCountRequest(ReportBucketCount r) throws Exception {
		var manager = findManager(r.getSender());
		if (null == manager)
			return errorCode(eManagerNotFound);
		manager.data.setBucketCount(r.Argument.getCount());
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessCheckFreeManagerRequest(CheckFreeManager r) {
		var managers = choiceSmallLoadManagers();
		r.Result.setCount(managers.size());
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessSetInUseRequest(SetInUse r) throws Exception {
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessClearInUseRequest(ClearInUse r) throws Exception {
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessSaveDataWithSameVersionRequest(SaveDataWithSameVersion r) throws Exception {
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessGetDataWithVersionRequest(GetDataWithVersion r) throws Exception {
		r.SendResultCode(Procedure.NotImplement);
		return 0;
	}

	@Override
	protected long ProcessTryLockRequest(TryLock r) throws Exception {
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessUnLockRequest(UnLock r) throws Exception {
		r.SendResult();
		return 0;
	}

}
