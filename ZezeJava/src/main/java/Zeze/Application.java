package Zeze;

import Zeze.Transaction.*;
import Zeze.Util.TaskCompletionSource;
import java.util.*;
import java.util.concurrent.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import Zeze.Services.ServiceManager.Agent;

public final class Application {
	static final Logger logger = LogManager.getLogger(Application.class);

	private final HashMap<String, Database> Databases = new HashMap<> ();
	public HashMap<String, Database> getDatabases() {
		return Databases;
	}

	private Config Conf;
	public Config getConfig() {
		return Conf;
	}
	private boolean IsStart;
	public boolean isStart() {
		return IsStart;
	}
	private void setStart(boolean value) {
		IsStart = value;
	}
	private Agent ServiceManagerAgent;
	public Agent getServiceManagerAgent() {
		return ServiceManagerAgent;
	}
	private void setServiceManagerAgent(Zeze.Services.ServiceManager.Agent value) {
		ServiceManagerAgent = value;
	}
	private GlobalAgent GlobalAgent;
	public GlobalAgent getGlobalAgent() {
		return GlobalAgent;
	}

	// 用来执行内部的一些重要任务，和系统默认 ThreadPool 分开，防止饥饿。
	ThreadPoolExecutor InternalThreadPool;

	public ThreadPoolExecutor __GetInternalThreadPoolUnsafe() {
		return InternalThreadPool;
	}

	private Checkpoint _checkpoint;
	public Checkpoint getCheckpoint() {
		return _checkpoint;
	}

	/*
	public void setCheckpoint(Checkpoint value) {
		synchronized (this) {
			if (null == value) {
				throw new NullPointerException();
			}
			if (isStart()) {
				throw new RuntimeException("Checkpoint only can setup before start.");
			}
			_checkpoint = value;
		}
	}
	*/

	private Locks Locks;
	public Locks getLocks() {
		return Locks;
	}

	private Schemas Schemas;
	public Schemas getSchemas() {
		return Schemas;
	}
	public void setSchemas(Schemas value) {
		Schemas = value;
	}
	private String SolutionName;
	public String getSolutionName() {
		return SolutionName;
	}


	public Application(String solutionName) throws Throwable {
		this(solutionName, null);
	}

	public Application(String solutionName, Config config) throws Throwable {
		SolutionName = solutionName;

		Conf = config;
		if (null == Conf) {
			Conf = Config.Load();
		}
		var core = getConfig().getInternalThreadPoolWorkerCount();
		core = core > 0 ? core : Runtime.getRuntime().availableProcessors() * 30;
		var poolName = "ZezeInternalPool-" + Conf.getServerId();
		InternalThreadPool = new ThreadPoolExecutor(core, core,
				0, TimeUnit.NANOSECONDS, new LinkedBlockingQueue<>(),
				new Zeze.Util.ThreadFactoryWithName(poolName));

		getConfig().CreateDatabase(this, getDatabases());
		GlobalAgent = new GlobalAgent(this);
		_checkpoint = new Checkpoint(getConfig().getCheckpointMode(), getDatabases().values(), Conf.getServerId());
		setServiceManagerAgent(new Agent(this));
	}

	public void AddTable(String dbName, Table table) {
		var db = GetDatabase(dbName);
		if (db == null)
			throw new RuntimeException("Db Not Exist. db=" + dbName);
		if (null != Tables.putIfAbsent(table.getName(), table))
			throw new RuntimeException("duplicate table name=" + table.getName());
		db.AddTable(table);
	}

	public void RemoveTable(String dbName, Table table) {
		Tables.remove(table.getName());
		GetDatabase(dbName).RemoveTable(table);
	}

	private ConcurrentHashMap<String, Table> Tables = new ConcurrentHashMap<>();

	public Table GetTable(String name) {
		return Tables.get(name);
	}

