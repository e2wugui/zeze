package Zeze.Dbh2;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.Master.CreateBucket;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Net.AsyncSocket;
import Zeze.Raft.RaftConfig;
import Zeze.Util.BitConverter;
import Zeze.Util.OutObject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.tikv.shade.com.google.common.io.Files;

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
		var first = r.Argument.getKeyFirst();
		var path = Path.of(r.Argument.getDatabaseName(), r.Argument.getTableName(),
				BitConverter.toString(first.bytesUnsafe(), first.getOffset(), first.size()));
		var dir = path.toFile();
		dir.mkdirs(); // ignore result
		var file = new File(dir, "raft.xml");
		Files.write(r.Argument.getRaftConfig().getBytes(StandardCharsets.UTF_8), file);
		var raftConfig = RaftConfig.loadFromString(r.Argument.getRaftConfig());
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
		var raftXmlFiles = new ArrayList<File>();
		listRaftXmlFiles(new File("."), raftXmlFiles);
		for (var raftXml : raftXmlFiles) {
			var raftStr = "";
			var raftConfig = RaftConfig.loadFromString(raftStr);
			dbh2s.computeIfAbsent(raftStr,
					(key) -> new Dbh2(raftConfig.getName(), raftConfig, null, false));
		}
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
