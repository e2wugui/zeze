
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.IO;
using System.Linq;
using System.Net;
using System.Text;
using System.Xml;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Services.ServiceManager;
using Zeze.Util;
using System.Threading.Tasks;
using System.Xml.Linq;
using System.Collections;
using DotNext.Threading;
using System.Threading;
using DotNext.Collections.Generic;

namespace Zeze.Services.ServiceManager
{
    public sealed class Agent : IDisposable
    {
        public const string SMCallbackOneByOneKey = "SMCallbackOneByOneKey";

        // key is ServiceName。对于一个Agent，一个服务只能有一个订阅。
        // ServiceName ->
        public ConcurrentDictionary<string, SubscribeState> SubscribeStates { get; } = new();
        public NetClient Client { get; private set; }
        public Application Zeze { get; }

        /// <summary>
        /// 订阅服务状态发生变化时回调。
        /// 如果需要处理这个事件，请在订阅前设置回调。
        /// </summary>
        public Action<BEditService> OnChanged { get; set; }
        public Action<ServerLoad> OnSetServerLoad { get; set; }
        public Func<BOfflineNotify, bool> OnOfflineNotify { get; set; }

        // 应用可以在这个Action内起一个测试事务并执行一次。也可以实现其他检测。
        // ServiceManager 定时发送KeepAlive给Agent，并等待结果。超时则认为服务失效。
        public Action OnKeepAlive { get; set; }

        // key is (ServiceName, ServideIdentity)
        private ConcurrentDictionary<ServiceInfo, ServiceInfo> Registers { get; } = new(new ServiceInfoEqualityComparer());

        // 【警告】
        // 记住当前已经注册和订阅信息，当ServiceManager连接发生重连时，重新发送请求。
        // 维护这些状态数据都是先更新本地再发送远程请求，在失败的时候rollback。
        // 当同一个Key(比如ServiceName)存在并发时，现在处理所有情况，但不保证都是合理的。
        public sealed class SubscribeState
        {
            public Agent Agent { get; }
            public SubscribeInfo SubscribeInfo { get; }
            public string ServiceName => SubscribeInfo.ServiceName;
            public ServiceInfosVersion ServiceInfosVersion { get; private set; }
            //public ServiceInfos ServiceInfos => ServiceInfosVersion.InfosVersion[0]; // 兼容

            public override string ToString()
            {
                return ServiceInfosVersion.ToString();
            }

            // 服务准备好。
            public ConcurrentDictionary<string, object> LocalStates { get; } = new();

            public SubscribeState(Agent ag, SubscribeInfo info)
            {
                Agent = ag;
                SubscribeInfo = info;
                ServiceInfosVersion = new ServiceInfosVersion();
            }

            public void SetIdentityLocalState(string identity, object state)
            {
                if (null == state)
                    LocalStates.TryRemove(identity, out var _);
                else
                    LocalStates[identity] = state;
            }

            internal ServiceInfo OnRegister(ServiceInfo info)
            {
                lock (this)
                {
                    if (ServiceInfosVersion.InfosVersion.TryGetValue(info.Version, out var versions))
                    {
                        var exist = versions.Insert(info);
                        return null != exist && !exist.FullEquals(info) ? exist : null;
                    }
                    return null;
                }
            }

            internal bool OnUnRegister(ServiceInfo info)
            {
                lock (this)
                {
                    if (false == ServiceInfosVersion.InfosVersion.TryGetValue(info.Version, out var versions))
                        return false;
                    return null != versions.Remove(info);
                }
            }

            internal void OnFirstCommit(ServiceInfosVersion infos)
            {
                var edits = new List<BEditService>();
                lock (this)
                {
                    ServiceInfosVersion = infos;
                    foreach (var identityMap in infos.InfosVersion.Values)
                    {
                        var edit = new BEditService();
                        foreach (var info in identityMap.SortedIdentity)
                            edit.Add.Add(info);
                        edits.Add(edit);
                    }
                }
                foreach (var edit in edits)
                    Agent.TriggerOnChanged(edit);
            }
        }

