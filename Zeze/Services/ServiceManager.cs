using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading.Tasks;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;

namespace Zeze.Services
{
    public sealed class ServiceManager
    {
        //////////////////////////////////////////////////////////////////////////////
        /// 动态服务列表同步更新。
        /// 【名词】
        /// 动态服务器(gs)
        ///     动态服务器一般指启用cache-sync的逻辑服务器。比如gs。
        /// 注册服务器（ServiceManager）
        ///     支持更新服务器，这个服务一开始是为了启用cache-sync的服务器的查找。
        /// 动态服务器列表使用者(linkd)
        ///     当前使用动态服务的客户端主要是Game2/linkd，linkd在hash分配请求的时候需要一致动态服务器列表。
        ///
        /// 【下面的流程都是用现有的服务名字（上面括号中的名字）】
        /// 
        /// 【本控制功能的目标】
        /// 所有的linkd的可用动态服务列表的更新并不是原子的。
        /// 1. 让所有的linkd的列表保持最新；
        /// 2. 尽可能减少linkd上的服务列表不一致的时间（通过ready-commit机制）；
        /// 3. 列表不一致时，分发请求可能引起cache不命中，但不影响正确性（cache-sync保证了正确性）；
        /// 
        /// 【主要事件和流程】
        /// 1. gs停止时调用 RegisterService,UnRegisterService 向ServiceManager声明自己服务状态。
        /// 2. linkd启动时调用 UseService, UnUseService 向ServiceManager申请使用gs-list。
        /// 3. ServiceManager在RegisterService,UnRegisterService处理时发送 NotifyServiceList 给所有的 linkd。
        /// 4. linkd收到NotifyServiceList先记录到本地，同时持续关注自己和gs之间的连接，
        ///    当列表中的所有serivce都准备完成时调用 ReadyServiceList。
        /// 5. ServiceManager收到所有的linkd的ReadyServiceList后，向所有的linkd广播 CommitServiceList。
        /// 6. linkd 收到 CommitServiceList 时，启用新的服务列表。
        /// 
        /// 【特别规则和错误处理】
        /// 1. linkd 异常停止，ServiceManager 按 UnUseService 处理，仅仅简单移除use-list。相当于减少了以后请求来源。
        /// 2. gs 异常停止，ServiceManager 按 UnRegisterService 处理，移除可用服务，并启动列表更新流程（NotifyServiceList）。
        /// 3. linkd 处理 gs 关闭（在NotifyServiceList之前），仅仅更新本地服务列表状态，让该服务暂时不可用，但不改变列表。
        ///    linkd总是使用ServiceManager提交给他的服务列表，自己不主动增删。
        ///    linkd在NotifyServiceList的列表减少的处理：一般总是立即进入ready（因为其他gs都是可用状态）。
        /// 4. ServiceManager 异常关闭：
        ///    a) 启用raft以后，新的master会有正确列表数据，但服务状态（连接）未知，此时等待gs的RegisterService一段时间,
        ///       然后开启新的一轮NotifyServiceList，等待时间内没有再次注册的gs以后当作新的处理。
        ///    b) 启用raft的好处是raft的非master服务器会识别这种状态，并重定向请求到master，使得系统内只有一个master启用服务。
        ///       实际上raft不需要维护相同数据状态（gs-list），从空的开始即可，启用raft的话仅使用他的选举功能。
        /// 5. ServiceManager开启一轮变更通告过程中，有新的gs启动停止，将开启新的通告(NotifyServiceList)。
        ///    ReadyServiceList时会检查ready中的列表是否和当前ServiceManagerlist一致，不一致直接忽略。
        ///    新的通告流程会促使linkd继续发送ready。
        ///    另外为了更健壮的处理通告，通告加一个超时机制。超时没有全部ready，就启动一次新的通告。
        ///    原则是：总按最新的gs-list通告。中间不一致的ready全部忽略。

        // ServiceInfo.Name -> ServiceState
        private ConcurrentDictionary<string, ServiceState> States = new ConcurrentDictionary<string, ServiceState>();
        public NetServer Server { get; private set; }
        public sealed class ServiceState
        {
            public ConcurrentDictionary<string, ServiceInfo> ServiceInfos { get; }
                = new ConcurrentDictionary<string, ServiceInfo>();
            public ConcurrentDictionary<long, ServiceUser> SimpleUsers { get; }
                = new ConcurrentDictionary<long, ServiceUser>();
            public ConcurrentDictionary<long, ServiceUser> ReadyCommitUsers { get; }
                = new ConcurrentDictionary<long, ServiceUser>();
        }

