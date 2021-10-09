package Zeze;

import Zeze.Net.*;
import java.util.*;

import org.apache.logging.log4j.Level;

import java.io.*;

public final class Config {
	public interface ICustomize {
		public String getName();
		public void Parse(XmlElement self);
	}

	public enum DbType {
		Memory,
		MySql,
		SqlServer,
		Tikv;

		public static final int SIZE = java.lang.Integer.SIZE;

		public int getValue() {
			return this.ordinal();
		}

		public static DbType forValue(int value) {
			return values()[value];
		}
	}

	private int WorkerThreads;
	public int getWorkerThreads() {
		return WorkerThreads;
	}
	public void setWorkerThreads(int value) {
		WorkerThreads = value;
	}
	private int CompletionPortThreads;
	public int getCompletionPortThreads() {
		return CompletionPortThreads;
	}
	public void setCompletionPortThreads(int value) {
		CompletionPortThreads = value;
	}
	private int CheckpointPeriod = 60000;
	public int getCheckpointPeriod() {
		return CheckpointPeriod;
	}
	public void setCheckpointPeriod(int value) {
		CheckpointPeriod = value;
	}
	private Zeze.Transaction.CheckpointMode CheckpointMode = Zeze.Transaction.CheckpointMode.Period;
	public Zeze.Transaction.CheckpointMode getCheckpointMode() {
		return CheckpointMode;
	}
	public void setCheckpointMode(Zeze.Transaction.CheckpointMode value) {
		CheckpointMode = value;
	}

	private Level ProcessReturnErrorLogLevel = Level.INFO;
	public Level getProcessReturnErrorLogLevel() {
		return ProcessReturnErrorLogLevel;
	}
	public void setProcessReturnErrorLogLevel(Level value) {
		ProcessReturnErrorLogLevel = value;
	}
	private int InternalThreadPoolWorkerCount;
	public int getInternalThreadPoolWorkerCount() {
		return InternalThreadPoolWorkerCount;
	}
	public void setInternalThreadPoolWorkerCount(int value) {
		InternalThreadPoolWorkerCount = value;
	}
	private int ServerId;
	public int getServerId() {
		return ServerId;
	}
	public void setServerId(int value) {
		ServerId = value;
	}
	private String GlobalCacheManagerHostNameOrAddress;
	public String getGlobalCacheManagerHostNameOrAddress() {
		return GlobalCacheManagerHostNameOrAddress;
	}
	public void setGlobalCacheManagerHostNameOrAddress(String value) {
		GlobalCacheManagerHostNameOrAddress = value;
	}
	private int GlobalCacheManagerPort;
	public int getGlobalCacheManagerPort() {
		return GlobalCacheManagerPort;
	}
	private void setGlobalCacheManagerPort(int value) {
		GlobalCacheManagerPort = value;
	}
	private java.util.concurrent.ConcurrentHashMap<String, TableConf> TableConfMap = new java.util.concurrent.ConcurrentHashMap<String, TableConf> ();
	public java.util.concurrent.ConcurrentHashMap<String, TableConf> getTableConfMap() {
		return TableConfMap;
	}
	private TableConf DefaultTableConf;
	public TableConf getDefaultTableConf() {
		return DefaultTableConf;
	}
	public void setDefaultTableConf(TableConf value) {
		DefaultTableConf = value;
	}
	private boolean AllowReadWhenRecoredNotAccessed = true;
	public boolean getAllowReadWhenRecoredNotAccessed() {
		return AllowReadWhenRecoredNotAccessed;
	}
	public void setAllowReadWhenRecoredNotAccessed(boolean value) {
		AllowReadWhenRecoredNotAccessed = value;
	}
	private boolean AllowSchemasReuseVariableIdWithSameType = true;
	public boolean getAllowSchemasReuseVariableIdWithSameType() {
		return AllowSchemasReuseVariableIdWithSameType;
	}
	public void setAllowSchemasReuseVariableIdWithSameType(boolean value) {
		AllowSchemasReuseVariableIdWithSameType = value;
	}
	private java.util.concurrent.ConcurrentHashMap<String, ICustomize> Customize = new java.util.concurrent.ConcurrentHashMap<String, ICustomize> ();
	public java.util.concurrent.ConcurrentHashMap<String, ICustomize> getCustomize() {
		return Customize;
	}

