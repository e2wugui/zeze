package Zeze.Dbh2;

import java.io.File;
import java.io.IOException;
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
import Zeze.Raft.ProxyServer;
import Zeze.Raft.RaftConfig;
import Zeze.Util.KV;
import Zeze.Util.PerfCounter;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
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
	private final ProxyServer proxyServer;

	static {
		var level = Level.toLevel(System.getProperty("logLevel"), Level.INFO);
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
	}

	private Future<?> loadMonitorTimer;
	final AtomicLong atomicSerialNo = new AtomicLong();
	private final Dbh2Config dbh2Config = new Dbh2Config();

	private final String home;

	private final ConcurrentHashMap<String, Dbh2> dbh2s = new ConcurrentHashMap<>();

	private final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	public MasterAgent getMasterAgent() {
		return masterAgent;
	}

	public Dbh2Config getDbh2Config() {
		return dbh2Config;
	}

	public TaskOneByOneByKey getTaskOneByOne() {
		return taskOneByOne;
	}

	void register(String acceptor, int port, int bucketCount) {
		masterAgent.register(acceptor, port, bucketCount);
	}

	protected long ProcessCreateBucketRequest(CreateBucket r) throws Exception {
		logger.info("CreateBucket: db={}, table={}, config={}",
				r.Argument.getDatabaseName(), r.Argument.getTableName(), r.Argument.getRaftConfig());
		var raftConfig = RaftConfig.loadFromString(r.Argument.getRaftConfig());
		var portId = Integer.parseInt(raftConfig.getName().split("_")[1]);
		var bucketDir = Path.of(
				home,
				r.Argument.getDatabaseName(),
				r.Argument.getTableName(),
				String.valueOf(portId));

		var nodeDirPart = raftConfig.getName().replace(':', '_');
		var dbHome = new File(bucketDir.toFile(), nodeDirPart);
		//noinspection ResultOfMethodCallIgnored
		dbHome.mkdirs();
		raftConfig.setDbHome(dbHome.toString());
		var file = new File(raftConfig.getDbHome(), "raft.xml");
		java.nio.file.Files.writeString(file.toPath(),
				r.Argument.getRaftConfig(),
				StandardOpenOption.CREATE);
		dbh2s.computeIfAbsent(r.Argument.getRaftConfig(), __ -> {
			var dbh2 = new Dbh2(this, raftConfig.getName(), raftConfig,
					null, false, taskOneByOne);
			proxyServer.addRaft(dbh2.getRaft());
			logger.info("CreateBucket: add raftName = '{}'", dbh2.getRaft().getName());
			return dbh2;
		});
		r.SendResult();
		masterAgent.reportBucketCount(dbh2s.size());
		return 0;
	}

	public class Service extends MasterAgent.Service {
		private final ProxyServer proxyServer;

		public Service(Config config) {
			super(config);
			proxyServer = null;
		}

		public Service(Config config, ProxyServer proxyServer) {
			super(config);
			this.proxyServer = proxyServer;
		}

		@Override
		public void OnHandshakeDone(AsyncSocket so) throws Exception {
			super.OnHandshakeDone(so);

			// 优先查找代理配置，
			KV<String, Integer> ipPort =
					null != proxyServer
					? proxyServer.getOneAcceptorAddress()
					: getOneAcceptorAddress();
			Dbh2Manager.this.register(ipPort.getKey(), ipPort.getValue(), Dbh2Manager.this.dbh2s.size());
		}
	}

	public Dbh2Manager(String home, String configXml) {
		this.home = home;
		var config = Config.load(configXml);
		config.parseCustomize(this.dbh2Config);
		proxyServer = new ProxyServer(config, dbh2Config.getRpcTimeout());
		masterAgent = new MasterAgent(config, this::ProcessCreateBucketRequest, new Service(config, proxyServer));
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
		logger.info("loading {} raftXmlFiles from '{}'", raftXmlFiles.size(), home);
		raftXmlFiles.parallelStream().forEach((raftXml) -> {
			try {
				var bytes = java.nio.file.Files.readAllBytes(raftXml.toPath());
				var raftStr = new String(bytes, StandardCharsets.UTF_8);
				var raftConfig = RaftConfig.loadFromString(raftStr);
				raftConfig.setDbHome(raftXml.getParent());
				dbh2s.computeIfAbsent(raftStr, __ -> {
					var dbh2 = new Dbh2(this, raftConfig.getName(), raftConfig,
							null, false, taskOneByOne);
					proxyServer.addRaft(dbh2.getRaft());
					logger.info("start: add raftName = '{}'", dbh2.getRaft().getName());
					return dbh2;
				});
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		});
		masterAgent.startAndWaitConnectionReady();
		proxyServer.start();

		loadMonitorTimer = Task.scheduleUnsafe(120_000, 120_000, this::loadMonitor);
	}

	private void loadMonitor() throws Exception {
		var loadManager = 0.0;
		var willSplit = new ArrayList<Dbh2>();
		Dbh2 maxLoadDbh2 = null;
		double maxLoad = 0.0f;
		var hasSplitting = false;
		for (var dbh2 : dbh2s.values()) {
			var load = dbh2.getStateMachine().load();
			loadManager += load;
			hasSplitting |= dbh2.getStateMachine().getBucket().getSplittingMeta() != null;

			// 达到分桶条件之一：负载高于最大值的80%。
			// 这里可以考虑dbFileSize(min,max)，当库比较大时也分桶，另外库很小时即使负载高也不分桶。
			if (load > dbh2.getDbh2Config().getSplitLoad())
				willSplit.add(dbh2);
			if (load > maxLoad) {
				maxLoad = load;
				maxLoadDbh2 = dbh2;
			}
		}
		logger.info("splitting try ... manager={} load={}", home, loadManager);
		masterAgent.reportLoad(loadManager);
		if (!willSplit.isEmpty()) {
			// 分桶优先处理
			for (var split : willSplit) {
				split.tryStartSplit(false); // 允许重复调用，里面需要去重。
			}
		} else if (!hasSplitting && null != maxLoadDbh2 && loadManager > maxLoadDbh2.getDbh2Config().getSplitMaxManagerLoad() * 0.6) {
			// 没有分桶，但是总负载达到，则把当前负载最大的桶迁移走。
			// （!hasSplitting）迁移桶仅在没有分桶并且没有迁移桶的时候才执行。
			maxLoadDbh2.tryStartSplit(true);
		}
	}

	public void stop() throws Exception {
		if (null != loadMonitorTimer)
			loadMonitorTimer.cancel(true);
		ShutdownHook.remove(this);
		proxyServer.stop();
		masterAgent.stop();
		for (var dbh2 : dbh2s.values())
			dbh2.close();
		dbh2s.clear();
	}

	public static void main(String[] args) {
		try {
			Task.tryInitThreadPool();

			var selector = 1;

			for (int i = 2; i < args.length; ++i) {
				//noinspection SwitchStatementWithTooFewBranches,EnhancedSwitchMigration
				switch (args[i]) {
				case "-selector":
					selector = Integer.parseInt(args[++i]);
					break;
				default:
					throw new RuntimeException("unknown option: " + args[i]);
				}
			}

			Zeze.Net.Selectors.getInstance().add(selector - 1);
			PerfCounter.instance.tryStartScheduledLog();

			var manager = new Dbh2Manager(args[0], args[1]);
			manager.start();
			synchronized (Thread.currentThread()) {
				Thread.currentThread().wait();
			}
		} catch (Exception e) {
			logger.error("", e);
		}
	}
}
