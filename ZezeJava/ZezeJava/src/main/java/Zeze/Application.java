package Zeze;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import Zeze.Component.AutoKey;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManagerWithRaftAgent;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Transaction.Checkpoint;
import Zeze.Transaction.Database;
import Zeze.Transaction.GlobalAgent;
import Zeze.Transaction.IGlobalAgent;
import Zeze.Transaction.Locks;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Table;
import Zeze.Transaction.TableKey;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.Func0;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskOneByOneByKey;
import Zeze.Util.ThreadFactoryWithName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Application {
	static final Logger logger = LogManager.getLogger(Application.class);
	private static final long MillisPerMinute = 60 * 1000;
	private static final long FlushWhenReduceIdleMinutes = 30;

	private final String SolutionName;
	private final HashMap<String, Database> Databases = new HashMap<>();
	private final ConcurrentHashMap<String, Table> Tables = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<TableKey, LastFlushWhenReduce> FlushWhenReduce = new ConcurrentHashMap<>();
	private final LongConcurrentHashMap<ConcurrentHashSet<LastFlushWhenReduce>> FlushWhenReduceActives = new LongConcurrentHashMap<>();
	private final TaskOneByOneByKey TaskOneByOneByKey = new TaskOneByOneByKey();
	private final Checkpoint _checkpoint;
	private final Agent ServiceManagerAgent;
	private final AutoKey.Module autoKey = new AutoKey.Module();
	private IGlobalAgent GlobalAgent;
	private Config Conf;
	private ThreadPoolExecutor InternalThreadPool; // 用来执行内部的一些重要任务，和系统默认 ThreadPool 分开，防止饥饿。
	private Future<?> FlushWhenReduceTimerTask;
	private Locks Locks;
	private Schemas Schemas;
	private boolean IsStart;

	public Application(String solutionName) throws Throwable {
		this(solutionName, null);
	}

	public Application(String solutionName, Config config) throws Throwable {
		SolutionName = solutionName;

		Conf = config != null ? config : Config.Load();
		int core = Conf.getInternalThreadPoolWorkerCount();
		core = core > 0 ? core : Runtime.getRuntime().availableProcessors() * 30;
		String poolName = "ZezeInternalPool-" + Conf.getServerId();
		InternalThreadPool = new ThreadPoolExecutor(core, core,
				0, TimeUnit.NANOSECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryWithName(poolName));

		Conf.CreateDatabase(this, Databases);
		_checkpoint = new Checkpoint(Conf.getCheckpointMode(), Databases.values(), Conf.getServerId());
		ServiceManagerAgent = new Agent(this);
	}

	public Application() {
		SolutionName = "";
		_checkpoint = null;
		ServiceManagerAgent = null;
		GlobalAgent = null;
		Runtime.getRuntime().addShutdownHook(new Thread("zeze.ShutdownHook") {
			@Override
			public void run() {
				logger.info("zeze stop start ... from ShutdownHook.");
				try {
					Stop();
				} catch (Throwable e) {
					logger.error("Stop Exception in ShutdownHook", e);
				}
			}
		});
	}

	public HashMap<String, Database> getDatabases() {
		return Databases;
	}

	public Config getConfig() {
		return Conf;
	}

	public boolean isStart() {
		return IsStart;
	}

	public Agent getServiceManagerAgent() {
		return ServiceManagerAgent;
	}

	public IGlobalAgent getGlobalAgent() {
		return GlobalAgent;
	}

	public ThreadPoolExecutor __GetInternalThreadPoolUnsafe() {
		return InternalThreadPool;
	}

	public Checkpoint getCheckpoint() {
		return _checkpoint;
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
		return Locks;
	}

	public Schemas getSchemas() {
		return Schemas;
	}

	public void setSchemas(Schemas value) {
		Schemas = value;
	}

	public String getSolutionName() {
		return SolutionName;
	}

	public void AddTable(String dbName, Table table) {
		var db = GetDatabase(dbName);
		if (Tables.putIfAbsent(table.getName(), table) != null)
			throw new IllegalStateException("duplicate table name=" + table.getName());
		db.AddTable(table);
	}

	public void RemoveTable(String dbName, Table table) {
		Tables.remove(table.getName());
		GetDatabase(dbName).RemoveTable(table);
	}

	public Table GetTable(String name) {
		return Tables.get(name);
	}

	public Database GetDatabase(String name) {
		var db = Databases.get(name);
		if (db == null)
			throw new IllegalStateException("database not exist name=" + name);
		return db;
	}

	public AutoKey GetAutoKey(String name) {
		return autoKey.getOrAdd(name);
	}

	public Procedure NewProcedure(Func0<Long> action, String actionName) {
		return NewProcedure(action, actionName, TransactionLevel.Serializable, null);
	}

	public Procedure NewProcedure(Func0<Long> action, String actionName, TransactionLevel level, Object userState) {
		if (!IsStart)
			throw new IllegalStateException("App Not Start");
		return new Procedure(this, action, actionName, level, userState);
	}

	public synchronized void Start() throws Throwable {
		Conf.ClearInUseAndIAmSureAppStopped(this, Databases);
		for (var db : Databases.values())
			db.getDirectOperates().SetInUse(Conf.getServerId(), Conf.getGlobalCacheManagerHostNameOrAddress());

		if (IsStart)
			return;
		IsStart = true;

		// 自动初始化的组件。
		autoKey.initialize(this);

		Locks = new Locks();
		Task.tryInitThreadPool(this, null, null);

		var serviceManagerConf = Conf.GetServiceConf(Agent.DefaultServiceName);
		if (serviceManagerConf != null) {
			ServiceManagerAgent.getClient().Start();
			ServiceManagerAgent.WaitConnectorReady();
		}

		Database defaultDb = GetDatabase(Conf.getDefaultTableConf().getDatabaseName());
		for (var db : Databases.values())
			db.Open(this);

		var hosts = Conf.getGlobalCacheManagerHostNameOrAddress().split(";");
		if (hosts.length > 0) {
			var isRaft = hosts[0].endsWith(".xml");
			if (!isRaft) {
				var impl = new GlobalAgent(this);
				impl.Start(hosts, Conf.getGlobalCacheManagerPort());
				GlobalAgent = impl;
			} else {
				var impl = new GlobalCacheManagerWithRaftAgent(this);
				impl.Start(hosts);
				GlobalAgent = impl;
			}
		}

		_checkpoint.Start(Conf.getCheckpointPeriod()); // 定时模式可以和其他模式混用。

		/////////////////////////////////////////////////////
		Schemas.Compile();
		var keyOfSchemas = ByteBuffer.Allocate(24);
		keyOfSchemas.WriteString("zeze.Schemas." + Conf.getServerId());
		while (true) {
			var dataVersion = defaultDb.getDirectOperates().GetDataWithVersion(keyOfSchemas);
			long version = 0;
			if (dataVersion != null && dataVersion.Data != null) {
				var SchemasPrevious = new Schemas();
				try {
					SchemasPrevious.Decode(dataVersion.Data);
					SchemasPrevious.Compile();
				} catch (Throwable ex) {
					SchemasPrevious = null;
					logger.error("Schemas Implement Changed?", ex);
				}
				Schemas.CheckCompatible(SchemasPrevious, this);
				version = dataVersion.Version;
			}
			var newData = ByteBuffer.Allocate(1024);
			Schemas.Encode(newData);
			var versionRc = defaultDb.getDirectOperates().SaveDataWithSameVersion(keyOfSchemas, newData, version);
			if (versionRc.getValue())
				break;
		}
		FlushWhenReduceTimerTask = Task.schedule(60 * 1000, 60 * 1000, this::FlushWhenReduceTimer);
	}

	public synchronized void Stop() throws Throwable {
		if (GlobalAgent != null)
			GlobalAgent.close();

		if (!IsStart)
			return;
		if (FlushWhenReduceTimerTask != null) {
			FlushWhenReduceTimerTask.cancel(false);
			FlushWhenReduceTimerTask = null;
		}

		if (Conf != null)
			Conf.ClearInUseAndIAmSureAppStopped(this, Databases);

		IsStart = false;

		if (_checkpoint != null)
			_checkpoint.StopAndJoin();
		for (var db : Databases.values())
			db.Close();
		Databases.clear();
		ServiceManagerAgent.Stop();
		autoKey.finalize(this);
		InternalThreadPool = null;
		Locks = null;
		Conf = null;
	}

	public void CheckpointRun() {
		_checkpoint.RunOnce();
	}

	private static final class LastFlushWhenReduce {
		public final TableKey Key;
		public volatile long LastGlobalSerialId;
		public long Ticks; // System.currentTimeMillis()
		public boolean Removed;

		public LastFlushWhenReduce(TableKey tkey) {
			Key = tkey;
		}
	}

	public void __SetLastGlobalSerialId(TableKey tkey, long globalSerialId) {
		while (true) {
			var last = FlushWhenReduce.computeIfAbsent(tkey, LastFlushWhenReduce::new);
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (last) {
				if (!last.Removed) {
					last.LastGlobalSerialId = globalSerialId;
					last.Ticks = System.currentTimeMillis();
					last.notifyAll();
					var minutes = last.Ticks / MillisPerMinute;
					FlushWhenReduceActives.computeIfAbsent(minutes, (k) -> new ConcurrentHashSet<>()).add(last);
					return;
				}
			}
		}
	}

	public boolean __TryWaitFlushWhenReduce(TableKey tkey, long hope) {
		while (true) {
			var last = FlushWhenReduce.computeIfAbsent(tkey, LastFlushWhenReduce::new);
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (last) {
				if (!last.Removed) {
					while (last.LastGlobalSerialId < hope) {
						// 超时的时候，马上返回。
						// 这个机制的是为了防止忙等。
						// 所以不需要严格等待成功。
						try {
							last.wait(5000);
						} catch (InterruptedException skip) {
							logger.error(skip);
							return false;
						}
					}
					return true;
				}
			}
		}
	}

	private void FlushWhenReduceTimer() {
		var minutes = System.currentTimeMillis() / MillisPerMinute;

		for (var it = FlushWhenReduceActives.entryIterator(); it.moveToNext(); ) {
			if (it.key() - minutes > FlushWhenReduceIdleMinutes) {
				for (var last : it.value()) {
					//noinspection SynchronizationOnLocalVariableOrMethodParameter
					synchronized (last) {
						if (last.Removed)
							continue;

						if (last.Ticks / MillisPerMinute > FlushWhenReduceIdleMinutes) {
							if (FlushWhenReduce.remove(last.Key) != null)
								last.Removed = true;
						}
					}
				}
			}
		}
	}

	public TaskOneByOneByKey getTaskOneByOneByKey() {
		return TaskOneByOneByKey;
	}

	public TaskCompletionSource<Long> Run(Func0<Long> func, String actionName, TransactionModes mode) {
		return Run(func, actionName, mode, null);
	}

	public TaskCompletionSource<Long> Run(Func0<Long> func, String actionName, TransactionModes mode, Object oneByOneKey) {
		final var future = new TaskCompletionSource<Long>();
		try {
			switch (mode) {
			case ExecuteInTheCallerTransaction:
				future.SetResult(func.call());
				break;

			case ExecuteInNestedCall:
				future.SetResult(NewProcedure(func, actionName).Call());
				break;

			case ExecuteInAnotherThread:
				if (oneByOneKey != null) {
					getTaskOneByOneByKey().Execute(oneByOneKey,
							() -> future.SetResult(NewProcedure(func, actionName).Call()), actionName);
				} else
					Task.run(() -> future.SetResult(NewProcedure(func, actionName).Call()), actionName);
				break;
			}
		} catch (Throwable e) {
			future.SetException(e);
		}
		return future;
	}
}
