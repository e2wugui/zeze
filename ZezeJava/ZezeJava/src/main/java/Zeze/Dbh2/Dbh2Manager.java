package Zeze.Dbh2;

import java.util.Objects;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.Dbh2.Master.BDbh2Config;
import Zeze.Builtin.Dbh2.Master.CreateBucket;
import Zeze.Builtin.Dbh2.Master.DestroyBucket;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Net.AsyncSocket;
import Zeze.Raft.ProxyServer;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.LogSequence;
import Zeze.Util.AtomicOutputFile;
import Zeze.Util.DaemonTimer;
import Zeze.Util.KVNotNull;
import Zeze.Util.RocksDatabase;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import Zeze.Util.TaskSpec;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.rocksdb.RocksDBException;
import org.jspecify.annotations.Nullable;
import static Zeze.Util.Args.requireInt;

/**
 * Dbh2管理器，管理Dbh2(Raft桶)的创建。
 * 一个管理器包含多个桶。
 */
public class Dbh2Manager {
	private static final Logger logger = LogManager.getLogger(Dbh2Manager.class);

	private final Service masterService;
	private final MasterAgent masterAgent;
	private final ProxyServer proxyServer;

	static {
		var level = Level.toLevel(System.getProperty("logLevel"), Level.INFO);
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
	}

	private final DaemonTimer loadMonitorTimer = new DaemonTimer("Dbh2Manager.loadMonitor", 120_000, this::loadMonitor);
	final AtomicLong atomicSerialNo = new AtomicLong();
	private final Dbh2Config dbh2Config = new Dbh2Config();

	private final String home;
	private final RocksDatabase database;

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

	private void createBucket(String databaseName, String tableName, String raftConfigStr) throws IOException {
		logger.info("CreateBucket: db={}, table={}, config={}",
				databaseName, tableName, raftConfigStr);
		var raftConfig = RaftConfig.loadFromString(raftConfigStr);
		//noinspection DataFlowIssue
		var portId = Integer.parseInt(raftConfig.getName().split("_")[1]);
		var bucketDir = Path.of(
				home,
				databaseName,
				tableName,
				String.valueOf(portId));

		var nodeDirPart = raftConfig.getName().replace(':', '_');
		var dbHome = new File(bucketDir.toFile(), nodeDirPart);
		//noinspection ResultOfMethodCallIgnored
		dbHome.mkdirs();
		raftConfig.setDbHome(dbHome.toString());
		var file = new File(raftConfig.getDbHome(), "raft.xml");
		AtomicOutputFile.writeAtomically(file.toPath(), raftConfigStr.getBytes(StandardCharsets.UTF_8));
		dbh2s.computeIfAbsent(raftConfig.getSortedNames(), __ -> {
			var dbh2 = new Dbh2(this, Objects.requireNonNull(raftConfig.getName()),
					database, raftConfig,
					null, taskOneByOne);
			proxyServer.addRaft(dbh2.getRaft());
			logger.info("CreateBucket: add raftName = '{}'", dbh2.getRaft().getName());
			return dbh2;
		});
	}

	protected long ProcessCreateBucketRequest(CreateBucket r) throws Exception {
		createBucket(r.Argument.getDatabaseName(), r.Argument.getTableName(), r.Argument.getRaftConfig());
		r.SendResult();
		masterAgent.reportBucketCount(dbh2s.size());
		return 0;
	}

	// 幂等销毁桶对应的 Raft 实例并清理桶目录
	void destroyBucket(String databaseName, String tableName, String raftConfigStr) throws IOException {
		var raftConfig = RaftConfig.loadFromString(raftConfigStr);
		//noinspection DataFlowIssue
		var portId = Integer.parseInt(raftConfig.getName().split("_")[1]);
		var bucketDir = Path.of(home, databaseName, tableName, String.valueOf(portId));
		logger.info("DestroyBucket: db={}, table={}, raftName='{}'", databaseName, tableName, raftConfig.getName());
		var dbh2 = dbh2s.remove(raftConfig.getSortedNames());
		if (null != dbh2) {
			proxyServer.removeRaft(dbh2.getRaft());
			// close 失败时不删目录，避免破坏仍打开的 RocksDB
			dbh2.close();
		} else
			logger.info("DestroyBucket: raft not found (idempotent). raftName='{}'", raftConfig.getName());
		// 确认目录已删，避免残留 raft.xml 在重启时被重新加载
		LogSequence.deletedDirectoryAndCheck(bucketDir.toFile());
	}

	protected long ProcessDestroyBucketRequest(DestroyBucket r) throws Exception {
		destroyBucket(r.Argument.getDatabaseName(), r.Argument.getTableName(), r.Argument.getRaftConfig());
		r.SendResult();
		masterAgent.reportBucketCount(dbh2s.size());
		return 0;
	}

	public static class Service extends MasterAgent.Service {
		private final @Nullable ProxyServer proxyServer;
		private volatile @Nullable Dbh2Manager manager;

		public Service(Config config) {
			super(config);
			proxyServer = null;
		}

		public Service(Config config, ProxyServer proxyServer) {
			super(config);
			this.proxyServer = proxyServer;
		}

		public void setManager(Dbh2Manager manager) {
			this.manager = manager;
		}

