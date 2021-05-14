using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Xml;

namespace Zeze.Net
{
    public sealed class ServiceConf
    {
        public string Name { get; }
        public Net.SocketOptions SocketOptions { get; set; } = new Net.SocketOptions();
        public Services.HandshakeOptions HandshakeOptions { get; set; } = new Services.HandshakeOptions();

        private List<Acceptor> Acceptors { get; } = new List<Acceptor>();
        private List<Connector> Connectors { get; } = new List<Connector>();

        public void AddConnector(Connector connector)
        {
            lock (Connectors)
            {
                Connectors.Add(connector);
            }
        }

        public Connector FindConnector(string host, int port)
        {
            lock (Connectors)
            {
                foreach (var c in Connectors)
                {
                    if (c.HostNameOrAddress.Equals(host) && c.Port.Equals(port))
                        return c;
                }
                return null;
            }
        }

        /// <summary>
        /// 查找，不存在则创建。
        /// </summary>
        /// <param name="host"></param>
        /// <param name="port"></param>
        /// <param name="autoReconnect"></param>
        /// <param name="getOrAdd"></param>
        /// <returns>true if addNew</returns>
        public bool TryGetOrAddConnector(string host, int port, bool autoReconnect, out Connector getOrAdd)
        {
            lock (Connectors)
            {
                getOrAdd = FindConnector(host, port);
                if (null != getOrAdd)
                    return false;
                getOrAdd = new Connector(host, port, autoReconnect);
                Connectors.Add(getOrAdd);
                return true;
            }
        }

        public void RemoveConnector(Connector c)
        {
            lock (Connectors)
            {
                Connectors.Remove(c);
            }
        }

        public void ForEachConnector(Action<Connector> action)
        {
            lock (Connectors)
            {
                Connectors.ForEach(action);
            }
        }

        public int ConnectorCount()
        {
            lock (Connectors)
            {
                return Connectors.Count;
            }
        }

        public bool ForEachConnector(Func<Connector, bool> func)
        {
            lock (Connectors)
            {
                foreach (var c in Connectors)
                {
                    if (false == func(c))
                        return false;
                }
                return true;
            }
        }

        public void AddAcceptor(Acceptor a)
        {
            lock (Acceptors)
            {
                Acceptors.Add(a);
            }
        }

        public void RemoveAcceptor(Acceptor a)
        {
            lock (Acceptors)
            {
                Acceptors.Remove(a);
            }
        }

        public void ForEachAcceptor(Action<Acceptor> action)
        {
            lock (Acceptors)
            {
                Acceptors.ForEach(action);
            }
        }

        public bool ForEachAcceptor(Func<Acceptor, bool> func)
        {
            lock (Acceptors)
            {
                foreach (var a in Acceptors)
                {
                    if (false == func(a))
                        return false;
                }
                return true;
            }
        }

        public int AcceptorCount()
        { 
            lock (Acceptors)
            {
                return Acceptors.Count;
            }
        }

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
            if (attr.Length > 0) SocketOptions.SendBuffer = int.Parse(attr);
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
            attr = self.GetAttribute("DhGroups");
            if (attr.Length > 0)
            {
                HandshakeOptions.DhGroups = new HashSet<int>();
                foreach (string dg in attr.Split(','))
                {
                    string dgtmp = dg.Trim();
                    if (dgtmp.Length == 0)
                        continue;
                    HandshakeOptions.AddDhGroup(int.Parse(dgtmp));
                }
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
            {
                if (!conf.ServiceConfMap.TryAdd(Name, this))
                {
                    throw new Exception($"Duplicate ServiceConf '{Name}'");
                }
            }
            else
            {
                conf.DefaultServiceConf = this;
            }

            // connection creator options
            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;
                switch (e.Name)
                {
                    case "Acceptor": AddAcceptor(new Acceptor(e)); break;
                    case "Connector": AddConnector(new Connector(e)); break;
                    default: throw new Exception("unknown node name: " + e.Name);
                }
            }
        }

        public void Start(Service service)
        {
            ForEachAcceptor((a) => a.Start(service));
            ForEachConnector((c) => c.Start(service));
        }

        public void Stop(Service service)
        {
            ForEachAcceptor((a) => a.Stop(service));
            ForEachConnector((c) => c.Stop(service));
        }

        public void StopListen(Service service)
        {
            ForEachAcceptor((a) => a.Stop(service));
        }
    }
}
