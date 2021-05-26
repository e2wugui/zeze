using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Xml;

namespace Zeze.Net
{
    public class Acceptor
    {
        public int Port { get; } = 0;
        public string Ip { get; } = string.Empty;
        public AsyncSocket Socket { get; private set; }
        public string Name => $"{Ip}:{Port}";

        public Acceptor(int port, string ip)
        {
            Port = port;
            Ip = ip;
        }

        public Acceptor(XmlElement self)
        {
            string attr = self.GetAttribute("Port");
            if (attr.Length > 0)
                Port = int.Parse(attr);
            Ip = self.GetAttribute("Ip");
        }

        public void Start(Service service)
        {
            Socket?.Dispose();
            Socket = Ip.Length > 0
                ? service.NewServerSocket(Ip, Port)
                : service.NewServerSocket(System.Net.IPAddress.Any, Port);
            Socket.Acceptor = this;
        }

        public void Stop(Service service)
        {
            Socket?.Dispose();
            Socket = null;
        }
    }
}