		@Override
		protected void OnMasterConnected(AsyncSocket so) {
			var m = manager;
			if (null == m)
				return;
			// IO 线程回调，异步提交重注册避免阻塞
			TaskSpec.ofAction(m::reRegister).name("Dbh2Manager.reRegister").submitNow();
		}

		public KVNotNull<String, Integer> getAcceptorAddress() {
			// 优先查找代理配置，
			return null != proxyServer
					? proxyServer.getOneAcceptorAddress()
					: getOneAcceptorAddress();
		}
	}

	public Dbh2Manager(String home, String configXml) throws RocksDBException {
		this.home = home;
		var config = Config.load(configXml);
		config.parseCustomize(this.dbh2Config);
		proxyServer = new ProxyServer(config, dbh2Config.getRpcTimeout());
		masterService = new Service(config, proxyServer);
		masterService.setManager(this);
		masterAgent = new MasterAgent(config, this::ProcessCreateBucketRequest, this::ProcessDestroyBucketRequest, masterService);
		database = new RocksDatabase(Paths.get(home, "db").toString());
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
				dbh2s.computeIfAbsent(raftConfig.getSortedNames(), __ -> {
					var dbh2 = new Dbh2(this, Objects.requireNonNull(raftConfig.getName()),
							database, raftConfig,
							null, taskOneByOne);
					proxyServer.addRaft(dbh2.getRaft());
					logger.info("start: add raftName = '{}'", dbh2.getRaft().getName());
					return dbh2;
				});
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		});
		masterAgent.startAndWaitConnectionReady();
		registerToMaster();
		proxyServer.start();

		loadMonitorTimer.start();
	}

	private volatile boolean masterRegisterReady = false;

	// 向 Master 注册、补建缺失桶并上报就绪；synchronized 串行化启动与重连并发，避免同时落盘 raft.xml
	private synchronized void registerToMaster() throws Exception {
		masterRegisterReady = false;
		var acceptorAddress = masterService.getAcceptorAddress();
		var dbh2sAtMaster = masterAgent.register(acceptorAddress.getKey(), acceptorAddress.getValue(), dbh2s.size());
		logger.info("{}, {} - rafts=\n{}\n{}", acceptorAddress.getKey(), acceptorAddress.getValue(), dbh2sAtMaster, dbh2s.keySet());
		var dbh2sAtMasterMiss = new HashMap<String, BDbh2Config.Data>();
		for (var dbh2 : dbh2sAtMaster.getDbh2Configs()) {
			var sortedNames = RaftConfig.loadFromString(dbh2.getRaftConfig()).getSortedNames();
			if (!dbh2s.containsKey(sortedNames))
				dbh2sAtMasterMiss.put(dbh2.getRaftConfig(), dbh2);
		}
		// 补建本地缺失的桶（如节点重建恢复）
		logger.info("miss rafts={}", dbh2sAtMasterMiss.values());
		for (var dbh2 : dbh2sAtMasterMiss.values()) {
			createBucket(dbh2.getDatabase(), dbh2.getTable(), dbh2.getRaftConfig());
		}
		masterAgent.setDbh2Ready();
		masterRegisterReady = true;
	}

	void reRegister() {
		try {
			registerToMaster();
		} catch (Exception e) {
			logger.error("re-register to master failed, wait for next reconnect or loadMonitor retry", e);
		}
	}

	private void loadMonitor() throws Exception {
		// 若上次注册未完成就绪，周期补发重注册
		if (!masterRegisterReady) {
			logger.warn("loadMonitor: master registration incomplete (register or setDbh2Ready failed), re-registering");
			reRegister();
			if (!masterRegisterReady)
				return;
		}
		var loadManager = 0.0;
		var willSplit = new ArrayList<Dbh2>();
		Dbh2 maxLoadDbh2 = null;
		double maxLoad = 0.0f;
		var hasSplitting = false;
		for (var dbh2 : dbh2s.values()) {
			// 快照恢复期间 bucket 暂为 null，跳过本轮统计
			var bucket = dbh2.getStateMachine().getBucket();
			if (null == bucket)
				continue;
			var load = dbh2.getStateMachine().load();
			loadManager += load;
			hasSplitting |= bucket.getSplittingMeta() != null;

			// 达到分桶条件之一：负载高于最大值的80%。
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
		loadMonitorTimer.stop();
		ShutdownHook.remove(this);
		proxyServer.stop();
		masterAgent.stop();
		// 逐桶捕获异常，避免单个桶关闭失败导致后续桶与底层库泄漏
		for (var dbh2 : dbh2s.values()) {
			try {
				dbh2.close();
			} catch (Exception e) {
				logger.error("stop close dbh2 fail. raft={}", dbh2.getRaft().getName(), e);
			}
		}
		dbh2s.clear();
		database.close();
	}

	public static void main(String[] args) {
		try {
			Task.tryInitThreadPool();

			var selector = 1;

			for (int i = 2; i < args.length; ++i) {
				//noinspection SwitchStatementWithTooFewBranches,EnhancedSwitchMigration
				switch (args[i]) {
				case "-selector":
					selector = requireInt(args, ++i, "-selector");
					break;
				default:
					throw new RuntimeException("unknown option: " + args[i]);
				}
			}

			Zeze.Net.Selectors.getInstance().add(selector - 1);
			ZezeCounter.tryInit();

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