        private static readonly ILogger logger = LogManager.GetLogger(typeof(Agent));

        public async Task<ServiceInfo> RegisterService(
            string name, string identity, long version,
            string ip = null, int port = 0,
            Binary extrainfo = null)
        {
            return await RegisterService(new ServiceInfo(name, identity, version, ip, port, extrainfo));
        }

        public async Task WaitConnectorReadyAsync()
        {
            // 实际上只有一个连接，这样就不用查找了。
            await Client.Config.ForEachConnectorAsync(async (c) => await c.GetReadySocketAsync());
        }

        public static void Verify(string identity)
        {
            if (!identity.StartsWith("@") && !identity.StartsWith("#"))
                long.Parse(identity);
        }

        public async Task<BEditService> EditService(BEditService arg)
        {
            foreach (var info in arg.Add)
                Verify(info.ServiceIdentity);

            await WaitConnectorReadyAsync();

            var edit = new EditService() { Argument = arg };
            await edit.SendAndCheckResultCodeAsync(Client.Socket);

            // 成功以后更新本地信息。
            foreach (var unReg in arg.Remove)
                Registers.Remove(unReg, out _);

            foreach (var reg in arg.Add)
                Registers[reg] = reg;

            return arg;
        }

        public async Task<ServiceInfo> RegisterService(ServiceInfo info)
        {
            var edit = new BEditService();
            edit.Add.Add(info);
            await EditService(edit);
            return info;
        }

        public async Task UnRegisterService(string name, string identity)
        {
            await UnRegisterService(new ServiceInfo(name, identity, 0));
        }

        private async Task UnRegisterService(ServiceInfo info)
        {
            var edit = new BEditService();
            edit.Remove.Add(info);
            await EditService(edit);
        }

        public async Task<SubscribeState> SubscribeService(string serviceName, object state = null)
        {
            return await SubscribeService(new SubscribeInfo()
            {
                ServiceName = serviceName,
                LocalState = state
            });
        }

        public async Task<SubscribeState> SubscribeService(SubscribeInfo info)
        {
            var infos = new SubscribeArgument();
            infos.Subs.Add(info);
            var states = await SubscribeServices(infos);
            return states[0];
        }

        public async Task<List<SubscribeState>> SubscribeServices(SubscribeArgument infos)
        {
            await WaitConnectorReadyAsync();

            var r = new Subscribe() { Argument = infos };
            await r.SendAsync(Client.Socket);

            var states = new List<SubscribeState>();
            foreach (var info in r.Argument.Subs)
            {
                var state = SubscribeStates.GetOrAdd(info.ServiceName, _ => new SubscribeState(this, info));
                states.Add(state);
                if (r.Result.Map.TryGetValue(info.ServiceName, out var result))
                    state.OnFirstCommit(result);
            }
            return states;
        }

        public bool SetServerLoad(ServerLoad load)
        {
            var p = new SetServerLoad
            {
                Argument = load
            };
            return p.Send(Client?.Socket);
        }

        public async Task OfflineRegisterAsync(BOfflineNotify arg)
        {
            await WaitConnectorReadyAsync();
            _ = new OfflineRegister(arg).SendAndCheckResultCodeAsync(Client.GetSocket());
        }

        public async Task UnSubscribeService(string serviceName)
        {
            await WaitConnectorReadyAsync();

            var arg = new UnSubscribeArgument();
            arg.ServiceNames.Add(serviceName);
            await UnSubscribeService(arg);
        }

        public async Task UnSubscribeService(UnSubscribeArgument arg)
        {
            await WaitConnectorReadyAsync();

            var r = new UnSubscribe() { Argument = arg };
            await r.SendAndCheckResultCodeAsync(Client.Socket);

            foreach (var serviceName in arg.ServiceNames)
                SubscribeStates.Remove(serviceName, out _);
        }

