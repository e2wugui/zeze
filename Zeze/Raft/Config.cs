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
    public sealed class Config
    {
        private XmlDocument XmlDocument { get; }

        private string XmlFileName { get; }
        private XmlElement Self { get; }

        internal ConcurrentDictionary<string, Node> Nodes { get; }
            = new ConcurrentDictionary<string, Node>();

        // 【这个参数不保存】
        public string Name { get; internal set; }

        private Config(XmlDocument xml, string filename, XmlElement self)
        {
            XmlDocument = xml;
            XmlFileName = filename;
            Self = self;

            Name = self.GetAttribute("Name");

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
            // TODO other elements

            foreach (var node in Nodes)
            {
                node.Value.Save(XmlDocument, Self);
            }

            using (TextWriter sw = new StreamWriter(XmlFileName, false, Encoding.UTF8))
            {
                XmlDocument.Save(sw);
            }
        }

        public static Config Load(string xmlfile = "raft.xml")
        {
            if (File.Exists(xmlfile))
            {
                XmlDocument doc = new XmlDocument();
                doc.Load(xmlfile);
                return new Config(doc, xmlfile, doc.DocumentElement);
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
            public string HostNameOrAddress { get; set; }
            public int Port { get; set; }
            public XmlElement Self { get; private set; }
            public string Name => $"{HostNameOrAddress}:{Port}";

            public Node(XmlElement self)
            {
                Self = self;
                HostNameOrAddress = self.GetAttribute("HostNameOrAddress");
                Port = int.Parse(self.GetAttribute("Port"));
            }

            public Node(string host, int port)
            {
                HostNameOrAddress = host;
                Port = port;
            }

            public void Save(XmlDocument doc, XmlElement parent)
            {
                if (null == Self)
                {
                    Self = doc.CreateElement("node");
                    parent.AppendChild(Self);
                }
                Self.SetAttribute("HostNameOrAddress", HostNameOrAddress);
                Self.SetAttribute("Port", Port.ToString());
            }
        }

    }
}