        public sealed class ServiceUser
        {
            public long SessionId { get; set; }
            public bool Ready { get; set; } // ReadyCommit时才被使用。
        }

        private ServiceManager()
        {

        }

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public static ServiceManager Instance { get; }
            = new ServiceManager();

        private int ProcessRegisterService(Protocol p)
        {
            var r = p as Register;
            r.ResultCode = 0;
            r.SendResult();
            return Procedure.Success;
        }

        private int ProcessUnRegisterService(Protocol p)
        {
            var r = p as UnRegister;
            r.ResultCode = 0;
            r.SendResult();
            return Procedure.Success;
        }

        private int ProcessUseService(Protocol p)
        {
            var r = p as Subscribe;
            r.ResultCode = 0;
            r.SendResult();
            return Procedure.Success;
        }

        private int ProcessUnUseService(Protocol p)
        {
            var r = p as UnSubscribe;
            r.ResultCode = 0;
            r.SendResult();
            return Procedure.Success;
        }

        private int ProcessReadyServiceList(Protocol p)
        {
            var r = p as ReadyServiceList;
            r.ResultCode = 0;
            return Procedure.Success;
        }

        public void Start(IPAddress ipaddress, int port, Config config = null)
        {
            lock (this)
            {
                if (Server != null)
                    return;
                if (null == config)
                    config = Config.Load();
                Server = new NetServer(config);

                Server.AddFactoryHandle(new Register().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new Register(),
                    Handle = ProcessRegisterService,
                });

                Server.AddFactoryHandle(new UnRegister().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new UnRegister(),
                    Handle = ProcessUnRegisterService,
                });

                Server.AddFactoryHandle(new Subscribe().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new Subscribe(),
                    Handle = ProcessUseService,
                });

