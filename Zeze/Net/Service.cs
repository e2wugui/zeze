using System;
using System.Collections.Generic;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;
using System.Collections.Concurrent;
using Zeze.Transaction;
using System.Net;
using System.Net.Sockets;
using System.Net.NetworkInformation;

namespace Zeze.Net
{
    public class Service
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        /// <summary>
        /// 同一个 Service 下的所有连接都是用相同配置。
        /// </summary>
        public SocketOptions SocketOptions { get; private set; } = new SocketOptions();
        public ServiceConf Config { get; private set; }
        public Application Zeze { get; }
        public string Name { get; }

        protected ConcurrentDictionary<long, AsyncSocket> SocketMap { get; }
            = new ConcurrentDictionary<long, AsyncSocket>();

        internal ConcurrentDictionary<long, AsyncSocket> SocketMapInternal => SocketMap;

        private void InitConfig(Config config)
        {
            Config = config?.GetServiceConf(Name);
            if (null == Config)
            {
                // setup program default
                Config = new ServiceConf();
                if (null != config)
                {
                    // reference to config default
                    Config.SocketOptions = config.DefaultServiceConf.SocketOptions;
                    Config.HandshakeOptions = config.DefaultServiceConf.HandshakeOptions;
                }
            }
            Config.SetService(this);
            SocketOptions = Config.SocketOptions;
        }

        public Service(string name, Config config)
        {
            Name = name;
            InitConfig(config);
        }

        public Service(string name, Application app)
        {
            Name = name;
            Zeze = app;
            InitConfig(app?.Config);
        }

        public Service(string name)
        {
            Name = name;
        }

        /// <summary>
        /// 只包含成功建立的连接：服务器Accept和客户端Connected的连接。
        /// ServerSocket 保存在 Acceptor 中。这里查询不到。
        /// </summary>
        /// <param name="serialNo"></param>
        /// <returns></returns>
        public virtual AsyncSocket GetSocket(long serialNo)
        {
            if (SocketMap.TryGetValue(serialNo, out var value))
                return value;
            return null;
        }

        public virtual AsyncSocket GetSocket()
        {
            foreach (var e in SocketMap)
            {
                return e.Value;
            }
            return null;
        }

        public virtual void Start()
        {
            Config?.Start();
        }

        public virtual void Stop()
        {
            Config?.Stop();

            foreach (var e in SocketMap)
            {
                e.Value.Dispose(); // remove in callback OnSocketClose
            }

            // 先不清除，让Rpc的TimerTask仍然在超时以后触发回调。
            // 【考虑一下】也许在服务停止时马上触发回调并且清除上下文比较好。
            // 【注意】直接清除会导致同步等待的操作无法继续。异步只会没有回调，没问题。
            // _RpcContexts.Clear();
        }

        public AsyncSocket NewServerSocket(string ipaddress, int port)
        {
            return NewServerSocket(IPAddress.Parse(ipaddress), port);
        }

        public AsyncSocket NewServerSocket(IPAddress ipaddress, int port)
        {
            return NewServerSocket(new IPEndPoint(ipaddress, port));
        }

        public AsyncSocket NewServerSocket(EndPoint localEP)
        {
            return new AsyncSocket(this, localEP);
        }

        public AsyncSocket NewClientSocket(string hostNameOrAddress, int port, object userState = null)
        {
            return new AsyncSocket(this, hostNameOrAddress, port, userState);
        }

        /// <summary>
        /// ASocket 关闭的时候总是回调。
        /// </summary>
        /// <param name="so"></param>
        /// <param name="e"></param>
        public virtual void OnSocketClose(AsyncSocket so, Exception e)
        {
            SocketMap.TryRemove(KeyValuePair.Create(so.SessionId, so));

            if (null != e)
                logger.Log(SocketOptions.SocketLogLevel, e, "OnSocketClose");
        }

