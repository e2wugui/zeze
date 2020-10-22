using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;
using System.Xml.Schema;
using Zeze.Gen;

namespace Zeze
{
    public sealed class Config
    {
        public enum DbType
        {
            Memory,
            MySql,
            SqlServer,
        }

        public int WorkerThreads { get; set; }
        public int CompletionPortThreads { get; set; }
        public int CheckpointPeriod { get; set; } = 60000; // 60 seconds
        public int InternalThreadPoolWorkerCount { get; set; }
        public int AutoKeyLocalId { get; } = 0;
        public int AutoKeyLocalStep { get; } = 4096;
        public string GlobalCacheManagerHostNameOrAddress { get; set; }
        public int GlobalCacheManagerPort { get; }
        public Dictionary<string, TableConf> TableConfMap { get; } = new Dictionary<string, TableConf>();
        public TableConf DefaultTableConf { get; set; } = new TableConf();
        public TableConf GetTableConf(string name)
        {
            if (TableConfMap.TryGetValue(name, out var tableConf))
            {
                return tableConf;
            }
            return DefaultTableConf;
        }
        public Dictionary<string, DatabaseConf> DatabaseConfMap { get; } = new Dictionary<string, DatabaseConf>();

        private Transaction.Database CreateDatabase(DbType dbType, string url)
        {
            switch (dbType)
            {
                case DbType.Memory:
                    return new Transaction.DatabaseMemory(url);
                case DbType.MySql:
                    return new Transaction.DatabaseMySql(url);
                case DbType.SqlServer:
                    return new Transaction.DatabaseSqlServer(url);
                default:
                    throw new Exception("unknown database type.");
            }
        }

        public void CreateDatabase(Dictionary<string, Transaction.Database> map)
        {
            // add other database
            foreach (var db in DatabaseConfMap.Values)
            {
                map.Add(db.Name, CreateDatabase(db.DatabaseType, db.DatabaseUrl));
            }
        }

        public Dictionary<string, ServiceConf> ServiceConfMap { get; } = new Dictionary<string, ServiceConf>();
        public ServiceConf DefaultServiceConf { get; set; } = new ServiceConf();

        public ServiceConf GetServiceConf(string name)
        {
            if (ServiceConfMap.TryGetValue(name, out var serviceConf))
                return serviceConf;
            return DefaultServiceConf;
        }

        public static Config Load(string xmlfile = null)
        {
            if (null == xmlfile)
            {
                xmlfile = "zeze.xml";
            }

            if (System.IO.File.Exists(xmlfile))
            {
                XmlDocument doc = new XmlDocument();
                doc.Load(xmlfile);
                return new Config(doc.DocumentElement);
            }

            return new Config();
        }

        public Config(XmlElement self)
        {
            if (false == self.Name.Equals("zeze"))
                throw new Exception("is it a zeze config.");

            CheckpointPeriod = int.Parse(self.GetAttribute("CheckpointPeriod"));
            AutoKeyLocalId = int.Parse(self.GetAttribute("AutoKeyLocalId"));
            AutoKeyLocalStep = int.Parse(self.GetAttribute("AutoKeyLocalStep"));
            GlobalCacheManagerHostNameOrAddress = self.GetAttribute("GlobalCacheManagerHostNameOrAddress");

            string attr = self.GetAttribute("GlobalCacheManagerPort");
            GlobalCacheManagerPort = attr.Length > 0 ? int.Parse(attr) : 0;

            attr = self.GetAttribute("InternalThreadPoolWorkerCount");
            InternalThreadPoolWorkerCount = attr.Length > 0 ? int.Parse(attr) : 10;

            attr = self.GetAttribute("WorkerThreads");
            WorkerThreads = attr.Length > 0 ? int.Parse(attr) : -1;

            attr = self.GetAttribute("CompletionPortThreads");
            CompletionPortThreads = attr.Length > 0 ? int.Parse(attr) : -1;

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;
                switch (e.Name)
                {
                    case "TableConf": new TableConf(this, e); break;
                    case "DatabaseConf": new DatabaseConf(this, e); break;
                    case "ServiceConf": new ServiceConf(this, e); break;
                    default: throw new Exception("unknown node name: " + e.Name);
                }
            }
            if (DatabaseConfMap.Count == 0) // add default databaseconf.
                DatabaseConfMap.Add("", new DatabaseConf());
        }