	public Database GetDatabase(String name) {
		var db = getDatabases().get(name);
		if (null != db) {
			return db;
		}
		throw new RuntimeException("database not exist name=" + name);
	}


	public Procedure NewProcedure(Zeze.Util.Func0<Long> action, String actionName) {
		return NewProcedure(action, actionName, TransactionLevel.Serializable, null);
	}

	public Procedure NewProcedure(Zeze.Util.Func0<Long> action, String actionName, TransactionLevel level, Object userState) {
		if (isStart()) {
			return new Procedure(this, action, actionName, level, userState);
		}
		throw new RuntimeException("App Not Start");
	}

	public void Start() throws Throwable {
		synchronized (this) {
			getConfig().ClearInUseAndIAmSureAppStopped(this, getDatabases());
			for (var db : getDatabases().values()) {
				db.getDirectOperates().SetInUse(getConfig().getServerId(), getConfig().getGlobalCacheManagerHostNameOrAddress());
			}

			if (isStart()) {
				return;
			}
			setStart(true);
			Locks = new Locks();
			Zeze.Util.Task.tryInitThreadPool(this, null, null);

			var serviceManagerConf = getConfig().GetServiceConf(Agent.DefaultServiceName);
			if (null != serviceManagerConf) {
				getServiceManagerAgent().getClient().Start();
				getServiceManagerAgent().WaitConnectorReady();
			}

			Database defaultDb = GetDatabase(getConfig().getDefaultTableConf().getDatabaseName());
			for (var db : getDatabases().values()) {
				db.Open(this);
			}

			if (getConfig().getGlobalCacheManagerHostNameOrAddress().length() > 0) {
				getGlobalAgent().Start(getConfig().getGlobalCacheManagerHostNameOrAddress(), getConfig().getGlobalCacheManagerPort());
			}

			getCheckpoint().Start(getConfig().getCheckpointPeriod()); // 定时模式可以和其他模式混用。

			/////////////////////////////////////////////////////
			getSchemas().Compile();
			var keyOfSchemas = Zeze.Serialize.ByteBuffer.Allocate();
			keyOfSchemas.WriteString("zeze.Schemas." + getConfig().getServerId());
			while (true) {
				var dataVersion = defaultDb.getDirectOperates().GetDataWithVersion(keyOfSchemas);
				long version = 0;
				if (dataVersion !=null && null != dataVersion.Data) {
					var SchemasPrevious = new Schemas();
					try {
						SchemasPrevious.Decode(dataVersion.Data);
						SchemasPrevious.Compile();
					}
					catch (Throwable ex) {
						SchemasPrevious = null;
						logger.error("Schemas Implement Changed?", ex);
					}
					getSchemas().CheckCompatible(SchemasPrevious, this);
					version = dataVersion.Version;
				}
				var newdata = Zeze.Serialize.ByteBuffer.Allocate();
				getSchemas().Encode(newdata);
				var versionRc = defaultDb.getDirectOperates().SaveDataWithSameVersion(keyOfSchemas, newdata, version);
				if (versionRc.getValue())
					break;
			}
			FlushWhenReduceTimerTask = Zeze.Util.Task.schedule(this::FlushWhenReduceTimer, 60 * 1000, 60 * 1000);
		}
	}

	public void Stop() throws Throwable {
		synchronized (this) {
			if (getGlobalAgent() != null) {
				getGlobalAgent().Stop();
			}

			if (!isStart()) {
				return;
			}
			if (null != FlushWhenReduceTimerTask) {
				FlushWhenReduceTimerTask.Cancel();
				FlushWhenReduceTimerTask = null;
			}

			if (getConfig() != null) {
				getConfig().ClearInUseAndIAmSureAppStopped(this, getDatabases());
			}
			setStart(false);

			if (getCheckpoint() != null) {
				getCheckpoint().StopAndJoin();
			}
			for (var db : getDatabases().values()) {
				db.Close();
			}
			getDatabases().clear();
			getServiceManagerAgent().Stop();
			InternalThreadPool = null;
			Locks = null;
			Conf = null;
		}
	}

