package Zeze.Dbh2.Master;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.Dbh2.Master.BRegister;
import Zeze.Builtin.Dbh2.Master.CreateDatabase;
import Zeze.Builtin.Dbh2.Master.CreateTable;
import Zeze.Builtin.Dbh2.Master.GetBuckets;
import Zeze.Builtin.Dbh2.Master.LocateBucket;
import Zeze.Builtin.Dbh2.Master.Register;
import Zeze.Net.AsyncSocket;
import Zeze.Util.OutObject;

public class Master extends AbstractMaster {
	private final ConcurrentHashMap<String, MasterDatabase> databases = new ConcurrentHashMap<>();

	// todo 可用Dbh2Manager的数据结构。
	private final HashMap<AsyncSocket, BRegister.Data> managers = new HashMap<>();
	private final AtomicInteger atomicBucketPortId = new AtomicInteger(10000);

	public Master() {

	}

	public void close() {
		for (var db : databases.values())
			db.close();
		databases.clear();
	}

	public int nextBucketPortId() {
		return atomicBucketPortId.incrementAndGet();
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