                Server.AddFactoryHandle(new UnSubscribe().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new UnSubscribe(),
                    Handle = ProcessUnUseService,
                });

                Server.AddFactoryHandle(new ReadyServiceList().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new ReadyServiceList(),
                    Handle = ProcessReadyServiceList,
                });
            }
        }

        public sealed class NetServer : Net.Service
        {
            public NetServer(Config config) : base("RService", config)
            {
            }
        }

        public sealed class ServiceInfo : Zeze.Transaction.Bean
        {
            /// <summary>
            /// 服务名，比如"GameServer"
            /// </summary>
            public string Name { get; private set; }

            /// <summary>
            /// 服务id，对于 Zeze.Application，一般就是 Config.AutoKeyLocalId.
            /// 这里使用类型 string 是为了更好的支持扩展。
            /// </summary>
            public string Identity { get; private set; }

            /// <summary>
            /// 服务ip-port，如果没有，保持空和0.
            /// </summary>
            public string PassiveIp { get; private set; } = "";
            public int PassivePort { get; private set; } = 0;

            // 服务扩展信息，可选。
            private Dictionary<int, string> _ExtraInfo { get; }
                = new Dictionary<int, string>();

            public IReadOnlyDictionary<int, string> ExtraInfo => _ExtraInfo;

            public ServiceInfo()
            { 
            }

            public ServiceInfo(
                string name, string identity,
                string ip, int port,
                Dictionary<int, string> extrainfo)
            {
                Name = name;
                Identity = identity;
                PassiveIp = ip;
                PassivePort = port;
                _ExtraInfo = extrainfo;
            }

            public override void Decode(ByteBuffer bb)
            {
                Name = bb.ReadString();
                Identity = bb.ReadString();
                PassiveIp = bb.ReadString();
                PassivePort = bb.ReadInt();
                for (int c = bb.ReadInt(); c > 0; --c)
                {
                    var extraKey = bb.ReadInt();
                    var extraValue = bb.ReadString();
                    _ExtraInfo[extraKey] = extraValue;
                }
            }

            public override void Encode(ByteBuffer bb)
            {
                bb.WriteString(Name);
                bb.WriteString(Identity);
                bb.WriteString(PassiveIp);
                bb.WriteInt(PassivePort);
                bb.WriteInt(ExtraInfo.Count);
                foreach (var e in ExtraInfo)
                {
                    bb.WriteInt(e.Key);
                    bb.WriteString(e.Value);
                }
            }

            protected override void InitChildrenRootInfo(Record.RootInfo root)
            {
                throw new NotImplementedException();
            }
        }

        /// <summary>
        /// 动态服务启动时通过这个rpc注册自己。
        /// </summary>
        public sealed class Register : Rpc<ServiceInfo, EmptyBean>
        {
            public override int ModuleId => 0;
            public override int ProtocolId => 100;

        }

        /// <summary>
        /// 动态服务关闭时，注销自己，当与本服务器的连接关闭时，默认也会注销。
        /// 最好主动注销，方便以后错误处理。
        /// </summary>
        public sealed class UnRegister : Rpc<ServiceInfo, EmptyBean>
        {
            public override int ModuleId => 0;
            public override int ProtocolId => 101;
        }

        public sealed class SubscribeInfo : Bean
        {
            public const int SubscribeTypeSimple = 0;
            public const int SubscribeTypeReadyCommit = 1;

            public string ServiceName { get; set; }
            public int SubscribeType { get; set; }

            public override void Decode(ByteBuffer bb)
            {
                ServiceName = bb.ReadString();
                SubscribeType = bb.ReadInt();
            }

            public override void Encode(ByteBuffer bb)
            {
                bb.WriteString(ServiceName);
                bb.WriteInt(SubscribeType);
            }

            protected override void InitChildrenRootInfo(Record.RootInfo root)
            {
                throw new NotImplementedException();
            }
        }
        public sealed class Subscribe : Rpc<SubscribeInfo, EmptyBean>
        {
            public override int ModuleId => 0;
            public override int ProtocolId => 102;
        }

        public sealed class UnSubscribe : Rpc<SubscribeInfo, EmptyBean>
        {
            public override int ModuleId => 0;
            public override int ProtocolId => 103;
        }

        public sealed class ServiceInfos : Bean
        {
            // ServiceList maybe empty. need a ServiceName
            public string ServiceName { get; private set; }
            private Dictionary<string, ServiceInfo> _Services { get; }
                = new Dictionary<string, ServiceInfo>();
            public IReadOnlyDictionary<string, ServiceInfo> Services => _Services;

            public ServiceInfos()
            { 
            }

            public ServiceInfos(string serviceName)
            {
                ServiceName = serviceName;
            }

            public override void Decode(ByteBuffer bb)
            {
                ServiceName = bb.ReadString();
                _Services.Clear();
                for (int c = bb.ReadInt(); c > 0; --c)
                {
                    var service = new ServiceInfo();
                    service.Decode(bb);
                    _Services.Add(service.Identity, service);
                }
            }

            public override void Encode(ByteBuffer bb)
            {
                bb.WriteString(ServiceName);
                bb.WriteInt(Services.Count);
                foreach (var service in Services.Values)
                {
                    service.Encode(bb);
                }
            }

            protected override void InitChildrenRootInfo(Record.RootInfo root)
            {
                throw new NotImplementedException();
            }
        }

        public sealed class NotifyServiceList : Protocol<ServiceInfos>
        {
            public override int ModuleId => 0;
            public override int ProtocolId => 104;
        }

        public sealed class ReadyServiceList : Protocol<ServiceInfos>
        {
            public override int ModuleId => 0;
            public override int ProtocolId => 105;
        }

        public sealed class CommitServiceList : Protocol<ServiceInfos>
        {
            public override int ModuleId => 0;
            public override int ProtocolId => 106;
        }

        public sealed class Agent
        {
            public ConcurrentDictionary<string, ServiceState> ServiceStates { get; }
                = new ConcurrentDictionary<string, ServiceState>();

            public NetClient Client { get; private set; }
            public Action RegisterAndSubscribe { get; private set; }
            public Action<ServiceState> OnServiceInfosCommit { get; private set; }
            public static Agent Instance { get; } = new Agent();

            public sealed class ServiceState
            {
                public int SubscribeType { get; }

                public ServiceInfos ServiceInfos { get; private set; }
                public ServiceInfos ServiceInfosPending { get; private set; }

                // 服务准备好。
                public ConcurrentDictionary<string, object> ServiceReadyStates { get; }
                    = new ConcurrentDictionary<string, object>();

                public ServiceState(string serviceName, int subscribeType)
                {
                    SubscribeType = subscribeType;
                    ServiceInfos = new ServiceInfos(serviceName);
                }

                // NOT UNDER LOCK
                private bool TrySendReadyServiceList()
                {
                    foreach (var pending in ServiceInfosPending.Services.Values)
                    {
                        if (!ServiceReadyStates.ContainsKey(pending.Identity))
                            return false;
                    }
                    var r = new ReadyServiceList() { Argument = ServiceInfosPending };
                    Agent.Instance.Client.Socket?.Send(r);
                    return true;
                }

                public void SetServiceReadyState(string identity, object state)
                {
                    ServiceReadyStates[identity] = state;
                    lock (this)
                    {
                        TrySendReadyServiceList();
                    }
                }

                internal void OnNotify(ServiceInfos infos)
                {
                    lock (this)
                    {
                        switch (SubscribeType)
                        {
                            case SubscribeInfo.SubscribeTypeSimple:
                                ServiceInfos = infos;
                                Agent.Instance.OnServiceInfosCommit(this);
                                break;

                            case SubscribeInfo.SubscribeTypeReadyCommit:
                                ServiceInfosPending = infos;
                                TrySendReadyServiceList();
                                break;
                        }
                    }
                }

                internal void OnCommit(ServiceInfos infos)
                {
                    lock (this)
                    {
                        // ServiceInfosPending 和 Commit.infos 应该一样，否则肯定哪里出错了。
                        // 这里总是使用最新的 Commit.infos，检查记录日志。
                        bool error = !Enumerable.SequenceEqual(infos.Services.Values, ServiceInfosPending.Services.Values);
                        ServiceInfos = infos;
                        ServiceInfosPending = null;
                        if (error)
                        {
                            Agent.logger.Error("OnCommit: ServiceInfosPending Miss Match.");
                        }
                        Agent.Instance.OnServiceInfosCommit(this);
                    }
                }
            }

            private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

            public void RegisterService(ServiceInfo info)
            {
                var future = new TaskCompletionSource<int>();
                var r = new Register() { Argument = info };
                r.Send(Client.Socket, (_)=>
                {
                    if (r.IsTimeout)
                    {
                        future.TrySetException(new Exception("RService.Client.RegisterService Timeout")); ;
                    }
                    else if (r.ResultCode != 0)
                    {
                        future.TrySetException(new Exception($"RService.Client.RegisterService ResultCode={r.ResultCode}")); ;
                    }
                    else
                    {
                        future.SetResult(0);
                    }
                    return Procedure.Success;
                });
                future.Task.Wait();
            }

            public void UnRegisterService(ServiceInfo info)
            {
                var future = new TaskCompletionSource<int>();
                var r = new UnRegister() { Argument = info };
                r.Send(Client.Socket, (_) =>
                {
                    // log only
                    if (r.IsTimeout)
                    {
                        logger.Error($"RService.Client.UnRegisterService Timeout Info={info}");
                    }
                    else if (r.ResultCode != 0)
                    {
                        logger.Error($"RService.Client.UnRegisterService ResultCode={r.ResultCode} Info={info}");
                    }
                    future.SetResult(0);
                    return Procedure.Success;
                });
                future.Task.Wait();
            }

            public void SubscribeService(SubscribeInfo info)
            {
                if (!ServiceStates.TryAdd(info.ServiceName, new ServiceState(info.ServiceName, info.SubscribeType)))
                {
                    throw new Exception("SubscribeService Duplicate.");
                }
                var future = new TaskCompletionSource<int>();
                var r = new Subscribe() { Argument = info };
                r.Send(Client.Socket, (_) =>
                {
                    if (r.IsTimeout)
                    {
                        future.TrySetException(new Exception($"RService.Client.UseService Timeout Info={info}"));
                    }
                    else if (r.ResultCode != 0)
                    {
                        future.TrySetException(new Exception($"RService.Client.UseService ResultCode={r.ResultCode} Info={info}"));
                    }
                    future.SetResult(0);
                    return Procedure.Success;
                });
                future.Task.Wait();
            }

            public void UnSubscribeService(SubscribeInfo info)
            {
                if (!ServiceStates.TryRemove(info.ServiceName, out var state))
                    return;

                if (info.SubscribeType != state.SubscribeType)
                    throw new Exception("UnSubscribeService SubscribeType Not Equals");

                var future = new TaskCompletionSource<int>();
                var r = new UnSubscribe() { Argument = info };
                r.Send(Client.Socket, (_) =>
                {
                    // log only
                    if (r.IsTimeout)
                    {
                        logger.Error($"RService.Client.UnUseService Timeout Info={info}");
                    }
                    else if (r.ResultCode != 0)
                    {
                        logger.Error($"RService.Client.UnUseService ResultCode={r.ResultCode} Info={info}");
                    }
                    future.SetResult(0);
                    return Procedure.Success;
                });
                future.Task.Wait();
            }

            private int ProcessNotifyServiceList(Protocol p)
            {
                var r = p as NotifyServiceList;
                if (ServiceStates.TryGetValue(r.Argument.ServiceName, out var state))
                {
                    state.OnNotify(r.Argument);
                }
                else
                {
                    Agent.logger.Warn("NotifyServiceList But Service Not Found.");
                }
                return Procedure.Success;
            }

            private int ProcessCommitServiceList(Protocol p)
            {
                var r = p as CommitServiceList;
                if (ServiceStates.TryGetValue(r.Argument.ServiceName, out var state))
                {
                    state.OnCommit(r.Argument);
                }
                else
                {
                    Agent.logger.Warn("CommitServiceList But Service Not Found.");
                }
                return Procedure.Success;
            }

            public void Dispose()
            {
                lock(this)
                {
                    if (null == Client)
                        return;
                    Client.Close();
                    Client = null;
                }
            }

            private Agent()
            {

            }

            /// <summary>
            /// 使用Config配置连接信息，可以配置是否支持重连。
            /// 使用配置启动网络 Agent.Client.Start()
            /// 不使用配置启动网络 Agent.Client.NewClientSocket(...)，不会自动重连。
            /// </summary>
            /// <param name="config"></param>
            public void Start(Config config,
                Action RegisterAndSubscribeWhenConnected,
                Action<ServiceState> ServiceInfosCommit)
            {
                lock (this)
                {
                    if (null != Client)
                        return;
                    if (null == config)
                        throw new Exception("Config is null");

                    Client = new NetClient(config);

                    RegisterAndSubscribe = RegisterAndSubscribeWhenConnected;
                    OnServiceInfosCommit = ServiceInfosCommit;

                    Client.AddFactoryHandle(new Register().TypeId, new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new Register(),
                    });

                    Client.AddFactoryHandle(new UnRegister().TypeId, new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new UnRegister(),
                    });

                    Client.AddFactoryHandle(new Subscribe().TypeId, new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new Subscribe(),
                    });

                    Client.AddFactoryHandle(new UnSubscribe().TypeId, new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new UnSubscribe(),
                    });

                    Client.AddFactoryHandle(new NotifyServiceList().TypeId, new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new NotifyServiceList(),
                        Handle = ProcessNotifyServiceList,
                    });

                    Client.AddFactoryHandle(new CommitServiceList().TypeId, new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new CommitServiceList(),
                        Handle = ProcessCommitServiceList,
                    });
                }
            }

            public sealed class NetClient : Net.Service
            {
                /// <summary>
                /// 和注册服务器之间只保持一个连接。并且不处理任何协议状态。
                /// </summary>
                public AsyncSocket Socket { get; private set; }

                public NetClient(Config config) : base("RService.Client", config)
                {
                }

                public override void OnHandshakeDone(AsyncSocket sender)
                {
                    base.OnHandshakeDone(sender);
                    if (null == Socket)
                    {
                        Socket = sender;
                        Agent.Instance.RegisterAndSubscribe();
                    }
                    else
                    {
                        Agent.logger.Error("Has Connected.");
                    }
                }

                public override void OnSocketClose(AsyncSocket so, Exception e)
                {
                    base.OnSocketClose(so, e);
                    if (Socket == so)
                        Socket = null;
                }
            }
        }
    }
}