        /// <summary>
        /// 可靠rpc调用：一般用于重新发送没有返回结果的rpc。
        /// 在 OnSocketClose 之后调用，此时外面【必须】拿不到此 AsyncSocket 了。
        /// 当 OnSocketDisposed 调用发生时，AsyncSocket.Socket已经设为 null。
        /// 对于那些在 AsyncSocket.Dispose 时已经得到的 AsyncSocket 引用，
        /// 使用时判断返回值：主要是 Send 返回 false。
        /// </summary>
        /// <param name="so"></param>
        public virtual void OnSocketDisposed(AsyncSocket so)
        {
            // 一般实现：遍历RpcContexts，
            /*
            var ctxSends = GetRpcContextsToSender(so);
            var ctxPending = RemoveRpcContets(ctxSends.Keys);
            foreach (var ctx in ctxRemoved)
            {
                // process
            }
            */
        }

        // Not Need Now
        public Dictionary<long, Protocol> GetRpcContextsToSender(AsyncSocket sender)
        {
            return GetRpcContexts((p) => p.Sender == sender);
        }

        public Dictionary<long, Protocol> GetRpcContexts(Func<Protocol, bool> filter)
        {
            var result = new Dictionary<long, Protocol>(RpcContexts.Count);
            foreach (var ctx in RpcContexts)
            {
                if (filter(ctx.Value))
                {
                    result.Add(ctx.Key, ctx.Value);
                }
            }
            return result;
        }

        public ICollection<Protocol> RemoveRpcContets(ICollection<long> sids)
        {
            var result = new List<Protocol>(sids.Count);
            foreach (var sid in sids)
            {
                var ctx = this.RemoveRpcContext<Protocol>(sid);
                if (null != ctx)
                {
                    result.Add(ctx);
                }
            }
            return result;
        }

        /// <summary>
        /// 服务器接受到新连接回调。
        /// </summary>
        /// <param name="so"></param>
        public virtual void OnSocketAccept(AsyncSocket so)
        {
            SocketMap.TryAdd(so.SessionId, so);
            OnHandshakeDone(so);
        }

        /// <summary>
        /// 连接完成建立调用。
        /// 未加密压缩的连接在 OnSocketAccept OnSocketConnected 里面调用这个方法。
        /// 加密压缩的连接在相应的方法中调用（see Services\Handshake.cs）。
        /// 注意：修改OnHandshakeDone的时机，需要重载OnSocketAccept OnSocketConnected，并且不再调用Service的默认实现。
        /// </summary>
        public virtual void OnHandshakeDone(AsyncSocket sender)
        {
            sender.IsHandshakeDone = true;
            sender.Connector?.OnSocketHandshakeDone(sender);
        }

        /// <summary>
        /// 连接失败回调。同时也会回调OnSocketClose。
        /// </summary>
        /// <param name="so"></param>
        /// <param name="e"></param>
        public virtual void OnSocketConnectError(AsyncSocket so, Exception e)
        {
            SocketMap.TryRemove(KeyValuePair.Create(so.SessionId, so));
            logger.Log(SocketOptions.SocketLogLevel, e, "OnSocketConnectError");
        }

        /// <summary>
        /// 连接成功回调。
        /// </summary>
        /// <param name="so"></param>
        public virtual void OnSocketConnected(AsyncSocket so)
        {
            SocketMap.TryAdd(so.SessionId, so);
            OnHandshakeDone(so);
        }