        private Task<long> ProcessKeepAlive(Protocol p)
        {
            var r = p as KeepAlive;
            OnKeepAlive?.Invoke();
            r.SendResultCode(KeepAlive.Success);
            return Task.FromResult(ResultCode.Success);
        }

        public sealed class AutoKey
        {
            public string Name { get; }
            public long Current { get; private set; }
            public int Count { get; private set; }
            public Agent Agent { get; }
            private AsyncLock Mutex { get; } = new();

            internal AutoKey(string name, Agent agent)
            {
                Name = name;
                Agent = agent;
            }

            public async Task<long> NextAsync()
            {
                using (await Mutex.AcquireAsync(CancellationToken.None))
                {
                    if (Count <= 0)
                        await Allocate();

                    if (Count <= 0)
                        throw new Exception($"AllocateId failed for {Name}");

                    var tmp = Current;
                    --Count;
                    ++Current;
                    return tmp;
                }
            }

            private async Task Allocate()
            {
                var r = new AllocateId();
                r.Argument.Name = Name;
                r.Argument.Count = 1024;
                await r.SendAndCheckResultCodeAsync(Agent.Client.Socket);
                Current = r.Result.StartId;
                Count = r.Result.Count;
            }
        }

        private ConcurrentDictionary<string, AutoKey> AutoKeys { get; } = new();

        public AutoKey GetAutoKey(string name)
        {
            return AutoKeys.GetOrAdd(name, (k) => new AutoKey(k, this));
        }

        public void Stop()
        {
            lock (this)
            {
                if (null == Client)
                    return;
                Client.Stop();
                Client = null;
            }
        }

        internal async Task OnConnected()
        {
            var edit = new BEditService();
            edit.Add.AddAll(Registers.Keys);
            await EditService(edit);

            var sub = new SubscribeArgument();
            foreach (var e in SubscribeStates)
                sub.Subs.Add(e.Value.SubscribeInfo);

            await SubscribeServices(sub);
        }

        private Task<long> ProcessEditService(Protocol _p)
        {
            var r = _p as EditService;
            var removing = new List<ServiceInfo>();
            foreach (var unReg in r.Argument.Remove)
            {
                if (SubscribeStates.TryGetValue(unReg.ServiceName, out var state) && !state.OnUnRegister(unReg))
                    removing.Add(unReg);
            }
            r.Argument.Remove.RemoveAll(r => removing.Contains(r));

            // 触发回调前修正集合之间的关系。
            // 删除后来又加入的。
            r.Argument.Remove.RemoveAll(o => r.Argument.Add.Contains(o));

            foreach (var reg in r.Argument.Add)
            {
                if (SubscribeStates.TryGetValue(reg.ServiceName, out var state))
                {
                    var oldNotSame = state.OnRegister(reg);
                    if (null != oldNotSame)
                        r.Argument.Remove.Add(oldNotSame);
                }
            }

            r.SendResult();

            TriggerOnChanged(r.Argument);

            return Task.FromResult(ResultCode.Success);
        }

        void TriggerOnChanged(BEditService edit)
        {
            if (OnChanged != null)
            {
                TaskOneByOneByKey.Instance.Execute(SMCallbackOneByOneKey, () => OnChanged(edit));
            }
        }