        public sealed class DatabaseConf
        {
            public string Name { get; set; } = "";
            public DbType DatabaseType { get; set; } = DbType.Memory;
            public string DatabaseUrl { get; set; } = "";

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
                    default: throw new Exception("unknown database type.");
                }
                DatabaseUrl = self.GetAttribute("DatabaseUrl");
                conf.DatabaseConfMap.Add(Name, this);
            }
        }

        public sealed class TableConf
        {
            public string Name { get; set; }
            public int CacheCapaicty { get; set; } = 20000;
            public int CacheCleanPeriod { get; set; } = 3600 * 1000; // 毫秒，一小时

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

                string attr = self.GetAttribute("CacheCapaicty");
                if (attr.Length > 0)
                    CacheCapaicty = int.Parse(attr);

                attr = self.GetAttribute("CacheCleanPeriod");
                if (attr.Length > 0)
                    CacheCleanPeriod = int.Parse(attr);
                DatabaseName = self.GetAttribute("DatabaseName");
                DatabaseOldName = self.GetAttribute("DatabaseOldName");
                attr = self.GetAttribute("DatabaseOldMode");
                DatabaseOldMode = attr.Length > 0 ? int.Parse(attr) : 0;

                if (Name.Length > 0)
                {
                    conf.TableConfMap.Add(Name, this);
                }
                else
                {
                    conf.DefaultTableConf = this;
                }
            }
        }

        public sealed class ServiceConf
        {
            public string Name { get; }
            public Net.SocketOptions SocketOptions { get; set; } = new Net.SocketOptions();
            public Services.HandshakeOptions HandshakeOptions { get; set; } = new Services.HandshakeOptions();

            public int Port { get; set; } = 0;
            public string HostNameOrAddress { get; set; } = ""; // 用于server时只能是ipaddress
            public bool IsAutoReconnect { get; set; } = true; // 仅用于客户端

            public ServiceConf()
            {
                Name = "";
            }

            public ServiceConf(Config conf, XmlElement self)
            {
                Name = self.GetAttribute("Name");

                string attr;

                // SocketOptions
                attr = self.GetAttribute("NoDelay");
                if (attr.Length > 0) SocketOptions.NoDelay = bool.Parse(attr);
                attr = self.GetAttribute("SendBuffer");
                if (attr.Length > 0) SocketOptions.SendBuffer= int.Parse(attr);
                attr = self.GetAttribute("ReceiveBuffer");
                if (attr.Length > 0) SocketOptions.ReceiveBuffer = int.Parse(attr);
                attr = self.GetAttribute("InputBufferSize");
                if (attr.Length > 0) SocketOptions.InputBufferSize = int.Parse(attr);
                attr = self.GetAttribute("InputBufferMaxProtocolSize");
                if (attr.Length > 0) SocketOptions.InputBufferMaxProtocolSize = int.Parse(attr);
                attr = self.GetAttribute("OutputBufferMaxSize");
                if (attr.Length > 0) SocketOptions.OutputBufferMaxSize = int.Parse(attr);
                attr = self.GetAttribute("Backlog");
                if (attr.Length > 0) SocketOptions.Backlog = int.Parse(attr);
                attr = self.GetAttribute("SocketLogLevel");
                if (attr.Length > 0) SocketOptions.SocketLogLevel = NLog.LogLevel.FromString(attr);

                // HandshakeOptions
                foreach (string dg in self.GetAttribute("DhGroups").Split(','))
                {
                    string dgtmp = dg.Trim();
                    if (dgtmp.Length == 0)
                        continue;
                    HandshakeOptions.AddDhGroup(int.Parse(dgtmp));
                }
                attr = self.GetAttribute("SecureIp");
                if (attr.Length > 0) HandshakeOptions.SecureIp = System.Net.IPAddress.Parse(attr).GetAddressBytes();
                attr = self.GetAttribute("S2cNeedCompress");
                if (attr.Length > 0) HandshakeOptions.S2cNeedCompress = bool.Parse(attr);
                attr = self.GetAttribute("C2sNeedCompress");
                if (attr.Length > 0) HandshakeOptions.C2sNeedCompress = bool.Parse(attr);
                attr = self.GetAttribute("DhGroup");
                if (attr.Length > 0) HandshakeOptions.DhGroup = byte.Parse(attr);

                if (Name.Length > 0)
                    conf.ServiceConfMap.Add(Name, this);
                else
                    conf.DefaultServiceConf = this;

                // connection options
                attr = self.GetAttribute("Port");
                if (attr.Length > 0) Port = int.Parse(attr);
                HostNameOrAddress = self.GetAttribute("HostNameOrAddress");
                attr = self.GetAttribute("IsAutoReconnect");
                if (attr.Length > 0) IsAutoReconnect = bool.Parse(attr);
            }
        }

        public Config()
        {
        }
    }
}
