package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.Master.CreateBucket;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Net.AsyncSocket;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

/**
 * Dbh2管理器，管理Dbh2(Raft桶)的创建。
 * 一个管理器包含多个桶。
 */
public class Dbh2Manager {
	private final MasterAgent masterAgent;
	static {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
		var level = Level.toLevel(System.getProperty("logLevel"), Level.INFO);
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
	}

	void register() {
		masterAgent.register(""); // todo get name
	}

	protected long ProcessCreateBucketRequest(CreateBucket r) throws Exception {
		return 0; // todo create bucket
	}

	public class Service extends MasterAgent.Service {
		public Service(Config config) {
			super(config);
		}

		@Override
		public void OnHandshakeDone(AsyncSocket so) throws Exception {
			super.OnHandshakeDone(so);
			Dbh2Manager.this.register();
		}

	}
	public Dbh2Manager() {
		var config = Config.load();
		masterAgent = new MasterAgent(config, this::ProcessCreateBucketRequest, new Service(config));
	}

	public void start() throws Exception {
		masterAgent.start();
	}

	public void stop() throws Exception {
		masterAgent.stop();
	}

	public static void main(String [] args) throws Exception {
		var manager = new Dbh2Manager();
		manager.start();
		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}
}
