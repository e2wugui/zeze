using System;
using System.Collections.Concurrent;
using System.IO;
#if !USE_CONFCS
using System.Collections.Generic;
using System.Text;
#endif
using System.Xml;
using Zeze.Net;
using Zeze.Util;

namespace Zeze
{
    public sealed class Config
    {
        public interface ICustomize
        {
            string Name { get; }
            void Parse(XmlElement self);
        }

        public ConcurrentDictionary<string, XmlElement> Customizes { get; } = new ConcurrentDictionary<string, XmlElement>();
        public LogLevel ProcessReturnErrorLogLevel { get; set; } = LogLevel.Info;

#if !USE_CONFCS
        public enum DbType
        {
            Memory,
            MySql,
            SqlServer,
            Tikv,
            RocksDb,
        }

        public string Name { get; private set; } = "";
        public int WorkerThreads { get; set; }
        public int CompletionPortThreads { get; set; }
        public int CheckpointPeriod { get; set; } = 60000; // 60 seconds
        public int CheckpointModeTableFlushConcurrent { get; set; } = 2;
        public int CheckpointModeTableFlushSetCount { get; set; } = 100;
        public Transaction.CheckpointMode CheckpointMode { get; set; }
            = Transaction.CheckpointMode.Table;

        public bool NoDatabase { get; set; } = false;

        public int ServerId { get; set; }
        public string GlobalCacheManagerHostNameOrAddress { get; set; }
        // 分成多行配置，支持多HostNameOrAddress或者多raft.xml。
        // 多行的时候，所有服务器的顺序必须保持一致。
        // 为了保持原来接口不变，多行会被编码成一个string保存到GlobalCacheManagerHostNameOrAddress中。
        public GlobalCacheManagersConf GlobalCacheManagers { get; private set; }
        public int GlobalCacheManagerPort { get; private set; }
        public ConcurrentDictionary<string, TableConf> TableConfMap { get; }
            = new ConcurrentDictionary<string, TableConf>();
        public TableConf DefaultTableConf { get; set; }
        public bool AllowReadWhenRecordNotAccessed { get; set; } = true;
        public bool AllowSchemasReuseVariableIdWithSameType { get; set; } = true;
        public bool FastRedoWhenConflict { get; set; } = false;
        public int OnlineLogoutDelay { get; set; } = 60 * 10 * 1000; // 10 minutes
        public bool DonotCheckSchemasWhenTableIsNew { get; set; } = false;

        public int DelayRemoveHourStart { get; set; } = 3;
        public int DelayRemoveHourEnd { get; set; } = 7;
        public int DelayRemoveDays { get; set; } = 7; // a week

#endif
        /// <summary>
        /// 根据自定义配置名字查找。
        /// 因为外面需要通过AddCustomize注册进来，
        /// 如果外面保存了配置引用，是不需要访问这个接口的。
        /// </summary>
        /// <param name="customize"></param>
        public void ParseCustomize(ICustomize customize)
        {
            if (Customizes.TryGetValue(customize.Name, out var xmlElement))
                customize.Parse(xmlElement);
        }

#if !USE_CONFCS
        public TableConf GetTableConf(string name)
        {
            if (TableConfMap.TryGetValue(name, out var tableConf))
                return tableConf;
            return DefaultTableConf;
        }

        public ConcurrentDictionary<string, DatabaseConf> DatabaseConfMap { get; }
            = new ConcurrentDictionary<string, DatabaseConf>();

        private Transaction.Database CreateDatabase(Application zeze, DbType dbType, DatabaseConf databaseConf)
        {
            switch (dbType)
            {
                case DbType.Memory:
                    return new Transaction.DatabaseMemory(zeze, databaseConf);
#if USE_DATABASE
                case DbType.MySql:
                    return new Transaction.DatabaseMySql(zeze, databaseConf);
                case DbType.SqlServer:
                    return new Transaction.DatabaseSqlServer(zeze, databaseConf);
                case DbType.Tikv:
                    return new Tikv.DatabaseTikv(zeze, databaseConf);
                case DbType.RocksDb:
                    return new Transaction.DatabaseRocksDb(zeze, databaseConf);
#endif
                default:
                    throw new Exception("unknown database type.");
            }
        }

