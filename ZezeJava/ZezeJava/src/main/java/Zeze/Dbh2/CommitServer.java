package Zeze.Dbh2;

import Zeze.Config;
import Zeze.Util.ShutdownHook;

public class CommitServer {
	public CommitServer() {
	}

	public void start(Config config) throws Exception {
		Dbh2AgentManager.getInstance().start(config);
		ShutdownHook.add(this, this::stop);
	}

	public void stop() throws Exception {
		Dbh2AgentManager.getInstance().stop();
	}

	public static void main(String [] args) throws Exception {
		var server = new CommitServer();
		var config = Config.load();
		server.start(config);
		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}
}