	/** 
	 根据自定义配置名字查找。
	 因为外面需要通过AddCustomize注册进来，
	 如果外面保存了配置引用，是不需要访问这个接口的。
	 
	 <typeparam name="T"></typeparam>
	 @param name
	 @param customize
	*/
//C# TO JAVA CONVERTER TODO TASK: The C# 'new()' constraint has no equivalent in Java:
//ORIGINAL LINE: public bool GetCustomize<T>(out T customize) where T : ICustomize, new()
	public <T extends ICustomize> boolean GetCustomize(tangible.OutObject<T> customize) {
		T forName = new T();
		TValue _customize;
		tangible.OutObject<TValue> tempOut__customize = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (getCustomize().TryGetValue(forName.getName(), tempOut__customize)) {
		_customize = tempOut__customize.outArgValue;
			customize.outArgValue = (T)_customize;
			return true;
		}
	else {
		_customize = tempOut__customize.outArgValue;
	}
		customize.outArgValue = null;
		return false;
	}

	public void AddCustomize(ICustomize c) {
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (!getCustomize().TryAdd(c.getName(), c)) {
			throw new RuntimeException(String.format("Duplicate Customize Config '%1$s'", c.getName()));
		}
	}

	public TableConf GetTableConf(String name) {
		TValue tableConf;
		tangible.OutObject<TValue> tempOut_tableConf = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (getTableConfMap().TryGetValue(name, tempOut_tableConf)) {
		tableConf = tempOut_tableConf.outArgValue;
			return tableConf;
		}
	else {
		tableConf = tempOut_tableConf.outArgValue;
	}
		return getDefaultTableConf();
	}

	private java.util.concurrent.ConcurrentHashMap<String, DatabaseConf> DatabaseConfMap = new java.util.concurrent.ConcurrentHashMap<String, DatabaseConf> ();
	public java.util.concurrent.ConcurrentHashMap<String, DatabaseConf> getDatabaseConfMap() {
		return DatabaseConfMap;
	}

	private Zeze.Transaction.Database CreateDatabase(DbType dbType, String url) {
		switch (dbType) {
			case Memory:
				return new Zeze.Transaction.DatabaseMemory(url);
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if USE_DATABASE
			case MySql:
				return new Zeze.Transaction.DatabaseMySql(url);
			case SqlServer:
				return new Zeze.Transaction.DatabaseSqlServer(url);
			case Tikv:
				return new Zeze.Tikv.DatabaseTikv(url);
//#endif
			default:
				throw new RuntimeException("unknown database type.");
		}
	}

	public void CreateDatabase(HashMap<String, Zeze.Transaction.Database> map) {
		// add other database
		for (var db : getDatabaseConfMap().values()) {
			map.put(db.Name, CreateDatabase(db.DatabaseType, db.DatabaseUrl));
		}
	}


