package Zeze.Dbh2.Master;

import Zeze.Builtin.Dbh2.Master.CreateBucket;
import Zeze.Builtin.Dbh2.Master.CreateDatabase;
import Zeze.Builtin.Dbh2.Master.CreateTable;
import Zeze.Builtin.Dbh2.Master.GetBuckets;
import Zeze.Builtin.Dbh2.Master.Register;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.ProtocolHandle;
import Zeze.Transaction.Procedure;
import Zeze.Util.OutObject;

public class MasterAgent extends AbstractMasterAgent {
	public static final String eServiceName = "Zeze.Dbh2.MasterAgent";
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

	public void start() {
		try {
			service.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		;
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
	}

	public boolean createTable(String database, String table, OutObject<MasterTable.Data> out) {
		var r = new CreateTable();
		r.Argument.setDatabase(database);
		r.Argument.setTable(table);
		r.SendForWait(service.GetSocket()).await();
		out.value = r.Result;
		var rc = r.getResultCode();
		if (rc != 0 && rc != errorCode(eTableIsNew))
			throw new RuntimeException("fail module=" + IModule.getModuleId(rc) + " code=" + IModule.getErrorCode(rc));
		return IModule.getErrorCode(rc) == eTableIsNew;
	}

	public MasterTable.Data getBuckets(String database, String table) {
		var r = new GetBuckets();
		r.Argument.setDatabase(database);
		r.Argument.setTable(table);
		r.SendForWait(service.GetSocket()).await();
		return r.Result;
	}

	public void register(String dbh2RaftAcceptorName) {
		var r = new Register();
		r.Argument.setDbh2RaftAcceptorName(dbh2RaftAcceptorName);
		r.SendForWait(service.GetSocket()).await();
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
}
