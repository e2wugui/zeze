// auto generate

// ReSharper disable RedundantNameQualifier UnusedParameter.Global UnusedVariable
// ReSharper disable once CheckNamespace
namespace Zeze.Arch
{
    public abstract class AbstractLinkdProvider : Zeze.IModule 
    {
        public const int ModuleId = 11008;
        public override string FullName => "Zeze.Arch.LinkdProvider";
        public override string Name => "LinkdProvider";
        public override int Id => ModuleId;
        public override bool IsBuiltin => true;


        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(GetType());
            service.AddFactoryHandle(47279202608226, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Provider.AnnounceProviderInfo(),
                Handle = ProcessAnnounceProviderInfo,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessAnnounceProviderInfop", Zeze.Transaction.TransactionLevel.None),
                Mode = _reflect.GetDispatchMode("ProcessAnnounceProviderInfo", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47279114253990, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Provider.Bind(),
                Handle = ProcessBindRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessBindRequest", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessBindRequest", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47282408036866, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Provider.Broadcast(),
                Handle = ProcessBroadcast,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessBroadcastp", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessBroadcast", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47283221887522, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Provider.Kick(),
                Handle = ProcessKick,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessKickp", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessKick", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47281226998238, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Provider.Send(),
                Handle = ProcessSendRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessSendRequest", Zeze.Transaction.TransactionLevel.None),
                Mode = _reflect.GetDispatchMode("ProcessSendRequest", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47281569047175, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Provider.SetUserState(),
                Handle = ProcessSetUserState,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessSetUserStatep", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessSetUserState", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47280110454586, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Provider.Subscribe(),
                Handle = ProcessSubscribeRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessSubscribeRequest", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessSubscribeRequest", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47281107578964, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Provider.UnBind(),
                Handle = ProcessUnBindRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessUnBindRequest", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessUnBindRequest", Zeze.Transaction.DispatchMode.Normal),
            });
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
            service.Factorys.TryRemove(47279202608226, out var _);
            service.Factorys.TryRemove(47279114253990, out var _);
            service.Factorys.TryRemove(47282408036866, out var _);
            service.Factorys.TryRemove(47283221887522, out var _);
            service.Factorys.TryRemove(47281226998238, out var _);
            service.Factorys.TryRemove(47281569047175, out var _);
            service.Factorys.TryRemove(47280110454586, out var _);
            service.Factorys.TryRemove(47281107578964, out var _);
        }

        public void RegisterZezeTables(Zeze.Application zeze)
        {
            // register table
        }

        public void UnRegisterZezeTables(Zeze.Application zeze)
        {
        }

        public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks)
        {
        }


        protected abstract System.Threading.Tasks.Task<long>  ProcessAnnounceProviderInfo(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessBindRequest(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessBroadcast(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessKick(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessSendRequest(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessSetUserState(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessSubscribeRequest(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessUnBindRequest(Zeze.Net.Protocol p);
    }
}