	public void ClearInUseAndIAmSureAppStopped() {
		ClearInUseAndIAmSureAppStopped(null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void ClearInUseAndIAmSureAppStopped(Dictionary<string, Transaction.Database> databases = null)
	public void ClearInUseAndIAmSureAppStopped(HashMap<String, Zeze.Transaction.Database> databases) {
		if (null == databases) {
			databases = new HashMap<String, Zeze.Transaction.Database>();
			CreateDatabase(databases);
		}
		for (var db : databases.values()) {
			db.DirectOperates.ClearInUse(getServerId(), getGlobalCacheManagerHostNameOrAddress());
		}
	}


	private java.util.concurrent.ConcurrentHashMap<String, ServiceConf> ServiceConfMap = new java.util.concurrent.ConcurrentHashMap<String, ServiceConf> ();
	public java.util.concurrent.ConcurrentHashMap<String, ServiceConf> getServiceConfMap() {
		return ServiceConfMap;
	}
	private ServiceConf DefaultServiceConf = new ServiceConf();
	public ServiceConf getDefaultServiceConf() {
		return DefaultServiceConf;
	}
	public void setDefaultServiceConf(ServiceConf value) {
		DefaultServiceConf = value;
	}

	public ServiceConf GetServiceConf(String name) {
		TValue serviceConf;
		tangible.OutObject<TValue> tempOut_serviceConf = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (getServiceConfMap().TryGetValue(name, tempOut_serviceConf)) {
		serviceConf = tempOut_serviceConf.outArgValue;
			return serviceConf;
		}
	else {
		serviceConf = tempOut_serviceConf.outArgValue;
	}
		return null;
	}

	/** 
	 由于这个方法没法加入Customize配置，为了兼容和内部测试保留，
	 应用应该自己LoadAndParse。
	 var c = new Config();
	 c.AddCustomize(...);
	 c.LoadAndParse();
	 
	 @param xmlfile
	 @return 
	*/

	public static Config Load() {
		return Load("zeze.xml");
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public static Config Load(string xmlfile = "zeze.xml")
	public static Config Load(String xmlfile) {
		return (new Config()).LoadAndParse(xmlfile);
	}


	public Config LoadAndParse() {
		return LoadAndParse("zeze.xml");
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public Config LoadAndParse(string xmlfile = "zeze.xml")
	public Config LoadAndParse(String xmlfile) {
		if ((new File(xmlfile)).isFile()) {
			XmlDocument doc = new XmlDocument();
			doc.Load(xmlfile);
			Parse(doc.DocumentElement);
		}

		return this;
	}

	public void Parse(XmlElement self) {
		if (false == self.Name.equals("zeze")) {
			throw new RuntimeException("is it a zeze config.");
		}

		setCheckpointPeriod(Integer.parseInt(self.GetAttribute("CheckpointPeriod")));
		setServerId(Integer.parseInt(self.GetAttribute("ServerId")));

		setGlobalCacheManagerHostNameOrAddress(self.GetAttribute("GlobalCacheManagerHostNameOrAddress"));
		String attr = self.GetAttribute("GlobalCacheManagerPort");
		setGlobalCacheManagerPort(attr.length() > 0 ? Integer.parseInt(attr) : 0);

		attr = self.GetAttribute("ProcessReturnErrorLogLevel");
		if (attr.length() > 0) {
			setProcessReturnErrorLogLevel(NLog.LogLevel.FromString(attr));
		}

		attr = self.GetAttribute("InternalThreadPoolWorkerCount");
		setInternalThreadPoolWorkerCount(attr.length() > 0 ? Integer.parseInt(attr) : 10);

		attr = self.GetAttribute("WorkerThreads");
		setWorkerThreads(attr.length() > 0 ? Integer.parseInt(attr) : -1);

		attr = self.GetAttribute("CompletionPortThreads");
		setCompletionPortThreads(attr.length() > 0 ? Integer.parseInt(attr) : -1);

		attr = self.GetAttribute("AllowReadWhenRecoredNotAccessed");
		setAllowReadWhenRecoredNotAccessed(attr.length() > 0 ? Boolean.parseBoolean(attr) : true);
		attr = self.GetAttribute("AllowSchemasReuseVariableIdWithSameType");
		setAllowSchemasReuseVariableIdWithSameType(attr.length() > 0 ? Boolean.parseBoolean(attr) : true);

		attr = self.GetAttribute("CheckpointMode");
		if (attr.length() > 0) {
			setCheckpointMode(Transaction.CheckpointMode.valueOf(attr));
		}

		XmlNodeList childNodes = self.ChildNodes;
		for (XmlNode node : childNodes) {
			if (XmlNodeType.Element != node.NodeType) {
				continue;
			}

			XmlElement e = (XmlElement)node;
			switch (e.Name) {
				case "TableConf":
					new TableConf(this, e);
					break;

				case "DatabaseConf":
					new DatabaseConf(this, e);
					break;

				case "ServiceConf":
					new ServiceConf(this, e);
					break;

				case "CustomizeConf":
					var cname = e.GetAttribute("Name");
					TValue customizeConf;
					tangible.OutObject<TValue> tempOut_customizeConf = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
					if (false == getCustomize().TryGetValue(cname, tempOut_customizeConf)) {
					customizeConf = tempOut_customizeConf.outArgValue;
						throw new RuntimeException(String.format("Unknown CustomizeConf Name='%1$s'", cname));
					}
				else {
					customizeConf = tempOut_customizeConf.outArgValue;
				}
					customizeConf.Parse(e);
					break;

				default:
					throw new RuntimeException("unknown node name: " + e.Name);
			}
		}
		if (null == getDefaultTableConf()) {
			setDefaultTableConf(new TableConf());
		}
		if (getDatabaseConfMap().isEmpty()) { // add default databaseconf.
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (!getDatabaseConfMap().TryAdd("", new DatabaseConf())) {
				throw new RuntimeException("Concurrent Add Default Database.");
			}
		}
	}

	public final static class DatabaseConf {
		private String Name = "";
		public String getName() {
			return Name;
		}
		private DbType DatabaseType = DbType.Memory;
		public DbType getDatabaseType() {
			return DatabaseType;
		}
		private String DatabaseUrl = "";
		public String getDatabaseUrl() {
			return DatabaseUrl;
		}

		public DatabaseConf() {
		}

		public DatabaseConf(Config conf, XmlElement self) {
			Name = self.GetAttribute("Name");
			switch (self.GetAttribute("DatabaseType")) {
				case "Memory":
					DatabaseType = DbType.Memory;
					break;
				case "MySql":
					DatabaseType = DbType.MySql;
					break;
				case "SqlServer":
					DatabaseType = DbType.SqlServer;
					break;
				case "Tikv":
					DatabaseType = DbType.Tikv;
					break;
				default:
					throw new RuntimeException("unknown database type.");
			}
			DatabaseUrl = self.GetAttribute("DatabaseUrl");
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (!conf.getDatabaseConfMap().TryAdd(getName(), this)) {
				throw new RuntimeException(String.format("Duplicate Database '%1$s'", getName()));
			}
		}
	}

	public final static class TableConf {
		private String Name;
		public String getName() {
			return Name;
		}
		private long CacheCapacity = 20000;
		public long getCacheCapacity() {
			return CacheCapacity;
		}
		public void setCacheCapacity(long value) {
			CacheCapacity = value;
		}
		private int CacheConcurrencyLevel;
		public int getCacheConcurrencyLevel() {
			return CacheConcurrencyLevel;
		}
		public void setCacheConcurrencyLevel(int value) {
			CacheConcurrencyLevel = value;
		}
		private long CacheInitialCapaicty;
		public long getCacheInitialCapaicty() {
			return CacheInitialCapaicty;
		}
		public void setCacheInitialCapaicty(long value) {
			CacheInitialCapaicty = value;
		}
		private int CacheNewAccessHotThreshold;
		public int getCacheNewAccessHotThreshold() {
			return CacheNewAccessHotThreshold;
		}
		public void setCacheNewAccessHotThreshold(int value) {
			CacheNewAccessHotThreshold = value;
		}
		private int CacheCleanPeriod = 1000;
		public int getCacheCleanPeriod() {
			return CacheCleanPeriod;
		}
		public void setCacheCleanPeriod(int value) {
			CacheCleanPeriod = value;
		}
		private int CacheBuckets = 16;
		public int getCacheBuckets() {
			return CacheBuckets;
		}
		public void setCacheBuckets(int value) {
			CacheBuckets = value;
		}
		private int CacheNewLruHotPeriod = 1000;
		public int getCacheNewLruHotPeriod() {
			return CacheNewLruHotPeriod;
		}
		public void setCacheNewLruHotPeriod(int value) {
			CacheNewLruHotPeriod = value;
		}
		private int CacheMaxLruInitialCapaicty = 100000;
		public int getCacheMaxLruInitialCapaicty() {
			return CacheMaxLruInitialCapaicty;
		}
		public void setCacheMaxLruInitialCapaicty(int value) {
			CacheMaxLruInitialCapaicty = value;
		}
		private int CacheCleanPeriodWhenExceedCapacity;
		public int getCacheCleanPeriodWhenExceedCapacity() {
			return CacheCleanPeriodWhenExceedCapacity;
		}
		public void setCacheCleanPeriodWhenExceedCapacity(int value) {
			CacheCleanPeriodWhenExceedCapacity = value;
		}
		private boolean CheckpointWhenCommit = false;
		public boolean getCheckpointWhenCommit() {
			return CheckpointWhenCommit;
		}
		public void setCheckpointWhenCommit(boolean value) {
			CheckpointWhenCommit = value;
		}

		// 自动倒库，当新库(DatabaseName)没有找到记录时，从旧库(DatabaseOldName)中读取，
		// Open 的时候找到旧库并打开Database.Table用来读取。
		// 内存表不支持倒库。
		private String DatabaseName = "";
		public String getDatabaseName() {
			return DatabaseName;
		}
		private String DatabaseOldName = "";
		public String getDatabaseOldName() {
			return DatabaseOldName;
		}
		private int DatabaseOldMode = 0;
		public int getDatabaseOldMode() {
			return DatabaseOldMode;
		}


		public TableConf() {

		}

		public TableConf(Config conf, XmlElement self) {
			Name = self.GetAttribute("Name");

			String attr = self.GetAttribute("CacheCapacity");
			if (attr.length() > 0) {
				setCacheCapacity(Long.parseLong(attr));
			}

			attr = self.GetAttribute("CacheCleanPeriod");
			if (attr.length() > 0) {
				setCacheCleanPeriod(Integer.parseInt(attr));
			}
			DatabaseName = self.GetAttribute("DatabaseName");
			DatabaseOldName = self.GetAttribute("DatabaseOldName");
			attr = self.GetAttribute("DatabaseOldMode");
			DatabaseOldMode = attr.length() > 0 ? Integer.parseInt(attr) : 0;
			attr = self.GetAttribute("CheckpointWhenCommit");
			if (attr.length() > 0) {
				setCheckpointWhenCommit(Boolean.parseBoolean(attr));
			}
			attr = self.GetAttribute("CacheConcurrencyLevel");
			if (attr.length() > 0) {
				setCacheConcurrencyLevel(Integer.parseInt(attr));
			}
			attr = self.GetAttribute("CacheInitialCapaicty");
			if (attr.length() > 0) {
				setCacheInitialCapaicty(Long.parseLong(attr));
			}
			attr = self.GetAttribute("CacheNewAccessHotThreshold");
			if (attr.length() > 0) {
				setCacheNewAccessHotThreshold(Integer.parseInt(attr));
			}
			attr = self.GetAttribute("CacheCleanPeriodWhenExceedCapacity");
			if (attr.length() > 0) {
				setCacheCleanPeriodWhenExceedCapacity(Integer.parseInt(attr));
			}
			attr = self.GetAttribute("CacheBuckets");
			if (attr.length() > 0) {
				setCacheBuckets(Integer.parseInt(attr));
			}
			attr = self.GetAttribute("CacheMaxLruInitialCapaicty");
			if (attr.length() > 0) {
				setCacheMaxLruInitialCapaicty(Integer.parseInt(attr));
			}

			if (getName().length() > 0) {
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
				if (!conf.getTableConfMap().TryAdd(getName(), this)) {
					throw new RuntimeException(String.format("Duplicate Table '%1$s'", getName()));
				}
			}
			else if (conf.getDefaultTableConf() == null) {
				conf.setDefaultTableConf(this);
			}
			else {
				throw new RuntimeException("too many DefaultTableConf.");
			}
		}
	}

	public Config() {
	}
}