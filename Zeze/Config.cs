﻿using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;
using System.Xml.Schema;
using Zeze.Gen;

namespace Zeze
{
    public class Config
    {
        public enum DbType
        {
            Memory,
            MySql,
            SqlServer,
        }

        public DbType DatabaseType { get; set; } = DbType.Memory;
        public string DatabaseUrl { get; set; } = "";
        public int CheckpointPeriod { get; set; } = 60000; // 60 seconds
        public int AutoKeyLocalId { get; } = 0;
        public int AutoKeyLocalStep { get; } = 4096;
        public string GlobalCacheManagerHostNameOrAddress { get; }
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

        private Transaction.Database CreateDatabase(Zeze.Application zeze, DbType dbType, string url)
        {
            switch (dbType)
            {
                case DbType.Memory:
                    return new Transaction.DatabaseMemory(zeze, url);
                case DbType.MySql:
                    return new Transaction.DatabaseMySql(zeze, url);
                case DbType.SqlServer:
                    return new Transaction.DatabaseSqlServer(zeze, url);
                default:
                    throw new Exception("unknown database type.");
            }
        }

        public void CreateDatabase(Zeze.Application zeze, Dictionary<string, Transaction.Database> map)
        {
            // add default database
            map.Add("", CreateDatabase(zeze, DatabaseType, DatabaseUrl));
            // add other database
            foreach (var db in DatabaseConfMap.Values)
            {
                map.Add(db.Name, CreateDatabase(zeze, db.DatabaseType, db.DatabaseUrl));
            }
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

            switch (self.GetAttribute("DatabaseType"))
            {
                case "Memory": DatabaseType = DbType.Memory; break;
                case "MySql": DatabaseType = DbType.MySql; break;
                case "SqlServer": DatabaseType = DbType.SqlServer; break;
                default: throw new Exception("unknown database type.");
            }

            DatabaseUrl = self.GetAttribute("DatabaseUrl");
            CheckpointPeriod = int.Parse(self.GetAttribute("CheckpointPeriod"));
            AutoKeyLocalId = int.Parse(self.GetAttribute("AutoKeyLocalId"));
            AutoKeyLocalStep = int.Parse(self.GetAttribute("AutoKeyLocalStep"));
            GlobalCacheManagerHostNameOrAddress = self.GetAttribute("GlobalCacheManagerHostNameOrAddress");
            string attr = self.GetAttribute("GlobalCacheManagerPort");
            GlobalCacheManagerPort = attr.Length > 0 ? int.Parse(attr) : 0;

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
                    default: throw new Exception("unknown node name: " + e.Name);
                }
            }
        }

        public class DatabaseConf
        { 
            public string Name { get; set; }
            public DbType DatabaseType { get; set; }
            public string DatabaseUrl { get; set; }

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

        public class TableConf
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

        public Config()
        {
        }
    }
}
