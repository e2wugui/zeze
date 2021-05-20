using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Xml;

namespace Zeze.Raft
{
    public sealed class RaftConfig
    {
        private XmlDocument XmlDocument { get; }

        private string XmlFileName { get; }
        private XmlElement Self { get; }

        internal ConcurrentDictionary<string, Node> Nodes { get; }
            = new ConcurrentDictionary<string, Node>();

        // 【这个参数不保存】
        public string Name { get; internal set; }
        // 多数确认时：大于等于这个即可，因为还有自己(Leader)。
        public int HalfCount => Nodes.Count / 2;
        public string DbHome { get; }

        /// <summary>
        /// 复制日志超时，以及发送失败重试超时。
        /// </summary>
        public int AppendEntriesTimeout { get; set; }
        /// <summary>
        /// 不精确 Heartbeat Idle 算法：
        /// 如果 AppendLogActive 则设为 false，然后等待下一次timer。
        /// 否则发送 AppendLog。
        /// </summary>
        public int LeaderHeartbeatTimer { get; set; }
        /// <summary>
        /// Leader失效检测超时，超时没有从Leader得到AppendEntries及启动新的选举。
        /// 【注意】LeaderLostTimeout > LeaderHeartbeatTimer + AppendEntriesTimeout
        /// </summary>
        public int LeaderLostTimeout { get; set; }

        /// <summary>
        /// 限制每次复制日志时打包的最大数量。
        /// </summary>
        public int MaxAppendEntiresCount { get; set; } = 500;

        private RaftConfig(XmlDocument xml, string filename, XmlElement self)
        {
            XmlDocument = xml;
            XmlFileName = filename;
            Self = self;

            Name = self.GetAttribute("Name");
            DbHome = self.GetAttribute("DbHome");
            if (string.IsNullOrEmpty(DbHome))
                DbHome = ".";

            var attr = self.GetAttribute("AppendEntriesTimeout");
            AppendEntriesTimeout = string.IsNullOrEmpty(attr) ? 5000 : int.Parse(attr);
            attr = self.GetAttribute("LeaderHeartbeatTimer");
            LeaderHeartbeatTimer = string.IsNullOrEmpty(attr) ? 6000 : int.Parse(attr);
            attr = self.GetAttribute("LeaderLostTimeout");
            LeaderLostTimeout = string.IsNullOrEmpty(attr) ? 12000 : int.Parse(attr);
            attr = self.GetAttribute("MaxAppendEntiresCount");
            MaxAppendEntiresCount = string.IsNullOrEmpty(attr) ? 500 : int.Parse(attr);

            // check and reset params
            if (AppendEntriesTimeout < 1000)
                AppendEntriesTimeout = 1000;
            if (LeaderHeartbeatTimer < AppendEntriesTimeout + 1000)
                AppendEntriesTimeout = AppendEntriesTimeout + 1000;
            if (LeaderLostTimeout < AppendEntriesTimeout + LeaderHeartbeatTimer + 1000)
                LeaderLostTimeout = AppendEntriesTimeout + LeaderHeartbeatTimer + 1000;

            if (MaxAppendEntiresCount < 100)
                MaxAppendEntiresCount = 100;

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;
                switch (e.Name)
                {
                    case "node":
                        AddNode(new Node(e));
                        break;
                }
            }
        }

        internal void Save()
        {
            Self.SetAttribute("AppendEntriesTimeout", AppendEntriesTimeout.ToString());
            Self.SetAttribute("LeaderHeartbeatTimer", LeaderHeartbeatTimer.ToString());
            Self.SetAttribute("LeaderLostTimeout", LeaderLostTimeout.ToString());
            Self.SetAttribute("MaxAppendEntiresCount", MaxAppendEntiresCount.ToString());

            foreach (var node in Nodes)
            {
                node.Value.Save(XmlDocument, Self);
            }

            using (TextWriter sw = new StreamWriter(XmlFileName, false, Encoding.UTF8))
            {
                XmlDocument.Save(sw);
            }
        }

        public static RaftConfig Load(string xmlfile = "raft.xml")
        {
            if (File.Exists(xmlfile))
            {
                XmlDocument doc = new XmlDocument();
                doc.Load(xmlfile);
                return new RaftConfig(doc, xmlfile, doc.DocumentElement);
            }

            throw new Exception($"Raft.Config: '{xmlfile}' not exists.");
        }

        private void AddNode(Node node)
        {
            if (false == Nodes.TryAdd(node.Name, node))
                throw new Exception($"duplicate node '{node.Name}'");
        }

        public sealed class Node
        {
            public string Host { get; set; }
            public int Port { get; set; }
            public XmlElement Self { get; private set; }
            public string Name => $"{Host}:{Port}";

            public Node(XmlElement self)
            {
                Self = self;
                Host = self.GetAttribute("Host");
                Port = int.Parse(self.GetAttribute("Port"));
            }

            public Node(string host, int port)
            {
                Host = host;
                Port = port;
            }

            public void Save(XmlDocument doc, XmlElement parent)
            {
                if (null == Self)
                {
                    Self = doc.CreateElement("node");
                    parent.AppendChild(Self);
                }
                Self.SetAttribute("HostNameOrAddress", Host);
                Self.SetAttribute("Port", Port.ToString());
            }
        }

    }
}
