using System;
using System.Threading.Tasks;
using System.Xml;

namespace Zeze.Net
{
    /// <summary>
    /// 连接器：建立并保持一个连接，可以设置自动重连及相关参数。
    /// 可以继承并重载相关事件函数。重载实现里面需要调用 base.OnXXX。
    /// 继承是为了给链接扩充状态，比如：应用的连接需要login，可以维护额外的状态。
    /// 继承类启用方式：
    /// 1. 在配置中通过 class="FullClassName" 的。
    /// 2. 动态创建并加入Service
    /// </summary>
    public class Connector
    {
        public Service Service { get; private set; }

        public string HostNameOrAddress { get; }
        public int Port { get; } = 0;
        public bool IsAutoReconnect { get; set; } = true;
        private int _MaxReconnectDelay = 8000;
        public int MaxReconnectDelay
        {
            get
            {
                return _MaxReconnectDelay;
            }

            set
            {
                _MaxReconnectDelay = value;
                if (_MaxReconnectDelay < 1000)
                    _MaxReconnectDelay = 1000;
            }
        }
        public bool IsConnected { get; private set; } = false;
        private int ConnectDelay;
        public bool IsHandshakeDone => TryGetReadySocket() != null;
        private volatile TaskCompletionSource<AsyncSocket> FutureSocket = new TaskCompletionSource<AsyncSocket>();
        public string Name => $"{HostNameOrAddress}:{Port}";

        public AsyncSocket Socket { get; private set; }
        public Util.SchedulerTask ReconnectTask { get; private set; }
        public object UserState;

        public Connector(string host, int port = 0, bool autoReconnect = true)
        {
            HostNameOrAddress = host;
            Port = port;
            IsAutoReconnect = autoReconnect;
        }

        public static Connector Create(XmlElement e)
        {
            var className = e.GetAttribute("Class");
            return string.IsNullOrEmpty(className)
                    ? new Connector(e)
                    : (Connector)Activator.CreateInstance(Type.GetType(className), e);
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
        }

        internal void SetService(Service service)
        {
            lock (this)
            {
                if (Service != null)
                    throw new Exception($"Connector of '{Name}' Service != null");
                Service = service;
            }
        }

        // 允许子类重新定义Ready.
        public virtual AsyncSocket WaitReady()
        {
            return GetReadySocket();
        }

        public virtual AsyncSocket GetReadySocket()
        {
            var volatileTmp = FutureSocket;
            volatileTmp.Task.Wait();
            return volatileTmp.Task.Result;
        }

        public virtual AsyncSocket TryGetReadySocket()
        {
            var volatileTmp = FutureSocket;
            try
            {
                if (volatileTmp.Task.Wait(0))
                    return volatileTmp.Task.Result;
            }
            catch (Exception)
            {
                // skip
            }
            return null;
        }

        public virtual void OnSocketClose(AsyncSocket closed, Exception e)
        {
            lock (this)
            {
                if (Socket != closed)
                    return;
                Stop(e);
                TryReconnect();
            }
        }

        public virtual void OnSocketConnected(AsyncSocket so)
        {
            lock (this)
            {
                ConnectDelay = 0;
                IsConnected = true;
            }
        }

        internal static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public virtual void OnSocketHandshakeDone(AsyncSocket so)
        {
            lock (this)
            {
                if (FutureSocket.TrySetResult(so))
                    return;
            }
            // error recover?
            so.Close(new Exception("FutureSocket.SetResult Fail. Close New Socket."));
            Stop();
            Start();
        }

        public virtual void TryReconnect()
        {
            lock (this)
            {
                if (false == IsAutoReconnect
                    || null != Socket
                    || null != ReconnectTask)
                {
                    return;
                }

                if (ConnectDelay <= 0)
                {
                    ConnectDelay = 1000;
                }
                else
                {
                    ConnectDelay *= 2;
                    if (ConnectDelay > MaxReconnectDelay)
                        ConnectDelay = MaxReconnectDelay;
                }
                ReconnectTask = Util.Scheduler.Instance.Schedule((ThisTask) => Start(), ConnectDelay); ;
            }
        }

        public virtual void Start()
        {
            lock (this)
            {
                ReconnectTask?.Cancel();
                ReconnectTask = null;

                if (null != Socket)
                    return;

                Socket = Service.NewClientSocket(HostNameOrAddress, Port, UserState, this);
            }
        }

        public virtual void Stop(Exception e = null)
        {
            AsyncSocket tmp = null;
            lock (this)
            {
                if (null == Socket)
                    return; // not start or has stopped.

                IsConnected = false;
                FutureSocket.TrySetException(e ?? new Exception("Connector Stopped: " + Name));
                FutureSocket = new TaskCompletionSource<AsyncSocket>();
                tmp = Socket;
                Socket = null; // 阻止递归。
            }
            tmp?.Dispose();
        }

        public override string ToString()
        {
            return $"{Name}-{Socket}-{Socket?.Socket}";
        }
    }
}
