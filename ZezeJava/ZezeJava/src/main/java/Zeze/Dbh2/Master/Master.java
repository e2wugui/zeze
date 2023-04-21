package Zeze.Dbh2.Master;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.Master.BRegister;
import Zeze.Builtin.Dbh2.Master.CreateDatabase;
import Zeze.Builtin.Dbh2.Master.CreateTable;
import Zeze.Builtin.Dbh2.Master.GetBuckets;
import Zeze.Builtin.Dbh2.Master.LocateBucket;
import Zeze.Builtin.Dbh2.Master.Register;
import Zeze.Net.AsyncSocket;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.OutObject;
import Zeze.Util.RocksDatabase;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class Master extends AbstractMaster {
	public static final String MasterDbName = "__master__";

	private final ConcurrentHashMap<String, MasterDatabase> databases = new ConcurrentHashMap<>();
	private final String home;

	// todo 可用Dbh2Manager的数据结构。
	public static class Manager {
		public final AsyncSocket socket;
		public final BRegister.Data data;

		public Manager(AsyncSocket socket, BRegister.Data data) {
			this.socket = socket;
			this.data = data;
		}
	}

	private final ArrayList<Manager> managers = new ArrayList<>();
	private int choiceIndex;
	private final RocksDB masterDb;

	public Master(String home) throws RocksDBException {
		this.home = home;

		masterDb = RocksDatabase.open(RocksDatabase.getCommonOptions(), Path.of(home, MasterDbName).toString());

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
		var seed = 10000;
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

	public synchronized ArrayList<Manager> choiceManagers() {
		var result = new ArrayList<Manager>();
		if (managers.size() < 3)
			return result;

		var last = choiceIndex;
		var c = 0;
		while (c < 3) {
			var m = managers.get(choiceIndex);
			result.add(m);
			++c;

			// 先推进到下一个，并且判断是否绕回到开始的索引。
			choiceIndex = (choiceIndex + 1) % managers.size();
			if (choiceIndex == last)
				break; // 绕回来了。
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
}
