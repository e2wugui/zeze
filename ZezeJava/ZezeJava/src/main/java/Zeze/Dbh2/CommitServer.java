package Zeze.Dbh2;

import Zeze.Config;
import Zeze.Util.ShutdownHook;

public class CommitServer {
	public CommitServer() {
	}

	public void start(Config config) throws Exception {
		Dbh2AgentManager.getInstance().locateBucket(config);
		ShutdownHook.add(this, this::stop);
	}

	public void stop() throws Exception {
		Dbh2AgentManager.getInstance().stop();
	}

	public static void main(String [] args) throws Exception {
		var config = "zeze.xml";
		for (int i = 0; i < args.length; ++i) {
			//noinspection SwitchStatementWithTooFewBranches
			switch (args[i]) {
			case "conf": config = args[++i]; break;
			}
		}
		var server = new CommitServer();
		server.start(Config.load(config));
		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}
}
