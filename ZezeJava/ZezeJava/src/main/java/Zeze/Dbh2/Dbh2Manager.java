package Zeze.Dbh2;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.Dbh2.Master.CreateBucket;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Net.AsyncSocket;
import Zeze.Raft.RaftConfig;
import Zeze.Util.OutObject;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

/**
 * Dbh2管理器，管理Dbh2(Raft桶)的创建。
 * 一个管理器包含多个桶。
 */
public class Dbh2Manager {
	private static final Logger logger = LogManager.getLogger(Dbh2Manager.class);

	private final MasterAgent masterAgent;
	static {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
		var level = Level.toLevel(System.getProperty("logLevel"), Level.INFO);
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
	}

	// 性能统计。
	public final AtomicLong counterGet = new AtomicLong();
	public final AtomicLong counterPut = new AtomicLong();
	public final AtomicLong counterDelete = new AtomicLong();
	public final AtomicLong counterBeginTransaction = new AtomicLong();
	public final AtomicLong counterCommitTransaction = new AtomicLong();
	public final AtomicLong counterRollbackTransaction = new AtomicLong();
	private Future<?> reportTimer;

	private final String home;

	private final ConcurrentHashMap<String, Dbh2> dbh2s = new ConcurrentHashMap<>();

	void register(String acceptor) {
		masterAgent.register(acceptor);
	}

	protected long ProcessCreateBucketRequest(CreateBucket r) throws Exception {
		var raftConfig = RaftConfig.loadFromString(r.Argument.getRaftConfig());
		var portId = Integer.parseInt(raftConfig.getName().split(":")[1]);
		var bucketDir = Path.of(
				home,
				r.Argument.getDatabaseName(),
				r.Argument.getTableName(),
				String.valueOf(portId));

		var nodeDirPart = raftConfig.getName().replace(":", "_");
		var dbHome = new File(bucketDir.toFile(), nodeDirPart);
		//noinspection ResultOfMethodCallIgnored
		dbHome.mkdirs();
		raftConfig.setDbHome(dbHome.toString());
		var file = new File(raftConfig.getDbHome(), "raft.xml");
		java.nio.file.Files.writeString(file.toPath(),
				r.Argument.getRaftConfig(),
				StandardOpenOption.CREATE);
		dbh2s.computeIfAbsent(r.Argument.getRaftConfig(),
				(key) -> new Dbh2(this, raftConfig.getName(), raftConfig, null, false));
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

	public Dbh2Manager(String home, String configXml) {
		this.home = home;
		var config = Config.load(configXml);
		masterAgent = new MasterAgent(config, this::ProcessCreateBucketRequest, new Service(config));
	}

	private static void listRaftXmlFiles(File dir, ArrayList<File> out) {
		var listFile = dir.listFiles();
		if (null == listFile)
			return;

		for (var file : listFile) {
			if (file.isDirectory())
				listRaftXmlFiles(file, out);
			else if (file.isFile() && file.getName().equals("raft.xml"))
				out.add(file);
		}
	}

	public void start() throws Exception {
		ShutdownHook.add(this, this::stop);
		var raftXmlFiles = new ArrayList<File>();
		listRaftXmlFiles(new File(home), raftXmlFiles);
		for (var raftXml : raftXmlFiles) {
			var bytes = java.nio.file.Files.readAllBytes(raftXml.toPath());
			var raftStr = new String(bytes, StandardCharsets.UTF_8);
			var raftConfig = RaftConfig.loadFromString(raftStr);
			raftConfig.setDbHome(raftXml.getParent()); // todo 这个路径对吗？ 需要调试一下。
			dbh2s.computeIfAbsent(raftStr,
					(key) -> new Dbh2(this, raftConfig.getName(), raftConfig, null, false));
		}
		masterAgent.startAndWaitConnectionReady();
		reportTimer = Task.scheduleUnsafe(2000, 2000, this::reportLoad);
	}


	public void stop() throws Exception {
		ShutdownHook.remove(this);
		if (null != reportTimer)
			reportTimer.cancel(true);
		masterAgent.stop();
		for (var dbh2 : dbh2s.values())
			dbh2.close();
		dbh2s.clear();
	}

	private long lastGet;
	private long lastPut;
	private long lastDelete;
	private long lastBeginTransaction;
	private long lastCommitTransaction;
	private long lastRollbackTransaction;
	private long lastReportTime = System.currentTimeMillis();

	private void reportLoad() {
		var now = System.currentTimeMillis();
		var elapse = (now - lastReportTime) / 1000.0f;
		lastReportTime = now;

		var nowGet = counterGet.get();
		var nowPut = counterPut.get();
		var nowDelete = counterDelete.get();
		var nowBeginTransaction = counterBeginTransaction.get();
		var nowCommitTransaction = counterCommitTransaction.get();
		var nowRollbackTransaction = counterRollbackTransaction.get();

		var diffGet = nowGet - lastGet;
		var diffPut = nowPut - lastPut;
		var diffDelete = nowDelete - lastDelete;
		var diffBeginTransaction = nowBeginTransaction - lastBeginTransaction;
		var diffCommitTransaction = nowCommitTransaction - lastCommitTransaction;
		var diffRollbackTransaction = nowRollbackTransaction - lastRollbackTransaction;

		if (diffGet > 0 || diffPut > 0 || diffDelete > 0
			|| diffBeginTransaction > 0 || diffCommitTransaction > 0 || diffRollbackTransaction > 0) {
			lastGet = nowGet;
			lastPut = nowPut;
			lastDelete = nowDelete;
			lastBeginTransaction = nowBeginTransaction;
			lastCommitTransaction = nowCommitTransaction;
			lastRollbackTransaction = nowRollbackTransaction;

			var sb = new StringBuilder();
			sb.append(" get=").append(diffGet / elapse);
			sb.append(" put=").append(diffPut / elapse);
			sb.append(" delete=").append(diffDelete / elapse);
			sb.append(" begin=").append(diffBeginTransaction / elapse);
			sb.append(" commit=").append(diffCommitTransaction / elapse);
			sb.append(" rollback=").append(diffRollbackTransaction / elapse);

			logger.info(sb.toString());
		}
	}

	public static void main(String [] args) throws Exception {
		Task.tryInitThreadPool(null, null, null);
		Zeze.Net.Selectors.getInstance().add(Runtime.getRuntime().availableProcessors() - 1);

		var manager = new Dbh2Manager(args[0], args[1]);
		manager.start();
		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}
}