        /// <summary>
        /// 使用Config配置连接信息，可以配置是否支持重连。
        /// 用于测试：Agent.Client.NewClientSocket(...)，不会自动重连，不要和Config混用。
        /// </summary>
        public Agent(Application zeze, string netServiceName = null)
        {
            Zeze = zeze;
            var config = zeze.Config;
            if (null == config)
                throw new Exception("Config is null");

            Client = string.IsNullOrEmpty(netServiceName)
                ? new NetClient(this, config) : new NetClient(this, config, netServiceName);

            Client.AddFactoryHandle(new EditService().TypeId, new Service.ProtocolFactoryHandle()
            {
                Factory = () => new EditService(),
                Handle = ProcessEditService,
            });

            Client.AddFactoryHandle(new Subscribe().TypeId, new Service.ProtocolFactoryHandle()
            {
                Factory = () => new Subscribe(),
            });

            Client.AddFactoryHandle(new UnSubscribe().TypeId, new Service.ProtocolFactoryHandle()
            {
                Factory = () => new UnSubscribe(),
            });

            Client.AddFactoryHandle(new KeepAlive().TypeId, new Service.ProtocolFactoryHandle()
            {
                Factory = () => new KeepAlive(),
                Handle = ProcessKeepAlive,
            });

            Client.AddFactoryHandle(new AllocateId().TypeId, new Service.ProtocolFactoryHandle()
            {
                Factory = () => new AllocateId(),
            });

            Client.AddFactoryHandle(new SetServerLoad().TypeId, new Service.ProtocolFactoryHandle()
            {
                Factory = () => new SetServerLoad(),
                Handle = ProcessSetServerLoad,
            });

            Client.AddFactoryHandle(new OfflineNotify().TypeId, new Service.ProtocolFactoryHandle()
            {
                Factory = () => new OfflineNotify(),
                Handle = ProcessOfflineNotify,
            });

            Client.AddFactoryHandle(new OfflineRegister().TypeId, new Service.ProtocolFactoryHandle()
            {
                Factory = () => new OfflineRegister(),
            });
        }

        public readonly ConcurrentDictionary<string, ServerLoad> Loads = new();

        private Task<long> ProcessSetServerLoad(Protocol _p)
        {
            var p = _p as SetServerLoad;
            Loads[p.Argument.Name] = p.Argument;
            OnSetServerLoad?.Invoke(p.Argument);
            return Task.FromResult(ResultCode.Success);
        }

        private Task<long> ProcessOfflineNotify(Protocol _p)
        {
            var p = _p as OfflineNotify;
            if (OnOfflineNotify == null)
            {
                p.TrySendResultCode(1);
                return Task.FromResult(ResultCode.Success); // TODO: maybe need to use ResultCode.Exception
            }
            try
            {
                if (OnOfflineNotify(p.Argument))
                {
                    p.SendResult();
                    return Task.FromResult(ResultCode.Success);
                }
                p.TrySendResultCode(2);
            }
            catch(Exception)
            {
                p.TrySendResultCode(3);
            }
            return Task.FromResult(ResultCode.Success);
        }

        public bool SetLoad(ServerLoad load)
        {
            var p = new SetServerLoad
            {
                Argument = load
            };
            return p.Send(Client.GetSocket());
        }

        public void Dispose()
        {
            try
            {
                Stop();
            }
            catch (Exception ex)
            {
                logger.Error(ex);
            }
        }

        public const string DefaultServiceName = "Zeze.Services.ServiceManager.Agent";

        public sealed class NetClient : HandshakeClient
        {
            public Agent Agent { get; }
            /// <summary>
            /// 和注册服务器之间只保持一个连接。并且不处理任何协议状态。
            /// </summary>
            public AsyncSocket Socket { get; private set; }

            public NetClient(Agent agent, Config config)
                : base("Zeze.Services.ServiceManager.Agent", config)
            {
                Agent = agent;
            }
            public NetClient(Agent agent, Config config, string name) : base(name, config)
            {
                Agent = agent;
            }

            public override void OnHandshakeDone(AsyncSocket sender)
            {
                base.OnHandshakeDone(sender);
                if (null == Socket)
                {
                    Socket = sender;
                    _ = Agent.OnConnected();
                }
                else
                {
                    Agent.logger.Error("Has Connected.");
                }
            }

            public override void OnSocketClose(AsyncSocket so, Exception e)
            {
                if (Socket == so)
                    Socket = null;
                base.OnSocketClose(so, e);
            }

            public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
            {
                _ = Mission.CallAsync(factoryHandle.Handle, p, (_, code) => p.TrySendResultCode(code));
            }

        }
    }