        public void CreateDatabase(Application zeze, Dictionary<string, Transaction.Database> map)
        {
            // add other database
            foreach (var db in DatabaseConfMap.Values)
                map.Add(db.Name, CreateDatabase(zeze, db.DatabaseType, db));
        }

        public void ClearInUseAndIAmSureAppStopped(Application zeze,
            Dictionary<string, Transaction.Database> databases = null)
        {
            if (databases == null)
            {
                databases = new Dictionary<string, Transaction.Database>();
                CreateDatabase(zeze, databases);
            }
            foreach (var db in databases.Values)
                db.DirectOperates.ClearInUse(ServerId, GlobalCacheManagerHostNameOrAddress);
        }
#endif

        public ConcurrentDictionary<string, ServiceConf> ServiceConfMap { get; }
            = new ConcurrentDictionary<string, ServiceConf>();
        public ServiceConf DefaultServiceConf { get; internal set; } = new ServiceConf();

        public ServiceConf GetServiceConf(string name)
        {
            return ServiceConfMap.TryGetValue(name, out var serviceConf) ? serviceConf : null;
        }

        /// <summary>
        /// 由于这个方法没法加入Customize配置，为了兼容和内部测试保留，
        /// 应用应该自己LoadAndParse。
        /// var c = new Config();
        /// c.AddCustomize(...);
        /// c.LoadAndParse();
        /// </summary>
        /// <param name="xmlFile"></param>
        /// <returns></returns>
        public static Config Load(string xmlFile = "zeze.xml")
        {
            return new Config().LoadAndParse(xmlFile);
        }

        public Config LoadAndParse(string xmlFile = "zeze.xml")
        {
            if (File.Exists(xmlFile))
            {
                var doc = new XmlDocument();
                doc.Load(xmlFile);
                Parse(doc.DocumentElement);
            }

#if !USE_CONFCS
            if (DefaultTableConf == null)
                DefaultTableConf = new TableConf();
            if (DatabaseConfMap.Count == 0) // add default databaseConf.
            {
                if (!DatabaseConfMap.TryAdd("", new DatabaseConf()))
                    throw new Exception("Concurrent Add Default Database.");
            }
#endif
            return this;
        }

