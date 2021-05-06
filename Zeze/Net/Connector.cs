using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Xml;

namespace Zeze.Net
{
    public class Connector
    {
        public string HostNameOrAddress { get; }
        public int Port { get; } = 0;
        public bool IsAutoReconnect { get; } = true;
        public int MaxReconnectDelay { get; set; }
        public bool IsConnected { get; private set; } = false;
        private int ConnectDelay;
        public bool IsHandshakeDone => Socket != null && Socket.IsHandshakeDone;
        public string Name => $"{HostNameOrAddress}:{Port}";

        public AsyncSocket Socket { get; private set; }

        public Connector(string host, int port = 0, bool autoReconnect = true)
        {
            HostNameOrAddress = host;
            Port = port;
            IsAutoReconnect = autoReconnect;
        }

        public Connector(XmlElement self)
        {
            string attr = self.GetAttribute("Port");
            if (attr.Length > 0)
                Port = int.Parse(attr);
            HostNameOrAddress = self.GetAttribute("HostNameOrAddress");
            attr = self.GetAttribute("IsAutoReconnect");
            if (attr.Length > 0)
                IsAutoReconnect = bool.Parse(attr);
            attr = self.GetAttribute("MaxReconnectDelay");
            if (attr.Length > 0)
                MaxReconnectDelay = int.Parse(attr) * 1000;
            if (MaxReconnectDelay < 8000)
                MaxReconnectDelay = 8000;
        }

        public void OnSocketClose(AsyncSocket closed)
        {
            if (Socket != closed)
                return;

            Socket = null;

            if (false == IsAutoReconnect)
                return;

            if (ConnectDelay == 0)
            {
                ConnectDelay = 2000;
            }
            else
            {
                ConnectDelay *= 2;
                if (ConnectDelay > MaxReconnectDelay)
                    ConnectDelay = MaxReconnectDelay;
            }
            Util.Scheduler.Instance.Schedule((ThisTask) => Start(closed.Service), ConnectDelay); ;
        }

        public void OnSocketConnected(AsyncSocket so)
        {
            ConnectDelay = 0;
            IsConnected = true;
        }

        public void Start(Service service)
        {
            Socket?.Dispose();
            IsConnected = false;
            Socket = service.NewClientSocket(HostNameOrAddress, Port);
            Socket.Connector = this;
        }

        public void Stop(Service service)
        {
            Socket?.Dispose();
            Socket = null;
        }
    }
}
