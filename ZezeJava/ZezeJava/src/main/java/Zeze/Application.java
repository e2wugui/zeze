package Zeze;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Arch.RedirectBase;
import Zeze.Collections.Queue;
import Zeze.Component.AutoKey;
import Zeze.Component.DelayRemove;
import Zeze.Component.Timer;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.Daemon;
import Zeze.Services.GlobalCacheManagerWithRaftAgent;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Transaction.AchillesHeelDaemon;
import Zeze.Transaction.Checkpoint;
import Zeze.Transaction.Database;
import Zeze.Transaction.DatabaseRocksDb;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.GlobalAgent;
import Zeze.Transaction.IGlobalAgent;
import Zeze.Transaction.Locks;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.ResetDB;
import Zeze.Transaction.Table;
import Zeze.Transaction.TableKey;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.FuncLong;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Str;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Application {
	static final Logger logger = LogManager.getLogger(Application.class);

	private final String solutionName;
	private final Config conf;
	private final HashMap<String, Database> databases = new HashMap<>();
	private final LongConcurrentHashMap<Table> tables = new LongConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Table> tableNameMap = new ConcurrentHashMap<>();
	private final TaskOneByOneByKey taskOneByOneByKey = new TaskOneByOneByKey();
	private final Locks locks = new Locks();
	private final Agent serviceManagerAgent;
	private AutoKey.Module autoKey;
	private Timer timer;
	private Zeze.Collections.Queue.Module queueModule;
	private Zeze.Component.DelayRemove delayRemove;
	private IGlobalAgent globalAgent;
	private Zeze.Transaction.AchillesHeelDaemon achillesHeelDaemon;
	private Checkpoint checkpoint;
	private Future<?> flushWhenReduceTimerTask;
	private Schemas schemas;
	private boolean isStart;
	public RedirectBase redirect;

	public Zeze.Transaction.AchillesHeelDaemon getAchillesHeelDaemon() {
		return achillesHeelDaemon;
	}

	/**
	 * 本地Rocks缓存数据库虽然也用了Database接口，但它不给用户提供事务操作的表。
	 * 1. 不需要加入到Databases里面。
	 * 2. 不需要在里面注册表(Database.AddTable)。
	 * 3. Flush的时候特殊处理。see Checkpoint。
	 */
	private DatabaseRocksDb LocalRocksCacheDb;

	public DatabaseRocksDb getLocalRocksCacheDb() {
		return LocalRocksCacheDb;
	}

	public Application(String solutionName) throws Throwable {
		this(solutionName, null);
	}

	public Application(String solutionName, Config config) throws Throwable {
		this.solutionName = solutionName;
		conf = config != null ? config : Config.load();
		conf.createDatabase(this, databases);
		serviceManagerAgent = new Agent(this);
		ShutdownHook.add(this, () -> {
			logger.info("zeze({}) ShutdownHook begin", this.solutionName);
			stop();
			logger.info("zeze({}) ShutdownHook end", this.solutionName);
		});
	}

	public Application() {
		solutionName = "";
		conf = null;
		serviceManagerAgent = null;
		ShutdownHook.add(this, () -> {
			logger.info("zeze ShutdownHook begin");
			stop();
			logger.info("zeze ShutdownHook end");
		});
	}

	public HashMap<String, Database> getDatabases() {
		return databases;
	}

	public Config getConfig() {
		return conf;
	}

	public boolean isStart() {
		return isStart;
	}

	public Agent getServiceManagerAgent() {
		return serviceManagerAgent;
	}

	public IGlobalAgent getGlobalAgent() {
		return globalAgent;
	}

	public Checkpoint getCheckpoint() {
		return checkpoint;
	}

	/*
	public synchronized void setCheckpoint(Checkpoint value) {
		if (value == null)
			throw new NullPointerException();
		if (IsStart)
			throw new IllegalStateException("Checkpoint only can setup before start.");
		_checkpoint = value;
	}
	*/

	public Locks getLocks() {
		return locks;
	}

	public Schemas getSchemas() {
		return schemas;
	}

	public void setSchemas(Schemas value) {
		schemas = value;
	}

	public String getSolutionName() {
		return solutionName;
	}

	public Database addTable(String dbName, Table table) {
		TableKey.tables.put(table.getId(), table.getName());
		var db = getDatabase(dbName);
		if (tables.putIfAbsent(table.getId(), table) != null)
			throw new IllegalStateException("duplicate table id=" + table.getId());
		if (tableNameMap.putIfAbsent(table.getName(), table) != null)
			throw new IllegalStateException("duplicate table name=" + table.getName());
		db.addTable(table);
		return db;
	}

	public synchronized void openDynamicTable(String dbName, Table table) {
		addTable(dbName, table).openDynamicTable(this, table);
	}

	public void removeTable(String dbName, Table table) {
		tables.remove(table.getId());
		tableNameMap.remove(table.getName());
		getDatabase(dbName).removeTable(table);
	}

	public Table getTable(int id) {
		return tables.get(id);
	}

	public Table getTable(String name) {
		return tableNameMap.get(name);
	}

	public Map<String, Table> getTables() {
		return Collections.unmodifiableMap(tableNameMap);
	}

	public Database getDatabase(String name) {
		var db = databases.get(name);
		if (db == null)
			throw new IllegalStateException("database not exist name=" + name);
		return db;
	}

	public AutoKey getAutoKey(String name) {
		return autoKey.getOrAdd(name);
	}

	public Timer getTimer() {
		return timer;
	}

	public Zeze.Collections.Queue.Module getQueueModule() {
		return queueModule;
	}

	public Zeze.Component.DelayRemove getDelayRemove() {
		return delayRemove;
	}

	@Deprecated //use newProcedure
	public Procedure NewProcedure(FuncLong action, String actionName) {
		return newProcedure(action, actionName);
	}

	public Procedure newProcedure(FuncLong action, String actionName) {
		return newProcedure(action, actionName, TransactionLevel.Serializable, null);
	}

	public Procedure newProcedure(FuncLong action, String actionName, TransactionLevel level, Object userState) {
		if (!isStart)
			throw new IllegalStateException("App Not Start");
		return new Procedure(this, action, actionName, level, userState);
	}

	static void deleteDirectory(File directoryToBeDeleted) throws IOException, InterruptedException {
		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectory(file);
			}
		}
		for (int i = 0; directoryToBeDeleted.exists(); i++) {
			//noinspection ResultOfMethodCallIgnored
			directoryToBeDeleted.delete();
			if (!directoryToBeDeleted.exists())
				break;
			if (i >= 50)
				throw new IOException("delete failed: " + directoryToBeDeleted.getAbsolutePath());
			//noinspection BusyWait
			Thread.sleep(100);
		}
	}

	public synchronized void start() throws Throwable {
		if (isStart)
			return;
		var serverId = conf != null ? conf.getServerId() : -1;
		logger.info("Start ServerId={}", serverId);

		// Start Thread Pool
		Task.tryInitThreadPool(this, null, null); // 确保Task线程池已经建立,如需定制,在Start前先手动初始化

		if (serverId >= 0) {
			// 自动初始化的组件。
			autoKey = new AutoKey.Module(this);
			queueModule = new Queue.Module(this);
			delayRemove = new DelayRemove(this);
			timer = new Timer(this);

			if ("true".equals(System.getProperty(Daemon.propertyNameClearInUse)))
				conf.clearInUseAndIAmSureAppStopped(this, databases);

			// Set Database InUse
			for (var db : databases.values())
				db.getDirectOperates().setInUse(serverId, conf.getGlobalCacheManagerHostNameOrAddress());

			// Open RocksCache
			var dbConf = new Config.DatabaseConf();
			dbConf.setName("zeze_cache_" + serverId);
			dbConf.setDatabaseUrl(dbConf.getName());
			deleteDirectory(new File(dbConf.getDatabaseUrl()));
			dbConf.setDatabaseType(Config.DbType.RocksDb);
			LocalRocksCacheDb = new DatabaseRocksDb(dbConf);
			LocalRocksCacheDb.open(this);
		}

		// Start ServiceManager
		var serviceManagerConf = conf != null ? conf.getServiceConf(Agent.defaultServiceName) : null;
		if (serviceManagerConf != null && serviceManagerAgent != null) {
			serviceManagerAgent.getClient().Start();
			serviceManagerAgent.waitConnectorReady();
		}

		if (serverId >= 0) {
			// Open Databases
			for (var db : databases.values())
				db.open(this);

			// Open Global
			var hosts = Str.trim(conf.getGlobalCacheManagerHostNameOrAddress().split(";"));
			if (hosts.length > 0) {
				var isRaft = hosts[0].endsWith(".xml");
				if (!isRaft) {
					var impl = new GlobalAgent(this, hosts, conf.getGlobalCacheManagerPort());
					globalAgent = impl;
					achillesHeelDaemon = new AchillesHeelDaemon(this, impl.agents);
					impl.start();
				} else {
					var impl = new GlobalCacheManagerWithRaftAgent(this, hosts);
					globalAgent = impl;
					achillesHeelDaemon = new AchillesHeelDaemon(this, impl.agents);
					impl.start();
				}
			}

			// Checkpoint
			checkpoint = new Checkpoint(this, conf.getCheckpointMode(), databases.values(), serverId);
			checkpoint.start(conf.getCheckpointPeriod()); // 定时模式可以和其他模式混用。

			/////////////////////////////////////////////////////
			// Schemas
			var defaultDb = getDatabase(conf.getDefaultTableConf().getDatabaseName());
			if (schemas != null) {
				schemas.compile();
				var keyOfSchemas = ByteBuffer.Allocate(24);
				keyOfSchemas.WriteString("zeze.Schemas." + serverId);
				while (true) {
					var dataVersion = defaultDb.getDirectOperates().getDataWithVersion(keyOfSchemas);
					long version = 0;
					if (dataVersion != null && dataVersion.data != null) {
						var SchemasPrevious = new Schemas();
						try {
							SchemasPrevious.decode(dataVersion.data);
							SchemasPrevious.compile();
						} catch (Throwable ex) {
							SchemasPrevious = null;
							logger.error("Schemas Implement Changed?", ex);
						}
						ResetDB.checkAndRemoveTable(SchemasPrevious, this);
						version = dataVersion.version;
					}
					var newData = ByteBuffer.Allocate(1024);
					schemas.encode(newData);
					var versionRc = defaultDb.getDirectOperates().saveDataWithSameVersion(keyOfSchemas, newData, version);
					if (versionRc.getValue())
						break;
				}
			}
			// start last
			if (null != achillesHeelDaemon)
				achillesHeelDaemon.start();
			isStart = true;

			timer.start();
			delayRemove.start();
		}
	}

	public synchronized void stop() throws Throwable {
		if (!isStart)
			return;
		isStart = false;
		logger.info("Stop ServerId={}", conf != null ? conf.getServerId() : -1);

		if (null != delayRemove) {
			delayRemove.stop();
			delayRemove = null;
		}

		if (null != timer) {
			timer.stop();
			timer = null;
		}

		ShutdownHook.remove(this);

		if (null != achillesHeelDaemon) {
			achillesHeelDaemon.stopAndJoin();
		}

		if (globalAgent != null) {
			globalAgent.close();
			globalAgent = null;
		}
		if (flushWhenReduceTimerTask != null) {
			flushWhenReduceTimerTask.cancel(false);
			flushWhenReduceTimerTask = null;
		}

		if (checkpoint != null) {
			checkpoint.stopAndJoin();
			checkpoint = null;
		}

		if (LocalRocksCacheDb != null) {
			var dir = LocalRocksCacheDb.getDatabaseUrl();
			LocalRocksCacheDb.close();
			deleteDirectory(new File(dir));
			LocalRocksCacheDb = null;
		}

		if (serviceManagerAgent != null)
			serviceManagerAgent.stop();

		delayRemove = null;
		if (queueModule != null) {
			queueModule.UnRegisterZezeTables(this);
			queueModule = null;
		}
		if (autoKey != null) {
			autoKey.UnRegisterZezeTables(this);
			autoKey = null;
		}

		if (conf != null)
			conf.clearInUseAndIAmSureAppStopped(this, databases);

		for (var db : databases.values())
			db.close();
	}

	public void checkpointRun() {
		checkpoint.runOnce();
	}

	public TaskOneByOneByKey getTaskOneByOneByKey() {
		return taskOneByOneByKey;
	}

	public void runTaskOneByOneByKey(Object oneByOneKey, String actionName, FuncLong func) {
		taskOneByOneByKey.Execute(oneByOneKey, newProcedure(func, actionName), DispatchMode.Normal);
	}

	public void runTaskOneByOneByKey(int oneByOneKey, String actionName, FuncLong func) {
		taskOneByOneByKey.Execute(oneByOneKey, newProcedure(func, actionName), DispatchMode.Normal);
	}

	public void runTaskOneByOneByKey(long oneByOneKey, String actionName, FuncLong func) {
		taskOneByOneByKey.Execute(oneByOneKey, newProcedure(func, actionName), DispatchMode.Normal);
	}
}
