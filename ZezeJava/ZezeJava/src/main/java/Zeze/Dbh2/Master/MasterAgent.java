package Zeze.Dbh2.Master;

import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.BRegisterResult;
import Zeze.Builtin.Dbh2.Master.CheckFreeManager;
import Zeze.Builtin.Dbh2.Master.CreateBucket;
import Zeze.Builtin.Dbh2.Master.CreateDatabase;
import Zeze.Builtin.Dbh2.Master.CreateSplitBucket;
import Zeze.Builtin.Dbh2.Master.CreateTable;
import Zeze.Builtin.Dbh2.Master.EndMove;
import Zeze.Builtin.Dbh2.Master.EndSplit;
import Zeze.Builtin.Dbh2.Master.GetBuckets;
import Zeze.Builtin.Dbh2.Master.Register;
import Zeze.Builtin.Dbh2.Master.ReportBucketCount;
import Zeze.Builtin.Dbh2.Master.ReportLoad;
import Zeze.Builtin.Dbh2.Master.SetDbh2Ready;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.Connector;
import Zeze.Net.ProtocolHandle;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action3;
import Zeze.Util.OutObject;
import Zeze.Util.Task;

public class MasterAgent extends AbstractMasterAgent {
	public static final String eServiceName = "Zeze.Dbh2.Master.Agent";
	private final Service service;
	private ProtocolHandle<CreateBucket> createBucketHandle;

	public MasterAgent(Config config) {
		service = new Service(config);
		RegisterProtocols(service);
	}

	public MasterAgent(Config config, ProtocolHandle<CreateBucket> handle, Service service) {
		this.service = service;
		this.createBucketHandle = handle;
		RegisterProtocols(this.service);
	}

	public Service getService() {
		return service;
	}

	public void startAndWaitConnectionReady() {
		try {
			service.start();
			service.getConfig().forEachConnector(Connector::WaitReady);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void stop() {
		try {
			service.stop();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void createDatabase(String database) {
		var r = new CreateDatabase();
		r.Argument.setDatabase(database);
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("createDatabase error=" + IModule.getErrorCode(r.getResultCode()));
	}

	public boolean createTable(String database, String table, OutObject<MasterTable.Data> out) {
		var r = new CreateTable();
		r.Argument.setDatabase(database);
		r.Argument.setTable(table);
		r.SendForWait(service.GetSocket(), 30_000).await();
		out.value = r.Result;
		var rc = r.getResultCode();
		if (rc != 0 && rc != errorCode(eTableIsNew))
			throw new RuntimeException("fail module=" + IModule.getModuleId(rc) + " code=" + IModule.getErrorCode(rc));
		return IModule.getErrorCode(rc) == eTableIsNew;
	}

	public void createTableAsync(String database, String table, Action3<Integer, Boolean, MasterTable.Data> callback) {
		var r = new CreateTable();
		r.Argument.setDatabase(database);
		r.Argument.setTable(table);
		r.Send(service.GetSocket(), (p) -> {
			var rc = r.getResultCode();
			if (rc == 0) {
				callback.run(0, false, r.Result);
			} else {
				var error = IModule.getErrorCode(rc);
				if (error == eTableIsNew)
					callback.run(0, true, r.Result);
				else
					callback.run(error, false, null);
			}
			return 0;
		}, 60_000);
	}

	public MasterTable.Data getBuckets(String database, String table) {
		var r = new GetBuckets();
		r.Argument.setDatabase(database);
		r.Argument.setTable(table);
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("getBuckets error=" + IModule.getErrorCode(r.getResultCode()));
		return r.Result;
	}

	public BRegisterResult.Data register(String dbh2RaftAcceptorName, int port, int bucketCount) {
		var r = new Register();
		r.Argument.setDbh2RaftAcceptorName(dbh2RaftAcceptorName);
		r.Argument.setPort(port);
		r.Argument.setBucketCount(bucketCount);
		r.SendForWait(service.GetSocket()).await(); // 这里不能等待，现在直接在网络线程中运行。
		if (r.getResultCode() != 0)
			throw new RuntimeException("register error=" + IModule.getErrorCode(r.getResultCode()));
		return r.Result;
	}

	public void setDbh2Ready() {
		var r = new SetDbh2Ready();
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("setDbh2Ready error=" + IModule.getErrorCode(r.getResultCode()));
	}

	@Override
	protected long ProcessCreateBucketRequest(CreateBucket r) throws Exception {
		if (null == createBucketHandle)
			return Procedure.NotImplement;
		return createBucketHandle.handle(r);
	}

	public static class Service extends Zeze.Net.Service {
		public Service(Config config) {
			super(eServiceName, config);
		}
	}

	public void reportLoad(double load) {
		var r = new ReportLoad();
		r.Argument.setLoad(load);
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("reportLoad error=" + IModule.getErrorCode(r.getResultCode()));
	}

	public BBucketMeta.Data createSplitBucket(BBucketMeta.Data bucket) {
		var r = new CreateSplitBucket();
		r.Argument = bucket;
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("error=" + IModule.getErrorCode(r.getResultCode()));
		return r.Result;
	}

	public void reportBucketCount(int count) {
		var r = new ReportBucketCount();
		r.Argument.setCount(count);
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("error=" + IModule.getErrorCode(r.getResultCode()));
	}

	public void endMoveWithRetryAsync(BBucketMeta.Data to) {
		var r = new EndMove();
		r.Argument.setTo(to);
		if (!r.Send(service.GetSocket(), (p) -> {
			if (p.getResultCode() != 0) {
				Task.schedule(30_000, () -> endMoveWithRetryAsync(to));
			}
			return 0;
		})) {
			Task.schedule(30_000, () -> endMoveWithRetryAsync(to));
		}
	}

	public void endSplitWithRetryAsync(BBucketMeta.Data from, BBucketMeta.Data to) {
		var r = new EndSplit();
		r.Argument.setFrom(from);
		r.Argument.setTo(to);
		if (!r.Send(service.GetSocket(), (p) -> {
			if (p.getResultCode() != 0) {
				Task.schedule(30_000, () -> endSplitWithRetryAsync(from, to));
			}
			return 0;
		})) {
			Task.schedule(30_000, () -> endSplitWithRetryAsync(from, to));
		}
	}

	public int checkFreeManager() {
		var r = new CheckFreeManager();
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("error=" + IModule.getErrorCode(r.getResultCode()));
		return r.Result.getCount();
	}
}
