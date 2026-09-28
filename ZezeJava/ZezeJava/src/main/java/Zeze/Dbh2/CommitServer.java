package Zeze.Dbh2;

import Zeze.Application;
import Zeze.Config;

/**
 * Dbh2 Commit 服务进程入口。
 */
public class CommitServer {
	public static void main(String[] args) throws Exception {
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
