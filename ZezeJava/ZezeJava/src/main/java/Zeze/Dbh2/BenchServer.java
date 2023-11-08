package Zeze.Dbh2;

import java.util.ArrayList;
import Zeze.Config;
import Zeze.Util.Task;

public class BenchServer {
	private static Database newDatabase(Dbh2AgentManager dbh2AgentManager, @SuppressWarnings("SameParameterValue") String dbName) {
		var databaseConf = new Config.DatabaseConf();
		databaseConf.setDatabaseType(Config.DbType.Dbh2);
		databaseConf.setDatabaseUrl("dbh2://127.0.0.1:10999/" + dbName);
		databaseConf.setName("dbh2");
		return new Database(null, dbh2AgentManager, databaseConf);
	}

	public static void main(String [] args) throws Exception {
		Task.tryInitThreadPool();

		var master = new Zeze.Dbh2.Master.Main("zeze.xml");
		var managers = new ArrayList<Dbh2Manager>();
		try {
			master.start();
			for (int i = 0; i < 3; ++i)
				managers.add(new Zeze.Dbh2.Dbh2Manager("manager" + i, "zeze.xml"));
			for (var manager : managers)
				manager.start();

			BenchClient.main(args);
		} finally {
			master.stop();
			for (var manager : managers)
				manager.stop();
		}
	}
}