        /// <summary>
        /// 处理数据。
        /// 在异步线程中回调，要注意线程安全。
        /// </summary>
        /// <param name="so"></param>
        /// <param name="input"></param>
        public virtual void OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input)
        {
            Protocol.Decode(this, so, input);
        }

        // 用来派发异步rpc回调。
        public virtual void DispatchRpcResponse(Protocol rpc,
            Func<Protocol, int> responseHandle,
            ProtocolFactoryHandle factoryHandle)
        {
            if (null != Zeze && false == factoryHandle.NoProcedure)
            {
                global::Zeze.Util.Task.Run(
                    Zeze.NewProcedure(
                        () => responseHandle(rpc),
                        rpc.GetType().FullName + ":Response",
                        rpc.UserState));
            }
            else
            {
                global::Zeze.Util.Task.Run(() => responseHandle(rpc), rpc);
            }
        }

        public void DispatchProtocol2(object key, Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            if (null != factoryHandle.Handle)
            {
                if (null != Zeze && false == factoryHandle.NoProcedure)
                {
                    Zeze.TaskOneByOneByKey.Execute(key,
                        () => global::Zeze.Util.Task.Call(Zeze.NewProcedure(
                            () => factoryHandle.Handle(p), p.GetType().FullName, p.UserState),
                            p,
                            (p, code) => p.SendResultCode(code))
                        );
                }
                else
                {
                    Zeze.TaskOneByOneByKey.Execute(key,
                        () => global::Zeze.Util.Task.Call(
                            () => factoryHandle.Handle(p),
                            p,
                            (p, code) => p.SendResultCode(code))
                        );
                }
            }
            else
            {
                logger.Log(SocketOptions.SocketLogLevel, "Protocol Handle Not Found. {0}", p);
            }
        }

        public virtual void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            if (null != factoryHandle.Handle)
            {
                if (null != Zeze && false == factoryHandle.NoProcedure)
                {
                    global::Zeze.Util.Task.Run(
                        Zeze.NewProcedure(
                            () => factoryHandle.Handle(p),
                            p.GetType().FullName,
                            p.UserState), p);
                }
                else
                {
                    global::Zeze.Util.Task.Run(() => factoryHandle.Handle(p), p);
                }
            }
            else
            {
                logger.Log(SocketOptions.SocketLogLevel, "Protocol Handle Not Found. {0}", p);
            }
        }

        public virtual void DispatchUnknownProtocol(AsyncSocket so, int type, ByteBuffer data)
        {
            throw new Exception("Unknown Protocol (" + (type >> 16 & 0xffff) + ", " + (type & 0xffff) + ") size=" + data.Size);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////
        /// 协议工厂
        public class ProtocolFactoryHandle
        { 
            public Func<Protocol> Factory { get; set; }
            public Func<Protocol, int> Handle { get; set; }
            public bool NoProcedure { get; set; } = false;
        }

        public ConcurrentDictionary<int, ProtocolFactoryHandle> Factorys { get; } = new ConcurrentDictionary<int, ProtocolFactoryHandle>();

        public void AddFactoryHandle(int type, ProtocolFactoryHandle factory)
        {
            if (false == Factorys.TryAdd(type, factory))
                throw new Exception($"duplicate factory type={type} moduleid={(type >> 16) & 0x7fff} id={type & 0x7fff}");
        }

        public static Func<Protocol, int> MakeHandle<T>(object target /*静态方法可以传null*/, MethodInfo method) where T : Protocol
        {
            return (Protocol p) =>
            {
                if (method.IsStatic)
                {
                    var handler = Delegate.CreateDelegate(typeof(Func<T, int>), method);
                    return ((Func<T, int>)handler)((T)p);
                }
                else 
                {
                    var handler = Delegate.CreateDelegate(typeof(Func<T, int>), target, method);
                    return ((Func<T, int>)handler)((T)p);
                }
            };
        }

        public ProtocolFactoryHandle FindProtocolFactoryHandle(int type)
        {
            if (Factorys.TryGetValue(type, out ProtocolFactoryHandle factory))
            {
                return factory;
            }

            return null;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////
        /// Rpc Context. 模板不好放进去，使用基类 Protocol
        private static Util.AtomicLong StaticSessionIdAtomicLong { get; } = new Util.AtomicLong();
        public Func<long> SessionIdGenerator { get; set; }

        private readonly ConcurrentDictionary<long, Protocol> _RpcContexts
            = new ConcurrentDictionary<long, Protocol>();
        public IReadOnlyDictionary<long, Protocol> RpcContexts => _RpcContexts;

        public long NextSessionId()
        {
            if (null != SessionIdGenerator)
                return SessionIdGenerator();
            return StaticSessionIdAtomicLong.IncrementAndGet();
        }

        internal long AddRpcContext(Protocol p)
        {
            while (true)
            {
                long sessionId = NextSessionId();
                if (_RpcContexts.TryAdd(sessionId, p))
                {
                    return sessionId;
                }
            }
        }

        internal T RemoveRpcContext<T>(long sid) where T : Protocol
        {
            if (_RpcContexts.TryRemove(sid, out var p))
            {
                return (T)p;
            }
            return null;
        }

        public abstract class ManualContext
        {
            public long SessionId { get; internal set; }
            public object UserState { get; set; }

            public virtual void OnRemoved()
            {
            }

            // after OnRemoved if Timeout
            public virtual void OnTimeout()
            {
            }

        }

        private readonly ConcurrentDictionary<long, ManualContext> ManualContexts = new ConcurrentDictionary<long, ManualContext>();

        public long AddManualContextWithTimeout(ManualContext context, long timeout = 10*1000)
        {
            while (true)
            {
                long sessionId = NextSessionId();
                if (ManualContexts.TryAdd(sessionId, context))
                {
                    context.SessionId = sessionId;
                    Util.Scheduler.Instance.Schedule(
                        (ThisTask) => TryRemoveManualContext<ManualContext>(sessionId)?.OnTimeout(),
                        timeout);
                    return sessionId;
                }
            }
        }

        public T TryGetManualContext<T>(long sessionId) where T : ManualContext
        {
            if (ManualContexts.TryGetValue(sessionId, out var c))
                return (T)c;
            return null;
        }

        public T TryRemoveManualContext<T>(long sessionId) where T : ManualContext
        {
            if (ManualContexts.TryRemove(sessionId, out var c))
            {
                c.OnRemoved();
                return (T)c;
            }
            return null;
        }

        // 还是不直接暴露内部的容器。提供这个方法给外面用。以后如果有问题，可以改这里。
        public void Foreach(Action<AsyncSocket> action)
        {
            foreach (var socket in SocketMap.Values)
            {
                action(socket);
            }
        }

        public string GetOneNetworkInterfaceIpAddress(AddressFamily family = AddressFamily.Unspecified)
        {
            foreach (NetworkInterface neti in NetworkInterface.GetAllNetworkInterfaces())
            {
                if (neti.NetworkInterfaceType == NetworkInterfaceType.Loopback)
                    continue;

                IPInterfaceProperties property = neti.GetIPProperties();
                foreach (UnicastIPAddressInformation ip in property.UnicastAddresses)
                {
                    switch (ip.Address.AddressFamily)
                    {
                        case AddressFamily.InterNetworkV6:
                        case AddressFamily.InterNetwork:
                            if (family == AddressFamily.Unspecified
                                || family == ip.Address.AddressFamily)
                            {
                                return ip.Address.ToString();
                            }
                            continue;
                    }
                }
            }
            return null;
        }

        public (string, int) GetOneAcceptorAddress()
        {
            string ip = string.Empty;
            int port = 0;

            Config.ForEachAcceptor(
                (a) =>
                {
                    if (false == string.IsNullOrEmpty(a.Ip) && a.Port != 0)
                    {
                        // 找到ip，port都配置成明确地址的。
                        ip = a.Ip;
                        port = a.Port;
                        return false;
                    }
                    // 获得最后一个配置的port。允许返回(null, port)。
                    port = a.Port;
                    return true;
                });

            return (ip, port);
        }

        public (string, int) GetOnePassiveAddress()
        {
            var (ip, port) = GetOneAcceptorAddress();
            if (port == 0)
                throw new Exception("Acceptor: No Config.");

            if (string.IsNullOrEmpty(ip))
            {
                // 可能绑定在任意地址上。尝试获得网卡的地址。
                ip = GetOneNetworkInterfaceIpAddress();
                if (string.IsNullOrEmpty(ip))
                {
                    // 实在找不到ip地址，就设置成loopback。
                    logger.Warn("PassiveAddress No Config. set ip to 127.0.0.1");
                    ip = "127.0.0.1";
                }
            }
            return (ip, port);
        }
    }
}