	public void CheckpointRun() {
		_checkpoint.RunOnce();
	}

	public static class LastFlushWhenReduce
	{
		public TableKey Key;
		public long LastGlobalSerialId;
		public long Ticks; // System.currentTimeMillis()
		public boolean Removed;

		public LastFlushWhenReduce(TableKey tkey) {
			Key = tkey;
		}
	}

	private final ConcurrentHashMap<TableKey, LastFlushWhenReduce> FlushWhenReduce = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, Zeze.Util.IdentityHashSet<LastFlushWhenReduce>> FlushWhenReduceActives = new ConcurrentHashMap<>();
	private Zeze.Util.Task FlushWhenReduceTimerTask;

	public final static long MillisPerMinute = 60 * 1000;

	public void __SetLastGlobalSerialId(TableKey tkey, long globalSerialId)
	{
		while (true) {
			var last = FlushWhenReduce.computeIfAbsent(tkey, LastFlushWhenReduce::new);
			synchronized (last) {
				if (last.Removed)
					continue;

				last.LastGlobalSerialId = globalSerialId;
				last.Ticks = System.currentTimeMillis();
				last.notifyAll();
				var minutes = last.Ticks / MillisPerMinute;
				FlushWhenReduceActives.computeIfAbsent(minutes, (k) -> new Zeze.Util.IdentityHashSet<>()).Add(last);
				return;
			}
		}
	}

	public boolean __TryWaitFlushWhenReduce(TableKey tkey, long hope)
	{
		while (true)
		{
			var last = FlushWhenReduce.computeIfAbsent(tkey, LastFlushWhenReduce::new);
			synchronized (last) {
				if (last.Removed)
					continue;

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

	public final static long FlushWhenReduceIdleMinuts = 30;

	private void FlushWhenReduceTimer(Zeze.Util.Task ThisTask) {
		var minuts = System.currentTimeMillis() / MillisPerMinute;

		for  (var active : FlushWhenReduceActives.entrySet()) {
			if (active.getKey() - minuts > FlushWhenReduceIdleMinuts) {
				for  (var last : active.getValue()) {
					synchronized (last) {
						if (last.Removed)
							continue;

						if (last.Ticks / MillisPerMinute > FlushWhenReduceIdleMinuts) {
							if (FlushWhenReduce.remove(last.Key) != null) {
								last.Removed = true;
							}
						}
					}
				}
			}
		}
	}

	public Application() {
		Runtime.getRuntime().addShutdownHook(new Thread("zeze.ShutdownHook") {
			@Override
			public void run() {
				logger.fatal("zeze stop start ... from ShutdownHook.");
				try {
					Stop();
				} catch (Throwable e) {
					logger.error(e);
				}
			}
		});
	}

	private final Zeze.Util.TaskOneByOneByKey TaskOneByOneByKey = new Zeze.Util.TaskOneByOneByKey();
	public Zeze.Util.TaskOneByOneByKey getTaskOneByOneByKey() {
		return TaskOneByOneByKey;
	}


	public TaskCompletionSource<Long> Run(Zeze.Util.Func0<Long> func, String actionName, TransactionModes mode) {
		return Run(func, actionName, mode, null);
	}

	public TaskCompletionSource<Long> Run(Zeze.Util.Func0<Long> func, String actionName, TransactionModes mode, Object oneByOneKey) {
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
					if (null != oneByOneKey) {
						getTaskOneByOneByKey().Execute(
								oneByOneKey,
								() -> future.SetResult(NewProcedure(func, actionName).Call()),
								actionName);
					} else {
						Zeze.Util.Task.Run(
								() -> future.SetResult(NewProcedure(func, actionName).Call()),
								actionName);
					}
					break;
			}
		} catch (Throwable e) {
			future.SetException(e);
		}
		return future;
	}
}