    public class BOfflineNotify : Bean
    {
        public int ServerId { get; set; }
        public string NotifyId { get; set; }
        public long NotifySerialId { get; set; } // context 如果够用就直接用这个，
        public byte[] NotifyContext { get; set; } // context 扩展context。

        public override void ClearParameters()
        {
            ServerId = 0;
            NotifyId = null;
            NotifySerialId = 0;
            NotifyContext = null;
        }

        public BOfflineNotify() { }

        public BOfflineNotify(int serverId, string notifyId)
        {
            ServerId = serverId;
            NotifyId = notifyId;
            NotifySerialId = (long)Convert.ToDouble(NotifyId);
            {
                var bb = ByteBuffer.Allocate();
                bb.WriteString(NotifyId);
                NotifyContext = bb.Copy();
            }
        }

        public override void Decode(ByteBuffer bb)
        {
            ServerId = bb.ReadInt();
            NotifyId = bb.ReadString();
            NotifySerialId = bb.ReadLong();
            NotifyContext = bb.ReadBytes();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteInt(ServerId);
            bb.WriteString((string)NotifyId);
            bb.WriteLong(NotifySerialId);
            bb.WriteBytes(NotifyContext);
        }
    }

    public class OfflineNotify : Rpc<BOfflineNotify, EmptyBean>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(OfflineNotify).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;

        public OfflineNotify()
        {
            Argument = new BOfflineNotify();
            Result = EmptyBean.Instance;
        }

