package Zeze.Dbh2;

import Zeze.Application;
import Zeze.Config;

public class CommitServer {
	public static void main(String[] args) throws Exception {
//		var config = "zeze.xml";
//		for (int i = 0; i < args.length; ++i) {
//			//noinspection SwitchStatementWithTooFewBranches
//			switch (args[i]) {
//			case "conf":
//				config = args[++i];
//				break;
//			}
//		}

		var serviceManager = Application.createServiceManager(Config.load(), "Dbh2ServiceManager");
		assert serviceManager != null;
		serviceManager.start();
		serviceManager.waitReady();
		var dbh2AgentManager = new Dbh2AgentManager(serviceManager, null);
		dbh2AgentManager.start();
		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}
}
