using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Threading.Tasks;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Util;

namespace Zeze.Net
{
    public class Service
    {
        protected static readonly ILogger logger = LogManager.GetLogger(typeof(Service));

        /// <summary>
        /// 同一个 Service 下的所有连接都是用相同配置。
        /// </summary>
        public SocketOptions SocketOptions { get; private set; } = new SocketOptions();

        public ServiceConf Config { get; private set; }
        public readonly Application Zeze;
        public string Name { get; }

        protected readonly ConcurrentDictionary<long, AsyncSocket> SocketMap
            = new ConcurrentDictionary<long, AsyncSocket>();

        internal ConcurrentDictionary<long, AsyncSocket> SocketMapInternal => SocketMap;

        public void AddSocket(AsyncSocket socket)
        {
            if (!SocketMap.TryAdd(socket.SessionId, socket))
                throw new Exception($"duplicate socket {socket}");
        }

        private void InitConfig(Config config)
        {
            Config = config?.GetServiceConf(Name);
            if (Config == null)
            {
                // setup program default
                Config = new ServiceConf();
                if (config != null)
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
            InitConfig(null);
        }

        /// <summary>
        /// 只包含成功建立的连接：服务器Accept和客户端Connected的连接。
        /// ServerSocket 保存在 Acceptor 中。这里查询不到。
        /// </summary>
        /// <param name="serialNo"></param>
        /// <returns></returns>
        public virtual AsyncSocket GetSocket(long serialNo)
        {
            return SocketMap.TryGetValue(serialNo, out var value) ? value : null;
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

        public AsyncSocket NewServerSocket(string ipaddress, int port, Acceptor acceptor)
        {
            return new TcpSocket(this, new IPEndPoint(IPAddress.Parse(ipaddress), port), acceptor);
        }

        public AsyncSocket NewServerSocket(IPAddress ipaddress, int port, Acceptor acceptor)
        {
            return new TcpSocket(this, new IPEndPoint(ipaddress, port), acceptor);
        }

        public AsyncSocket NewServerSocket(EndPoint localEP, Acceptor acceptor)
        {
            return new TcpSocket(this, localEP, acceptor);
        }

        public AsyncSocket NewClientSocket(string hostNameOrAddress, int port, object userState, Connector connector)
        {
            return new TcpSocket(this, hostNameOrAddress, port, userState, connector);
        }

        public AsyncSocket NewWebsocketClient(string wsUrl, object userState, Connector connector)
        {
            return new WebsocketClient(this, wsUrl, userState, connector);
        }

        /// <summary>
        /// ASocket 关闭的时候总是回调。
        /// </summary>
        /// <param name="so"></param>
        /// <param name="e"></param>
        public virtual void OnSocketClose(AsyncSocket so, Exception e)
        {
            SocketMap.TryRemove(new KeyValuePair<long, AsyncSocket>(so.SessionId, so));

            if (e != null)
            {
                logger.Log(SocketOptions.SocketLogLevel, e, "OnSocketClose");
            }
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
            var ctxPending = RemoveRpcContexts(ctxSends.Keys);
            foreach (var ctx in ctxRemoved)
            {
                // process
            }
            */
        }

        public Dictionary<long, Protocol> GetRpcContexts(Func<Protocol, bool> filter)
        {
            var result = new Dictionary<long, Protocol>();
            foreach (var ctx in RpcContexts)
            {
                if (filter(ctx.Value))
                {
                    result.Add(ctx.Key, ctx.Value);
                }
            }
            return result;
        }

        public ICollection<Protocol> RemoveRpcContexts(ICollection<long> sids)
        {
            var result = new List<Protocol>(sids.Count);
            foreach (var sid in sids)
            {
                var ctx = RemoveRpcContext<Protocol>(sid);
                if (ctx != null)
                    result.Add(ctx);
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
            SocketMap.TryRemove(new KeyValuePair<long, AsyncSocket>(so.SessionId, so));
            logger.Log(SocketOptions.SocketLogLevel, e, "OnSocketConnectError");
        }

        // ReSharper disable once UnusedParameter.Global
        public virtual void OnSocketAcceptError(AsyncSocket listener, Exception e)
        {
            logger.Log(SocketOptions.SocketLogLevel, e, "OnSocketAcceptError {0}", listener);
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
        public virtual void DispatchRpcResponse(Protocol rpc, Func<Protocol, Task<long>> responseHandle,
            ProtocolFactoryHandle factoryHandle)
        {
#if !USE_CONFCS
            if (Zeze != null && TransactionLevel.None != factoryHandle.TransactionLevel)
            {
                _ = Mission.CallAsync(Zeze.NewProcedure(async () => await responseHandle(rpc),
                    rpc.GetType().FullName + ":Response", factoryHandle.TransactionLevel, rpc.Sender?.UserState), rpc);
            }
            else
#endif
            {
                _ = Mission.CallAsync(responseHandle, rpc);
            }
        }

        public virtual void DispatchProtocol2(object key, Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            if (factoryHandle.Handle != null)
            {
#if !USE_CONFCS
                if (Zeze != null && TransactionLevel.None != factoryHandle.TransactionLevel)
                {
                    Zeze.TaskOneByOneByKey.Execute(key, Zeze.NewProcedure(
                            () => factoryHandle.Handle(p), p.GetType().FullName,
                            factoryHandle.TransactionLevel, p.Sender?.UserState),
                        p, (p2, code) => p2.TrySendResultCode(code));
                }
                else if (Zeze != null)
                    Zeze.TaskOneByOneByKey.Execute(key, factoryHandle.Handle, p, (p2, code) => p2.TrySendResultCode(code));
                else
#endif
                _ = Mission.CallAsync(factoryHandle.Handle, p, (p2, code) => p2.TrySendResultCode(code));
            }
            else
            {
                logger.Log(SocketOptions.SocketLogLevel, "Protocol Handle Not Found. {0}", p);
            }
        }

        public virtual bool IsHandshakeProtocol(long typeId)
        {
            return false;
        }

        public virtual async void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            if (factoryHandle.Handle != null)
            {
                if (IsHandshakeProtocol(p.TypeId))
                {
                    // handshake protocol call direct in io-thread.
                    await Mission.CallAsync(factoryHandle.Handle, p);
                }
#if !USE_CONFCS
                else if (Zeze != null && TransactionLevel.None != factoryHandle.TransactionLevel)
                {
                    _ = Mission.CallAsync(Zeze.NewProcedure(() => factoryHandle.Handle(p),
                        p.GetType().FullName, factoryHandle.TransactionLevel, p.Sender?.UserState), p);
                }
#endif
                else
                {
                    _ = Mission.CallAsync(factoryHandle.Handle, p);
                }
            }
            else
            {
                logger.Log(SocketOptions.SocketLogLevel, "Protocol Handle Not Found. {0}", p);
            }
        }

        public virtual void DispatchUnknownProtocol(AsyncSocket so, int moduleId, int protocolId, ByteBuffer data)
        {
            throw new Exception($"Unknown Protocol=({moduleId}, {protocolId}) size={data.Size}");
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////
        /// 协议工厂
        public class ProtocolFactoryHandle
        {
            public Func<Protocol> Factory;
            public Func<Protocol, Task<long>> Handle;
            public TransactionLevel TransactionLevel = TransactionLevel.Serializable;
            public bool NoProcedure => TransactionLevel == TransactionLevel.None;

            // 协议请求的派发（线程）模式。【警告，现在这个参数同时用于服务器和客户端，如果协议收发都需要处理时，无法支持两种派发模式】
            public DispatchMode Mode = DispatchMode.Normal;

            /////////////////////////////////////////////////////////////////////////////////////////////
            // 由于c#只有一个线程池，所以默认情况下，没有使用线程派发模式，需要的应用重载下面的方法，自行使用Mode配置。
            // DispatchProtocol,DispatchProtocol2,DispatchRpcResponse

            // 收到的协议计数
            public readonly AtomicLong RecvCount = new AtomicLong();
            private volatile ProtocolPool _ProtocolPool;

            public ProtocolPool ProtocolPool => _ProtocolPool;

            public void SetupProtocolPool(ProtocolPool.ReuseLevel level)
            {
                lock (this)
                {
                    if (_ProtocolPool == null)
                    {
                        var tmp = new ProtocolPool(Handle, level);
                        Handle = tmp.Process; // 先设置，拦截处理。
                        _ProtocolPool = tmp;
                    }
                }
            }

            public void RemoveProtocolPool()
            {
                lock (this)
                {
                    _ProtocolPool = null;
                }
            }
        }

        public readonly ConcurrentDictionary<long, ProtocolFactoryHandle> Factorys
            = new ConcurrentDictionary<long, ProtocolFactoryHandle>();

        public void AddFactoryHandle(long type, ProtocolFactoryHandle factory)
        {
            if (!Factorys.TryAdd(type, factory))
            {
                Factorys.TryGetValue(type, out var exist);
                // ReSharper disable once PossibleNullReferenceException
                var existType = exist.Factory().GetType();
                throw new Exception($"duplicate factory type={type} moduleId={(type >> 32) & 0x7fff} id={type & 0x7fff} exist={existType}");
            }
        }

        /* target: 静态方法可以传null */
        /*
        public static Func<Protocol, int> MakeHandle<T>(object target , MethodInfo method) where T : Protocol
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
        */

        public ProtocolFactoryHandle FindProtocolFactoryHandle(long type)
        {
            return Factorys.TryGetValue(type, out ProtocolFactoryHandle factory) ? factory : null;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////
        /// Rpc Context. 模板不好放进去，使用基类 Protocol
        private static readonly AtomicLong StaticSessionIdAtomicLong = new AtomicLong();

        // ReSharper disable once UnassignedField.Global
        public Func<long> SessionIdGenerator;

        private readonly ConcurrentDictionary<long, Protocol> RpcContextsPrivate =
            new ConcurrentDictionary<long, Protocol>();

        public IReadOnlyDictionary<long, Protocol> RpcContexts => RpcContextsPrivate;

        public long NextSessionId()
        {
            return SessionIdGenerator?.Invoke() ?? StaticSessionIdAtomicLong.IncrementAndGet();
        }

        internal long AddRpcContext(Protocol p)
        {
            while (true)
            {
                long sessionId = NextSessionId();
                if (RpcContextsPrivate.TryAdd(sessionId, p))
                    return sessionId;
            }
        }

        internal void TryRemoveRpcContext(long sid, Protocol current)
        {
            RpcContextsPrivate.TryRemove(new KeyValuePair<long, Protocol>(sid, current));
        }

        internal T RemoveRpcContext<T>(long sid) where T : Protocol
        {
            return RpcContextsPrivate.TryRemove(sid, out var p) ? (T)p : null;
        }

        public abstract class ManualContext
        {
            public long SessionId { get; internal set; }
            public object UserState;
            public Service Service;
            public bool IsTimeout { get; internal set; }

            public virtual void OnRemoved()
            {
            }
        }

        private readonly ConcurrentDictionary<long, ManualContext> ManualContexts =
            new ConcurrentDictionary<long, ManualContext>();

        public long AddManualContextWithTimeout(ManualContext context, long timeout = 10 * 1000)
        {
            while (true)
            {
                long sessionId = NextSessionId();
                if (ManualContexts.TryAdd(sessionId, context))
                {
                    context.SessionId = sessionId;
                    context.Service = this;
                    Scheduler.Schedule(_ => TryRemoveManualContext<ManualContext>(sessionId, true), timeout);
                    return sessionId;
                }
            }
        }

        public T TryGetManualContext<T>(long sessionId) where T : ManualContext
        {
            return ManualContexts.TryGetValue(sessionId, out var c) ? (T)c : null;
        }

        public T TryRemoveManualContext<T>(long sessionId) where T : ManualContext
        {
            return TryRemoveManualContext<T>(sessionId, false);
        }

        private T TryRemoveManualContext<T>(long sessionId, bool isTimeout) where T : ManualContext
        {
            if (ManualContexts.TryRemove(sessionId, out var c))
            {
                c.IsTimeout = isTimeout;
                c.OnRemoved();
                return (T)c;
            }
            return null;
        }

        // 还是不直接暴露内部的容器。提供这个方法给外面用。以后如果有问题，可以改这里。
        public void Foreach(Action<AsyncSocket> action)
        {
            foreach (var socket in SocketMap.Values)
                action(socket);
        }

        public static string GetOneNetworkInterfaceIpAddress(AddressFamily family = AddressFamily.Unspecified)
        {
            foreach (NetworkInterface ni in NetworkInterface.GetAllNetworkInterfaces())
            {
                if (ni.NetworkInterfaceType == NetworkInterfaceType.Loopback)
                    continue;

                IPInterfaceProperties property = ni.GetIPProperties();
                foreach (UnicastIPAddressInformation ip in property.UnicastAddresses)
                {
                    switch (ip.Address.AddressFamily)
                    {
                        case AddressFamily.InterNetworkV6:
                        case AddressFamily.InterNetwork:
                            if (family == AddressFamily.Unspecified || family == ip.Address.AddressFamily)
                                return ip.Address.ToString();
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

            Config.ForEachAcceptor(a =>
            {
                if (!string.IsNullOrEmpty(a.Ip) && a.Port != 0)
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

        // ReSharper disable UnusedParameter.Global
        public virtual bool CheckThrottle(AsyncSocket sender, int moduleId, int protocolId, int size)
        {
#if !USE_CONFCS
            var throttle = sender.TimeThrottle;
            if (throttle != null && !throttle.CheckNow(size))
            {
                // TrySendResultCode(ResultCode.Busy); // 超过速度限制，不报告错误。因为可能是一种攻击。
                sender.Dispose();
                return false; // 超过速度控制，丢弃这条协议。
            }
#endif
            return true;
        }

        public virtual bool Discard(AsyncSocket sender, int moduleId, int protocolId, int size)
        {
            return false;
        }
        // ReSharper restore UnusedParameter.Global

        private SchedulerTask keepCheckTimer;

        public void TryStartKeepAliveCheckTimer()
        {
            lock (this)
            {
                if (keepCheckTimer == null)
                {
                    var period = Config.HandshakeOptions.KeepCheckPeriod * 1000L;
                    if (period > 0)
                    {
                        keepCheckTimer = Scheduler.Schedule(CheckKeepAlive, Util.Random.Instance.Next((int)period) + 1, period);
                    }
                }
            }
        }

        private void CheckKeepAlive(SchedulerTask This)
        {
            var conf = Config.HandshakeOptions;
            var keepRecvTimeout = conf.KeepRecvTimeout > 0 ? conf.KeepRecvTimeout * 1000L : long.MaxValue;
            var keepSendTimeout = conf.KeepSendTimeout > 0 ? conf.KeepSendTimeout * 1000L : long.MaxValue;
            var now = Time.NowUnixMillis; // 使用毫秒，System.nanoTime c# 不知道怎么对应，查了一下说 StopWatch？
            foreach (var socket in SocketMap.Values)
            {
                var recvTime = now - socket.ActiveRecvTime;
                if (recvTime > keepRecvTimeout)
                {
                    try
                    {
                        OnKeepAliveTimeout(socket);
                    }
                    catch (Exception e)
                    {
                        logger.Error(e, "onKeepAliveTimeout exception:");
                    }
                }
                if (socket.Type == AsyncSocketType.eClient && // 上次接收时间超过SendTimeout也要发起KeepAlive,通过RPC回复更新上次接收时间
                    (now - socket.ActiveSendTime > keepSendTimeout || recvTime > keepSendTimeout))
                {
                    try
                    {
                        OnSendKeepAlive(socket);
                    }
                    catch (Exception e)
                    {
                        logger.Error(e, "onSendKeepAlive exception:");
                    }
                }
            }
        }

        protected virtual void OnKeepAliveTimeout(AsyncSocket socket)
        {
            logger.Log(SocketOptions.SocketLogLevel, "socket keep alive timeout: {0}", socket);
            socket.Close(null);
        }

        ///
        /// 1. 如果你是handshake的service，重载这个方法，按注释发送CKeepAlive即可【默认已实现，不需要操作了】；
        /// 2. 如果你是其他service子类，重载这个方法，按注释发送CKeepAlive，并且服务器端需要注册这条协议并写一个不需要处理代码的handler；
        /// 3. 如果不发送, 会导致KeepTimerClient时间后再次触发, 也可以调用socket.setActiveSendTime()避免频繁触发。
        protected virtual void OnSendKeepAlive(AsyncSocket socket)
        {
            Services.Handshake.KeepAlive.Instance.Send(socket); // skip result.
        }
    }
}
