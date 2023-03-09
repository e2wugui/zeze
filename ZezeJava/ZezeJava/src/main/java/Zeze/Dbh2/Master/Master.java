package Zeze.Dbh2.Master;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.Master.CreateDatabase;
import Zeze.Builtin.Dbh2.Master.CreateTable;
import Zeze.Builtin.Dbh2.Master.GetBuckets;
import Zeze.Builtin.Dbh2.Master.LocateBucket;
import Zeze.Builtin.Dbh2.Master.Register;
import Zeze.Util.OutObject;

public class Master extends AbstractMaster {
	private final ConcurrentHashMap<String, MasterDatabase> databases = new ConcurrentHashMap<>();

	public Master() {

	}

	@Override
	protected long ProcessCreateDatabaseRequest(CreateDatabase r) throws Exception {
		databases.computeIfAbsent(r.Argument.getDatabase(), MasterDatabase::new);
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
		// todo 注册dbh2Manager
		return 0;
	}
}