        public void Parse(XmlElement self)
        {
            if (!self.Name.Equals("zeze"))
                throw new Exception("is it a zeze config.");

            var attr = self.GetAttribute("ProcessReturnErrorLogLevel");
            if (attr.Length > 0)
                ProcessReturnErrorLogLevel = (LogLevel)Enum.Parse(typeof(LogLevel), attr);

#if !USE_CONFCS
            Name = self.GetAttribute("name");

            CheckpointPeriod = int.Parse(self.GetAttribute("CheckpointPeriod"));
            ServerId = int.Parse(self.GetAttribute("ServerId"));
            NoDatabase = self.GetAttribute("NoDatabase").Equals("true");

            GlobalCacheManagerHostNameOrAddress = self.GetAttribute("GlobalCacheManagerHostNameOrAddress");
            attr = self.GetAttribute("GlobalCacheManagerPort");
            GlobalCacheManagerPort = attr.Length > 0 ? int.Parse(attr) : 0;

            attr = self.GetAttribute("OnlineLogoutDelay");
            if (attr.Length > 0)
                OnlineLogoutDelay = int.Parse(attr);

            attr = self.GetAttribute("CheckpointModeTableFlushConcurrent");
            if (attr.Length > 0)
                CheckpointModeTableFlushConcurrent = int.Parse(attr);

            attr = self.GetAttribute("CheckpointModeTableFlushSetCount");
            if (attr.Length > 0)
                CheckpointModeTableFlushSetCount = int.Parse(attr);

            attr = self.GetAttribute("WorkerThreads");
            WorkerThreads = attr.Length > 0 ? int.Parse(attr) : -1;

            attr = self.GetAttribute("CompletionPortThreads");
            CompletionPortThreads = attr.Length > 0 ? int.Parse(attr) : -1;

            attr = self.GetAttribute("AllowReadWhenRecordNotAccessed");
            AllowReadWhenRecordNotAccessed = attr.Length > 0 ? bool.Parse(attr) : true;
            attr = self.GetAttribute("AllowSchemasReuseVariableIdWithSameType");
            AllowSchemasReuseVariableIdWithSameType = attr.Length > 0 ? bool.Parse(attr) : true;

            attr = self.GetAttribute("CheckpointMode");
            if (attr.Length > 0)
                CheckpointMode = (Transaction.CheckpointMode)Enum.Parse(typeof(Transaction.CheckpointMode), attr);
            if (CheckpointMode == Transaction.CheckpointMode.Period && GlobalCacheManagerHostNameOrAddress.Length > 0)
            {
                Application.logger.Warn("CheckpointMode.Period Cannot Work With Global. Change To CheckpointMode.Table Now.");
                CheckpointMode = Transaction.CheckpointMode.Table;
            }
            if (CheckpointMode == Transaction.CheckpointMode.Immediately)
                throw new NotImplementedException("Disable!");

            attr = self.GetAttribute("DonotCheckSchemasWhenTableIsNew");
            if (attr.Length > 0)
                DonotCheckSchemasWhenTableIsNew = bool.Parse(attr);

            attr = self.GetAttribute("FastRedoWhenConflict");
            if (attr.Length > 0)
                FastRedoWhenConflict = bool.Parse(attr);


            attr = self.GetAttribute("DelayRemoveHourStart");
            if (attr.Length > 0)
                DelayRemoveHourStart = int.Parse(attr);

            attr = self.GetAttribute("DelayRemoveHourEnd");
            if (attr.Length > 0)
                DelayRemoveHourEnd = int.Parse(attr);

            attr = self.GetAttribute("DelayRemoveDays");
            if (attr.Length > 0)
                DelayRemoveDays = int.Parse(attr);

#endif
            foreach (XmlNode node in self.ChildNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                var e = (XmlElement)node;
                switch (e.Name)
                {
#if !USE_CONFCS
                    case "GlobalCacheManagersConf":
                        new GlobalCacheManagersConf(this, e);
                        break;

                    case "TableConf":
                        new TableConf(this, e);
                        break;

                    case "DatabaseConf":
                        new DatabaseConf(this, e);
                        break;
#endif
                    case "ServiceConf":
                        _ = new ServiceConf(this, e);
                        break;

                    case "CustomizeConf":
                        var cname = e.GetAttribute("Name").Trim();
                        if (!Customizes.TryAdd(cname, e))
                            throw new Exception($"Unknown CustomizeConf Name='{cname}'");
                        break;

                    default:
                        throw new Exception("unknown node name: " + e.Name);
                }
            }

#if !USE_CONFCS
            if (GlobalCacheManagerHostNameOrAddress.Equals("GlobalCacheManagersConf"))
            {
                GlobalCacheManagerHostNameOrAddress = GlobalCacheManagers.ToString();
            }
#endif
        }

#if !USE_CONFCS
        public sealed class GlobalCacheManagersConf
        {
            public List<string> Hosts { get; } = new List<string>();

            public GlobalCacheManagersConf(Config conf, XmlElement self)
            {
                XmlNodeList childNodes = self.ChildNodes;
                foreach (XmlNode node in childNodes)
                {
                    if (XmlNodeType.Element != node.NodeType)
                        continue;

                    XmlElement e = (XmlElement)node;
                    switch (e.Name)
                    {
                        case "host":
                            var attr = e.GetAttribute("name").Trim();
                            Hosts.Add(attr);
                            break;
                        default:
                            throw new Exception("unknown node name: " + e.Name);
                    }
                }
                if (conf.GlobalCacheManagers != null)
                    throw new Exception("too many GlobalCacheManagersConf.");
                conf.GlobalCacheManagers = this;
            }

            public override string ToString()
            {
                var sb = new StringBuilder();
                bool first = true;
                foreach (var host in Hosts)
                {
                    if (first)
                        first = false;
                    else
                        sb.Append(";");
                    sb.Append(host);
                }
                return sb.ToString();
            }
        }

        public sealed class DatabaseConf
        {
            public string Name { get; } = "";
            public DbType DatabaseType { get; set; } = DbType.Memory;
            public string DatabaseUrl { get; set; } = "";
            public bool DisableOperates { get; set; } = false;

