package Zeze.Dbh2.Master;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.Dbh2.Master.BRegister;
import Zeze.Builtin.Dbh2.Master.CreateDatabase;
import Zeze.Builtin.Dbh2.Master.CreateTable;
import Zeze.Builtin.Dbh2.Master.GetBuckets;
import Zeze.Builtin.Dbh2.Master.LocateBucket;
import Zeze.Builtin.Dbh2.Master.Register;
import Zeze.Dbh2.Bucket;
import Zeze.Net.AsyncSocket;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.OutObject;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class Master extends AbstractMaster {
	public static final String MasterDbName = "__master__";

	private final ConcurrentHashMap<String, MasterDatabase> databases = new ConcurrentHashMap<>();
	private final String home;

	// todo 可用Dbh2Manager的数据结构。
	private final HashMap<AsyncSocket, BRegister.Data> managers = new HashMap<>();
	private RocksDB masterDb;

	public Master(String home) throws RocksDBException {
		this.home = home;

		var masterDbFile = new File(home, MasterDbName);
		masterDbFile.mkdirs();
		masterDb = RocksDB.open(masterDbFile.toString());

		var dbs = new File(home).listFiles();
		if (null != dbs) {
			for (var db : dbs) {
				if (!db.isDirectory())
					continue;
				if (db.getName().equals(MasterDbName)) {
					continue;
				}
				databases.computeIfAbsent(db.getName(), (dbName) -> new MasterDatabase(this, dbName));
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
		var seed = 0;
		var seedKey = acceptorName.getBytes(StandardCharsets.UTF_8);
		var seedValue = masterDb.get(Bucket.getDefaultReadOptions(), seedKey);
		if (null != seedValue) {
			seed = ByteBuffer.Wrap(seedValue).ReadInt();
		}
		seed ++;
		var bb = ByteBuffer.Allocate();
		bb.WriteInt(seed);
		masterDb.put(Bucket.getDefaultWriteOptions(), seedKey, 0, seedKey.length, bb.Bytes, bb.ReadIndex, bb.size());
		return seed;
	}

	public HashMap<AsyncSocket, BRegister.Data> choiceManagers() {
		// todo 选择规则。。。大大的。
		var result = new HashMap<AsyncSocket, BRegister.Data>();
		int i = 0;
		for (var e : managers.entrySet()) {
			result.put(e.getKey(), e.getValue());
			if (++i == 3)
				break;
		}
		return result;
	}

	@Override
	protected long ProcessCreateDatabaseRequest(CreateDatabase r) throws Exception {
		databases.computeIfAbsent(r.Argument.getDatabase(), (dbName) -> new MasterDatabase(this, dbName));
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
		if (outIsNew.value)
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
		managers.put(r.getSender(), r.Argument);
		r.SendResult();
		return 0;
	}
}
