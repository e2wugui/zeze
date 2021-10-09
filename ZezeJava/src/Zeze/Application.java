package Zeze;

import Zeze.Transaction.*;
import java.util.*;

import org.apache.log4j.Logger;

public final class Application {
	private static final Logger logger = Logger.getLogger(Application.class);

	private HashMap<String, Database> Databases = new HashMap<String, Database> ();
	public HashMap<String, Database> getDatabases() {
		return Databases;
	}
	private void setDatabases(HashMap<String, Database> value) {
		Databases = value;
	}
	private Config Config;
	public Config getConfig() {
		return Config;
	}
	private boolean IsStart;
	public boolean isStart() {
		return IsStart;
	}
	private void setStart(boolean value) {
		IsStart = value;
	}
	private Zeze.Services.ServiceManager.Agent ServiceManagerAgent;
	public Zeze.Services.ServiceManager.Agent getServiceManagerAgent() {
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
	public Zeze.Util.SimpleThreadPool InternalThreadPool;

	private Checkpoint _checkpoint;
	public Checkpoint getCheckpoint() {
		return _checkpoint;
	}
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


	public Application(String solutionName) {
		this(solutionName, null);
	}

	public Application(String solutionName, Config config) {
		SolutionName = solutionName;

		Config = config;
		if (null == getConfig()) {
			Config = Config.Load(null);
		}
		InternalThreadPool = new Zeze.Util.SimpleThreadPool(getConfig().getInternalThreadPoolWorkerCount(), "ZezeSpecialThreadPool");

		getConfig().CreateDatabase(getDatabases());
		GlobalAgent = new GlobalAgent(this);
		_checkpoint = new Checkpoint(getConfig().getCheckpointMode(), getDatabases().values());
	}

	public void AddTable(String dbName, Table table) {
		GetDatabase(dbName).AddTable(table);
	}

	public void RemoveTable(String dbName, Table table) {
		GetDatabase(dbName).RemoveTable(table);
	}

	public Table GetTable(String name) {
		for (Database db : getDatabases().values()) {
			Table t = db.GetTable(name);
			if (null != t) {
				return t;
			}
		}
		return null;
	}

	public Database GetDatabase(String name) {
		var db = getDatabases().get(name);
		if (null != db) {
			return db;
		}
		throw new RuntimeException(String.format("database not exist name=%1$s", name));
	}


	public Procedure NewProcedure(tangible.Func0Param<Integer> action, String actionName) {
		return NewProcedure(action, actionName, null);
	}

	public Procedure NewProcedure(tangible.Func0Param<Integer> action, String actionName, Object userState) {
		if (isStart()) {
			return new Procedure(this, action, actionName, userState);
		}
		throw new RuntimeException("App Not Start");
	}

	public void Start() {
		synchronized (this) {
			if (getConfig() != null) {
				getConfig().ClearInUseAndIAmSureAppStopped(getDatabases());
			}
			for (var db : getDatabases().values()) {
				db.getDirectOperates().SetInUse(getConfig().getServerId(), getConfig().getGlobalCacheManagerHostNameOrAddress());
			}

			if (isStart()) {
				return;
			}
			setStart(true);
			Zeze.Util.Task.tryInitThreadPool(this, null);

			setServiceManagerAgent(new Zeze.Services.ServiceManager.Agent(getConfig()));
			getServiceManagerAgent().WaitConnectorReady();

			Database defaultDb = GetDatabase("");
			for (var db : getDatabases().values()) {
				db.Open(this);
			}

			if (getConfig().getGlobalCacheManagerHostNameOrAddress().length() > 0) {
				getGlobalAgent().Start(getConfig().getGlobalCacheManagerHostNameOrAddress(), getConfig().getGlobalCacheManagerPort());
			}

			getCheckpoint().Start(getConfig().getCheckpointPeriod()); // 定时模式可以和其他模式混用。

			/////////////////////////////////////////////////////
			/** Schemas Check
			*/
			getSchemas().Compile();
			var keyOfSchemas = Zeze.Serialize.ByteBuffer.Allocate();
			keyOfSchemas.WriteString("zeze.Schemas." + getConfig().getServerId());
			while (true) {
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C# deconstruction declarations:
				var(data, version) = defaultDb.DirectOperates.GetDataWithVersion(keyOfSchemas);
				if (null != data) {
					var SchemasPrevious = new Schemas();
					try {
						SchemasPrevious.Decode(data);
						SchemasPrevious.Compile();
					}
					catch (RuntimeException ex) {
						SchemasPrevious = null;
						logger.Error(ex, "Schemas Implement Changed?");
					}
					if (false == getSchemas().IsCompatible(SchemasPrevious, getConfig())) {
						throw new RuntimeException("Database Struct Not Compatible!");
					}
				}
				var newdata = Serialize.ByteBuffer.Allocate();
				getSchemas().Encode(newdata);
				tangible.RefObject<Long> tempRef_version = new tangible.RefObject<Long>(version);
				if (defaultDb.getDirectOperates().SaveDataWithSameVersion(keyOfSchemas, newdata, tempRef_version)) {
				version = tempRef_version.refArgValue;
					break;
				}
			else {
				version = tempRef_version.refArgValue;
			}
			}
		}
	}

	public void Stop() {
		synchronized (this) {
			if (getGlobalAgent() != null) {
				getGlobalAgent().Stop();
			}

			if (false == isStart()) {
				return;
			}
			if (getConfig() != null) {
				getConfig().ClearInUseAndIAmSureAppStopped(getDatabases());
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
		}
	}

	public void CheckpointRun() {
		_checkpoint.RunOnce();
	}

	public Application() {
		var domain = AppDomain.CurrentDomain;
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C#-style event wireups:
		domain.UnhandledException += UnhandledExceptionEventHandler;
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C#-style event wireups:
		domain.ProcessExit += ProcessExit;
		// domain.DomainUnload += DomainUnload;
	}

	private void ProcessExit(Object sender, tangible.EventArgs e) {
		Stop();
	}

	private void UnhandledExceptionEventHandler(Object sender, UnhandledExceptionEventArgs args) {
		RuntimeException e = (RuntimeException)args.ExceptionObject;
		logger.Error(e, "UnhandledExceptionEventArgs");
	}

	private Zeze.Util.TaskOneByOneByKey TaskOneByOneByKey = new Zeze.Util.TaskOneByOneByKey();
	public Zeze.Util.TaskOneByOneByKey getTaskOneByOneByKey() {
		return TaskOneByOneByKey;
	}


	public TaskCompletionSource<Integer> Run(Func<Integer> func, String actionName, TransactionModes mode) {
		return Run(func, actionName, mode, null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public TaskCompletionSource<int> Run(Func<int> func, string actionName, TransactionModes mode, object oneByOneKey = null)
	public TaskCompletionSource<Integer> Run(tangible.Func0Param<Integer> func, String actionName, TransactionModes mode, Object oneByOneKey) {
		var future = new TaskCompletionSource<Integer>();
		switch (mode) {
			case ExecuteInTheCallerTransaction:
				future.SetResult(func.invoke());
				break;

			case ExecuteInNestedCall:
				future.SetResult(NewProcedure(func, actionName).Call());
				break;

			case ExecuteInAnotherThread:
				if (null != oneByOneKey) {
					getTaskOneByOneByKey().Execute(oneByOneKey, () -> future.SetResult(NewProcedure(func, actionName).Call()), actionName);
				}
				else {
					Zeze.Util.Task.Run(() -> future.SetResult(NewProcedure(func, actionName).Call()), actionName);
				}
				break;
		}
		return future;
	}
}