            public DatabaseConf()
            { 
            }

            public DatabaseConf(Config conf, XmlElement self)
            {
                Name = self.GetAttribute("Name");
                switch (self.GetAttribute("DatabaseType"))
                {
                    case "Memory": DatabaseType = DbType.Memory; break;
                    case "MySql": DatabaseType = DbType.MySql; break;
                    case "SqlServer": DatabaseType = DbType.SqlServer; break;
                    case "Tikv": DatabaseType = DbType.Tikv; break;
                    case "RocksDb": DatabaseType = DbType.RocksDb; break;
                    default: throw new Exception("unknown database type.");
                }
                DatabaseUrl = self.GetAttribute("DatabaseUrl");
                if (!conf.DatabaseConfMap.TryAdd(Name, this))
                    throw new Exception($"Duplicate Database '{Name}'");
                DisableOperates = "true".Equals(self.GetAttribute("DisableOperates"));
            }
        }

        public sealed class TableConf
        {
            public string Name { get; }
            public int CacheCapacity { get; set; } = 20000;
            public int CacheConcurrencyLevel { get; set; }
            public int CacheInitialCapacity { get; set; }
            public int CacheNewAccessHotThreshold { get; set; }
            public int CacheCleanPeriod { get; set; } = 10000;
            public int CacheNewLruHotPeriod { get; set; } = 10000;
            public int CacheMaxLruInitialCapacity { get; set; } = 100000;
            public int CacheCleanPeriodWhenExceedCapacity { get; set; } = 1000;
            public bool CheckpointWhenCommit { get; set; } = false;

            // 自动倒库，当新库(DatabaseName)没有找到记录时，从旧库(DatabaseOldName)中读取，
            // Open 的时候找到旧库并打开Database.Table用来读取。
            // 内存表不支持倒库。
            public string DatabaseName { get; } = "";
            public string DatabaseOldName { get; } = "";
            public int DatabaseOldMode { get; } = 0; // 0 none; 1 如果新库没有找到记录，尝试从旧库读取;


            public TableConf()
            {

            }

            public TableConf(Config conf, XmlElement self)
            {
                Name = self.GetAttribute("Name");

                string attr = self.GetAttribute("CacheCapacity");
                if (attr.Length > 0)
                    CacheCapacity = int.Parse(attr);

                attr = self.GetAttribute("CacheCleanPeriod");
                if (attr.Length > 0)
                    CacheCleanPeriod = int.Parse(attr);
                DatabaseName = self.GetAttribute("DatabaseName");
                DatabaseOldName = self.GetAttribute("DatabaseOldName");
                attr = self.GetAttribute("DatabaseOldMode");
                DatabaseOldMode = attr.Length > 0 ? int.Parse(attr) : 0;
                attr = self.GetAttribute("CheckpointWhenCommit");
                if (attr.Length > 0)
                    CheckpointWhenCommit = bool.Parse(attr);
                attr = self.GetAttribute("CacheConcurrencyLevel");
                if (attr.Length > 0)
                    CacheConcurrencyLevel = int.Parse(attr);
                attr = self.GetAttribute("CacheInitialCapacity");
                if (attr.Length > 0)
                    CacheInitialCapacity = int.Parse(attr);
                attr = self.GetAttribute("CacheNewAccessHotThreshold");
                if (attr.Length > 0)
                    CacheNewAccessHotThreshold = int.Parse(attr);
                attr = self.GetAttribute("CacheCleanPeriodWhenExceedCapacity");
                if (attr.Length > 0)
                    CacheCleanPeriodWhenExceedCapacity = int.Parse(attr);
                attr = self.GetAttribute("CacheMaxLruInitialCapacity");
                if (attr.Length > 0)
                    CacheMaxLruInitialCapacity = int.Parse(attr);

                if (Name.Length > 0)
                {
                    if (!conf.TableConfMap.TryAdd(Name, this))
                    {
                        throw new Exception($"Duplicate Table '{Name}'");
                    }
                }
                else if (conf.DefaultTableConf == null)
                {
                    conf.DefaultTableConf = this;
                }
                else
                    throw new Exception("too many DefaultTableConf.");
            }
        }
#endif
    }
}
