package Zeze.Dbh2;

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
import Zeze.Util.AtomicFileWriter;
import Zeze.Util.DaemonTimer;
import Zeze.Util.KV;
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
import org.jetbrains.annotations.NotNull;
import org.rocksdb.RocksDBException;
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

	// 周期守护：body(reportLoad阻塞RPC/tryStartSplit)进worker池，不占调度线程；stop有界等待在飞一轮
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
		// 原子落盘：Files.writeString默认截断，崩溃留半截xml。
		AtomicFileWriter.writeAtomically(file.toPath(), raftConfigStr.getBytes(StandardCharsets.UTF_8));
		dbh2s.computeIfAbsent(raftConfig.getSortedNames(), __ -> {
			var dbh2 = new Dbh2(this, raftConfig.getName(),
					database, raftConfig,
					null, false, taskOneByOne);
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

	// 销毁本manager为该桶建的raft（DestroyBucket，建桶半失败回滚协议）：先摘proxyServer派发与
	// dbh2s账目，再关raft（释放bucket端口），最后删桶目录——raft.xml残留会被start()的目录扫描
	// 复活。幂等：raft不存在（从未建或已销毁）也删目录并成功返回。包内可见供同包测试直驱。
	void destroyBucket(String databaseName, String tableName, String raftConfigStr) throws IOException {
		var raftConfig = RaftConfig.loadFromString(raftConfigStr);
		var portId = Integer.parseInt(raftConfig.getName().split("_")[1]);
		var bucketDir = Path.of(home, databaseName, tableName, String.valueOf(portId));
		logger.info("DestroyBucket: db={}, table={}, raftName='{}'", databaseName, tableName, raftConfig.getName());
		var dbh2 = dbh2s.remove(raftConfig.getSortedNames());
		if (null != dbh2) {
			proxyServer.removeRaft(dbh2.getRaft());
			// close失败（RuntimeException）不删目录：对打开的rocksdb删目录会留坏库，
			// 留着等下次销毁重试或人工核查。
			dbh2.close();
		} else
			logger.info("DestroyBucket: raft not found (idempotent). raftName='{}'", raftConfig.getName());
		// 失败重试后仍存在即抛（目录不存在时直通）：raft.xml残留会被start()扫描复活成幽灵raft。
		LogSequence.deletedDirectoryAndCheck(bucketDir.toFile());
	}

	protected long ProcessDestroyBucketRequest(DestroyBucket r) throws Exception {
		destroyBucket(r.Argument.getDatabaseName(), r.Argument.getTableName(), r.Argument.getRaftConfig());
		r.SendResult();
		masterAgent.reportBucketCount(dbh2s.size());
		return 0;
	}

	public static class Service extends MasterAgent.Service {
		private final ProxyServer proxyServer;
		private volatile Dbh2Manager manager;

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
		protected void OnMasterConnected(@NotNull AsyncSocket so) {
			var m = manager;
			if (null == m)
				return;
			// IO线程回调，不得同步等待rpc（register/setDbh2Ready为阻塞rpc），提交任务池异步重注册。
			TaskSpec.ofAction(m::reRegister).name("Dbh2Manager.reRegister").submitNow();
		}

		public KV<String, Integer> getAcceptorAddress() {
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
					var dbh2 = new Dbh2(this, raftConfig.getName(),
							database, raftConfig,
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
		registerToMaster();
		proxyServer.start();

		loadMonitorTimer.start();
	}

	// 注册三步（register→补齐缺失raft→setDbh2Ready）的单点：start()首注册与reRegister共用。
	// setDbh2Ready不可省略：Master侧managers为纯内存，重启后重建条目的ready=false，
	// 不重发则choiceManagers的shadowReadyManager恒空——建表/分桶静默瘫痪（比
	// eManagerNotFound更隐蔽的失败形态）。补齐缺失raft按master持久化主表对账本manager，
	// createBucket幂等（dbh2s.computeIfAbsent），重复执行无副作用。
	private volatile boolean masterRegisterReady = false;
	private void registerToMaster() throws Exception {
		// Register replaces the master's manager entry with ready=false on every retry.
		// Only this registration's successful SetDbh2Ready may restore local readiness.
		masterRegisterReady = false;
		var acceptorAddress = masterService.getAcceptorAddress();
		var dbh2sAtMaster = masterAgent.register(acceptorAddress.getKey(), acceptorAddress.getValue(), dbh2s.size());
		logger.info("{}, {} - rafts=\n{}\n{}", acceptorAddress.getKey(), acceptorAddress.getValue(), dbh2sAtMaster, dbh2s.keySet());
		var dbh2sAtMasterMiss = new HashMap<String, BDbh2Config.Data>();
		for (var dbh2 : dbh2sAtMaster.getDbh2Configs()) {
			if (!dbh2s.containsKey(dbh2.getRaftConfig()))
				dbh2sAtMasterMiss.put(dbh2.getRaftConfig(), dbh2);
		}
		// 保存并启动丢失的dbh2（一般是系统完全毁坏，重新找的新机器） ...
		logger.info("miss rafts={}", dbh2sAtMasterMiss.values());
		for (var dbh2 : dbh2sAtMasterMiss.values()) {
			createBucket(dbh2.getDatabase(), dbh2.getTable(), dbh2.getRaftConfig());
		}
		masterAgent.setDbh2Ready();
		masterRegisterReady = true;
	}

	// Master重启丢失managers注册表后由连接建立钩子（OnMasterConnected）重发注册恢复；
	// 失败仅记日志，等下次重连再试（Connector autoReconnect持续重连）。首连时与start()的
	// registerToMaster各发一次——重复注册幂等推演：Master侧ProcessRegisterRequest按socket
	// 与acceptor:port身份先摘同身份旧条目再入列（master锁内串行），任意次重发终态单条目；
	// tryRemoveManager按socket摘除亦不多摘。
	void reRegister() {
		try {
			registerToMaster();
		} catch (Exception e) {
			logger.error("re-register to master failed, wait for next reconnect or loadMonitor retry", e);
		}
	}

	private void loadMonitor() throws Exception {
		// R1（FND27审视残余）：register成功而setDbh2Ready rpc失败（拥塞超时、连接未断）时，
		// master侧条目停留ready=false且无事件再触发——OnMasterConnected只在（重）连接事件触发，
		// 连接健在则永不重发：shadowReadyManager恒空、建表/分桶静默瘫痪正是本类要消灭的形态。
		// 周期补触发完整重注册（register幂等：master按socket/acceptor:port先摘同身份旧条目
		// 再入列，任意次重发终态单条目）；仍失败则跳过本轮负载上报与分桶决策（两者都依赖
		// master侧注册就绪），等下一轮（120s）。
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
			// 快照恢复窗口（loadSnapshot→restore持raft锁：close置bucket=null，RocksDatabase.restore
			// 大桶可长达秒~分钟期间保持null；loadMonitor不持raft锁）内该桶不参与本轮统计：
			// null解引用（getSplittingMeta/load内getBucketMeta）会以NPE中止整轮loadMonitor链。
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
		loadMonitorTimer.stop(); // 有界等待在飞一轮（预算=timeoutMs+5s），不interrupt池线程
		ShutdownHook.remove(this);
		proxyServer.stop();
		masterAgent.stop();
		// 逐桶独立捕获：某桶close失败（raft.shutdown RocksDB错误等）不得中断循环——
		// 后续桶的句柄/定时任务将全部泄漏且database.close被跳过（Dbh2.close内部已保证
		// stateMachine清理必然执行，此处只需保证每桶都得到close机会）。
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
