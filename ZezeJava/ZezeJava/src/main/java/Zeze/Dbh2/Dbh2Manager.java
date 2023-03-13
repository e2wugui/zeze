package Zeze.Dbh2;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.Master.CreateBucket;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Net.AsyncSocket;
import Zeze.Raft.RaftConfig;
import Zeze.Util.OutObject;
import Zeze.Util.ShutdownHook;
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

	private final ConcurrentHashMap<String, Dbh2> dbh2s = new ConcurrentHashMap<>();

	void register(String acceptor) {
		masterAgent.register(acceptor);
	}

	protected long ProcessCreateBucketRequest(CreateBucket r) throws Exception {
		var raftConfig = RaftConfig.loadFromString(r.Argument.getRaftConfig());
		var portId = Integer.parseInt(raftConfig.getName().split(":")[1]);
		var bucketDir = r.Argument.getDatabaseName() + "@" + r.Argument.getTableName() + "@" + portId;
		var nodeDirPart = raftConfig.getName().replace(":", "_");
		raftConfig.setDbHome(new File(bucketDir, nodeDirPart).toString());
		new File(raftConfig.getDbHome()).mkdirs();
		var file = new File(raftConfig.getDbHome(), "raft.xml");
		java.nio.file.Files.write(file.toPath(),
				r.Argument.getRaftConfig().getBytes(StandardCharsets.UTF_8),
				StandardOpenOption.CREATE_NEW);
		dbh2s.computeIfAbsent(r.Argument.getRaftConfig(),
				(key) -> new Dbh2(raftConfig.getName(), raftConfig, null, false));
		r.SendResult();
		return 0;
	}

	public class Service extends MasterAgent.Service {
		public Service(Config config) {
			super(config);
		}

		private String getOneAcceptorIp() {
			var outIp = new OutObject<String>(null);
			getConfig().forEachAcceptor2((a) -> { outIp.value = a.getIp(); return false; } );
			if (null == outIp.value)
				throw new RuntimeException("acceptor not found");
			return outIp.value;
		}

		@Override
		public void OnHandshakeDone(AsyncSocket so) throws Exception {
			super.OnHandshakeDone(so);
			Dbh2Manager.this.register(getOneAcceptorIp());
		}

	}
	public Dbh2Manager() {
		var config = Config.load();
		masterAgent = new MasterAgent(config, this::ProcessCreateBucketRequest, new Service(config));
	}

	private static void listRaftXmlFiles(File dir, ArrayList<File> out) {
		for (var file : dir.listFiles()) {
			if (file.isDirectory())
				listRaftXmlFiles(file, out);
			else if (file.isFile() && file.getName().equals("raft.xml"))
				out.add(file);
		}
	}

	public void start() throws Exception {
		ShutdownHook.add(this, this::stop);
		var raftXmlFiles = new ArrayList<File>();
		listRaftXmlFiles(new File("."), raftXmlFiles);
		for (var raftXml : raftXmlFiles) {
			var bytes = java.nio.file.Files.readAllBytes(raftXml.toPath());
			var raftStr = new String(bytes, StandardCharsets.UTF_8);
			var raftConfig = RaftConfig.loadFromString(raftStr);
			raftConfig.setDbHome(raftXml.getParent()); // todo 这个路径对吗？ 需要调试一下。
			dbh2s.computeIfAbsent(raftStr,
					(key) -> new Dbh2(raftConfig.getName(), raftConfig, null, false));
		}
		masterAgent.startAndWaitConnectionReady();
	}

	public void stop() throws Exception {
		masterAgent.stop();
		for (var dbh2 : dbh2s.values())
			dbh2.close();
		dbh2s.clear();
	}

	public static void main(String [] args) throws Exception {
		var manager = new Dbh2Manager();
		manager.start();
		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}
}