        public OfflineNotify(BOfflineNotify bOfflineNotify)
        {
            Argument = bOfflineNotify;
            Result = EmptyBean.Instance;
        }
    }

    public class OfflineRegister : Rpc<BOfflineNotify, EmptyBean>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(OfflineRegister).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;

        public OfflineRegister()
        {
            Argument = new BOfflineNotify();
            Result = EmptyBean.Instance;
        }

        public OfflineRegister(BOfflineNotify bOfflineNotify)
        {
            Argument = bOfflineNotify;
            Result = EmptyBean.Instance;
        }
    }

    public sealed class ServerLoad : Bean
    {
        public string Ip { get; set; }
        public int Port { get; set; }
        public Binary Param { get; set; } = Binary.Empty;

        public string Name => Ip + ":" + Port;

        public override void Decode(ByteBuffer bb)
        {
            Ip = bb.ReadString();
            Port = bb.ReadInt();
            Param = bb.ReadBinary();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteString(Ip);
            bb.WriteInt(Port);
            bb.WriteBinary(Param);
        }

        public override string ToString()
        {
            return $"Ip={Ip} Port={Port} Param={BitConverter.ToString(Param.Bytes, Param.Offset, Param.Count)}";
        }

        public override void ClearParameters()
        {
            Ip = null;
            Port = 0;
            Param = Binary.Empty;
        }
    }

    public sealed class ServiceInfo : Bean
    {
        /// <summary>
        /// 服务名，比如"GameServer"
        /// </summary>
        public string ServiceName { get; private set; }
        public long Version { get; private set; }

        /// <summary>
        /// 服务id，对于 Zeze.Application，一般就是 Config.ServerId.
        /// 这里使用类型 string 是为了更好的支持扩展。
        /// </summary>
        public string ServiceIdentity { get; private set; }

        /// <summary>
        /// 服务ip-port，如果没有，保持空和0.
        /// </summary>
        public string PassiveIp { get; internal set; } = "";
        public int PassivePort { get; internal set; } = 0;

        // 服务扩展信息，可选。
        public Binary ExtraInfo { get; internal set; } = Binary.Empty;

        // ServiceManager，不是协议一部分，不会被系列化。
        // 算是一个简单的策略，不怎么优美。
        public object SessionId { get; internal set; }

        public ServiceInfo()
        {
        }

        public ServiceInfo(
            string name, string identity, long version,
            string ip = null, int port = 0,
            Binary extrainfo = null)
        {
            ServiceName = name;
            ServiceIdentity = identity;
            Version = version;
            if (null != ip)
                PassiveIp = ip;
            PassivePort = port;
            if (extrainfo != null)
                ExtraInfo = extrainfo;
        }

        public override void Decode(ByteBuffer bb)
        {
            ServiceName = bb.ReadString();
            ServiceIdentity = bb.ReadString();
            PassiveIp = bb.ReadString();
            PassivePort = bb.ReadInt();
            ExtraInfo = bb.ReadBinary();
            Version = bb.ReadLong();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteString(ServiceName);
            bb.WriteString(ServiceIdentity);
            bb.WriteString(PassiveIp);
            bb.WriteInt(PassivePort);
            bb.WriteBinary(ExtraInfo);
            bb.WriteLong(Version);
        }

        public override void ClearParameters()
        {
            ServiceName = null;
            ServiceIdentity = null;
            PassiveIp = "";
            PassivePort = 0;
            ExtraInfo = Binary.Empty;
            Version = 0;
        }

        public override int GetHashCode()
        {
            const int prime = 31;
            int result = 17;
            result = prime * result + ServiceName.GetHashCode();
            result = prime * result + ServiceIdentity.GetHashCode();
            return result;
        }

        public bool FullEquals(ServiceInfo other)
        {
            return ServiceName.Equals(other.ServiceName)
                && ServiceIdentity.Equals(other.ServiceIdentity)
                && Version.Equals(other.Version)
                && PassiveIp.Equals(other.PassiveIp)
                && PassivePort.Equals(other.PassivePort)
                && ExtraInfo.Equals(other.ExtraInfo);
        }

        public override bool Equals(object obj)
        {
            if (obj == this)
                return true;

            if (obj is ServiceInfo other)
            {
                return ServiceName.Equals(other.ServiceName)
                    && ServiceIdentity.Equals(other.ServiceIdentity);
            }
            return false;
        }

        public override string ToString()
        {
            return $"{ServiceName}@{ServiceIdentity}";
        }
    }

    public sealed class BEditService : Bean
    {
        public List<ServiceInfo> Remove { get; } = new();
        public List<ServiceInfo> Add { get; } = new();

        public override void ClearParameters()
        {
            Remove.Clear();
            Add.Clear();
        }

        public override void Decode(ByteBuffer bb)
        {
            for (var i = bb.ReadUInt(); i > 0; --i)
            {
                var r = new ServiceInfo();
                r.Decode(bb);
                Remove.Add(r);
            }
            for (var i = bb.ReadUInt(); i > 0; --i)
            {
                var r = new ServiceInfo();
                r.Decode(bb);
                Add.Add(r);
            }
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteUInt(Remove.Count);
            foreach (var r in Remove)
                r.Encode(bb);
            bb.WriteUInt(Add.Count);
            foreach (var p in Add)
                p.Encode(bb);
        }
    }

    /// <summary>
    /// 动态服务启动时通过这个rpc注册自己。
    /// </summary>
    public sealed class EditService : Rpc<BEditService, EmptyBean>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(EditService).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;

    }

    public sealed class SubscribeInfo : Bean
    {
        public string ServiceName { get; set; }
        public long Version { get; set; }

        // 目前这个用来给LinkdApp用来保存订阅的状态，不系列化。
        public object LocalState { get; set; }

        public SubscribeInfo()
        {

        }

        public SubscribeInfo(string name, long ver)
        {
            ServiceName = name;
            Version = ver;
        }

        public override void ClearParameters()
        {
            ServiceName = null;
            Version = 0;
        }

        public override void Decode(ByteBuffer bb)
        {
            ServiceName = bb.ReadString();
            Version = bb.ReadLong();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteString(ServiceName);
            bb.WriteLong(Version);
        }

        public override string ToString()
        {
            return ServiceName;
        }
    }

    public sealed class SubscribeArgument : Bean
    {
        public List<SubscribeInfo> Subs { get; } = new();

        public override void ClearParameters()
        {
            Subs.Clear();
        }

        public override void Decode(ByteBuffer bb)
        {
            for (var i = bb.ReadUInt(); i > 0; --i)
            {
                var sub = new SubscribeInfo();
                sub.Decode(bb);
                Subs.Add(sub);
            }
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteUInt(Subs.Count);
            foreach (var sub in Subs)
                sub.Encode(bb);
        }
    }

    public sealed class ServiceInfosVersion : Bean
    {
        public Dictionary<long, ServiceInfos> InfosVersion { get; } = new();

        public override void ClearParameters()
        {
            InfosVersion.Clear();
        }

        public bool IsServiceEmpty(long version)
        {
            if (InfosVersion.TryGetValue(version, out var infos))
                return infos.SortedIdentity.Count == 0;
            return true;
        }

        public override void Decode(ByteBuffer bb)
        {
            for (var i = bb.ReadInt(); i > 0; --i)
            {
                var version = bb.ReadLong();
                var infos = new ServiceInfos();
                infos.Decode(bb);
                InfosVersion[version] = infos;
            }
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteInt(InfosVersion.Count);
            foreach (var e in InfosVersion)
            {
                bb.WriteLong(e.Key);
                e.Value.Encode(bb);
            }
        }
    }

    public sealed class SubscribeResult : Bean
    {
        public Dictionary<string, ServiceInfosVersion> Map { get; } = new();

        public override void ClearParameters()
        {
            Map.Clear();
        }

        public override void Decode(ByteBuffer bb)
        {
            for (var i = bb.ReadUInt(); i > 0; --i)
            {
                var serviceName = bb.ReadString();
                var infos = new ServiceInfosVersion();
                infos.Decode(bb);
                Map[serviceName] = infos;
            }
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteUInt(Map.Count);
            foreach (var e in Map)
            {
                bb.WriteString(e.Key);
                e.Value.Encode(bb);
            }
        }
    }

    public sealed class Subscribe : Rpc<SubscribeArgument, SubscribeResult>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(Subscribe).FullName);

        public const int Success = 0;
        public const int DuplicateSubscribe = 1;
        public const int UnknownSubscribeType = 2;

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

    public sealed class UnSubscribeArgument : Bean
    {
        public List<string> ServiceNames { get; } = new();

        public override void ClearParameters()
        {
            ServiceNames.Clear();
        }

        public override void Decode(ByteBuffer bb)
        {
            for (var i = bb.ReadUInt(); i > 0; --i)
                ServiceNames.Add(bb.ReadString());
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteUInt(ServiceNames.Count);
            foreach (var serviceName in ServiceNames)
                bb.WriteString(serviceName);
        }
    }

    public sealed class UnSubscribe : Rpc<UnSubscribeArgument, EmptyBean>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(UnSubscribe).FullName);

        public const int Success = 0;
        public const int NotExist = 1;

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

    public sealed class ServiceInfos : Bean
    {
        // ServiceList maybe empty. need a ServiceName
        public string ServiceName { get; private set; }
        // sorted by ServiceIdentity
        private List<ServiceInfo> SortedIdentity_ { get; } = new();
        public IReadOnlyList<ServiceInfo> SortedIdentity => SortedIdentity_;
        public long SerialId { get; set; }

        private readonly static ServiceIdentityComparer Comparer = new();

        public override void ClearParameters()
        {
            ServiceName = null;
            SortedIdentity_.Clear();
            SerialId = 0;
        }

        public ServiceInfo Insert(ServiceInfo info)
        {
            var i = SortedIdentity_.BinarySearch(info, Comparer);
            if (i >= 0)
            {
                var exist = SortedIdentity_[i];
                SortedIdentity_[i] = info;
                return exist;
            }
            SortedIdentity_.Insert(~i, info);
            return null;
        }

        public ServiceInfo Remove(ServiceInfo info)
        {
            var i = SortedIdentity_.BinarySearch(info, Comparer);
            if (i >= 0)
            {
                info = SortedIdentity_[i];
                SortedIdentity_.RemoveAt(i);
                return info;
            }
            return null;
        }

        public ServiceInfo Find(string identity)
        {
            var i = SortedIdentity_.BinarySearch(new ServiceInfo(ServiceName, identity, 0), Comparer);
            if (i >= 0)
                return SortedIdentity_[i];
            return null;
        }

        public ServiceInfo Find(int serverId)
        {
            return Find(serverId.ToString());
        }

        public ServiceInfos()
        {
        }

        public ServiceInfos(string serviceName)
        {
            ServiceName = serviceName;
        }

        public bool TryGetServiceInfo(string identity, out ServiceInfo info)
        {
            var cur = new ServiceInfo(ServiceName, identity, 0);
            int index = SortedIdentity_.BinarySearch(cur, Comparer);
            if (index >= 0)
            {
                info = SortedIdentity[index];
                return true;
            }
            info = null;
            return false;
        }

        public override void Decode(ByteBuffer bb)
        {
            ServiceName = bb.ReadString();
            SortedIdentity_.Clear();
            for (int c = bb.ReadInt(); c > 0; --c)
            {
                var service = new ServiceInfo();
                service.Decode(bb);
                SortedIdentity_.Add(service);
            }
            SerialId = bb.ReadLong();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteString(ServiceName);
            bb.WriteInt(SortedIdentity_.Count);
            foreach (var service in SortedIdentity_)
            {
                service.Encode(bb);
            }
            bb.WriteLong(SerialId);
        }

        public override string ToString()
        {
            var sb = new StringBuilder();
            sb.Append(ServiceName).Append('=');
            sb.Append('[');
            foreach (var e in SortedIdentity)
            {
                sb.Append(e.ServiceIdentity);
                sb.Append(',');
            }
            sb.Append(']');
            return sb.ToString();
        }
    }

    // 实际上可以不用这个类，为了保持以后ServiceInfo的比较可能改变，写一个这个类。
    public sealed class ServiceInfoEqualityComparer : IEqualityComparer<ServiceInfo>
    {
        public bool Equals(ServiceInfo x, ServiceInfo y)
        {
            return x.Equals(y);
        }

        public int GetHashCode([DisallowNull] ServiceInfo obj)
        {
            return obj.GetHashCode();
        }
    }

    public sealed class ServiceIdentityComparer : IComparer<ServiceInfo>
    {
        public int Compare(ServiceInfo x, ServiceInfo y)
        {
            string id1 = x.ServiceIdentity;
            string id2 = y.ServiceIdentity;
            int c = id1.Length.CompareTo(id2.Length);
            return c != 0 ? c : String.Compare(id1, id2, StringComparison.Ordinal);
        }
    }

    public sealed class KeepAlive : Rpc<EmptyBean, EmptyBean>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(KeepAlive).FullName);

        public const int Success = 0;

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

    public sealed class SetServerLoad : Protocol<ServerLoad>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(SetServerLoad).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

    public sealed class AllocateIdArgument : Bean
    {
        public string Name { get; set; }
        public int Count { get; set; }

        public override void Decode(ByteBuffer bb)
        {
            Name = bb.ReadString();
            Count = bb.ReadInt();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteString(Name);
            bb.WriteInt(Count);
        }

        public override void ClearParameters()
        {
            Name = null;
            Count = 0;
        }
    }

    public sealed class AllocateIdResult : Bean
    {
        public string Name { get; set; }
        public long StartId { get; set; }
        public int Count { get; set; }

        public override void Decode(ByteBuffer bb)
        {
            Name = bb.ReadString();
            StartId = bb.ReadLong();
            Count = bb.ReadInt();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteString(Name);
            bb.WriteLong(StartId);
            bb.WriteInt(Count);
        }

        public override void ClearParameters()
        {
            Name = null;
            StartId = 0;
            Count = 0;
        }
    }

    public sealed class AllocateId : Rpc<AllocateIdArgument, AllocateIdResult>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(AllocateId).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

}
