package Zeze.Dbh2.Master;

import Zeze.Builtin.Dbh2.Master.CreateDatabase;
import Zeze.Builtin.Dbh2.Master.CreateTable;
import Zeze.Builtin.Dbh2.Master.GetBuckets;
import Zeze.Config;

public class MasterAgent extends AbstractMasterAgent {
	private final Service service;

	public MasterAgent(Config config) {
		service = new Service(config);
		RegisterProtocols(service);
	}

	public void start() throws Exception {
		service.start();;
	}

	public void stop() throws Exception {
		service.stop();
	}

	public void createDatabase(String database) {
		var r = new CreateDatabase();
		r.Argument.setDatabase(database);
		r.SendForWait(service.GetSocket()).await();
	}

	public MasterTableData createTable(String database, String table) {
		var r = new CreateTable();
		r.Argument.setDatabase(database);
		r.Argument.setTable(table);
		r.SendForWait(service.GetSocket()).await();
		return r.Result;
	}

	public MasterTableData getBuckets(String database, String table) {
		var r = new GetBuckets();
		r.Argument.setDatabase(database);
		r.Argument.setTable(table);
		r.SendForWait(service.GetSocket()).await();
		return r.Result;
	}

	public static class Service extends Zeze.Net.Service {
		public Service(Config config) {
			super("Zeze.Dbh2.MasterAgent", config);
		}
	}
}
