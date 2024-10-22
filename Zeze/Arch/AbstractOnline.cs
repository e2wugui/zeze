// auto generate

// ReSharper disable RedundantNameQualifier UnusedParameter.Global UnusedVariable
// ReSharper disable once CheckNamespace
namespace Zeze.Arch
{
    public abstract class AbstractOnline : Zeze.IModule 
    {
        public const int ModuleId = 11100;
        public override string FullName => "Zeze.Arch.Online";
        public override string Name => "Online";
        public override int Id => ModuleId;
        public override bool IsBuiltin => true;


        public const int ResultCodeSuccess = 0;
        public const int ResultCodeCreateRoleDuplicateRoleName = 1;
        public const int ResultCodeAccountNotExist = 2;
        public const int ResultCodeRoleNotExist = 3;
        public const int ResultCodeNotLastLoginRoleId = 4;
        public const int ResultCodeOnlineDataNotFound = 5;
        public const int ResultCodeReliableNotifyConfirmIndexOutOfRange = 6;
        public const int ResultCodeNotLogin = 7;
        internal Zeze.Builtin.Online.tLocal _tLocal = new();
        internal Zeze.Builtin.Online.tOnline _tOnline = new();
        internal Zeze.Builtin.Online.tVersion _tVersion = new();

        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(GetType());
            service.AddFactoryHandle(47676933001134, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Online.Login(),
                Handle = ProcessLoginRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessLoginRequest", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessLoginRequest", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47676519983553, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Online.Logout(),
                Handle = ProcessLogoutRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessLogoutRequest", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessLogoutRequest", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47678187220010, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Online.ReliableNotifyConfirm(),
                Handle = ProcessReliableNotifyConfirmRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessReliableNotifyConfirmRequest", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessReliableNotifyConfirmRequest", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47675064884515, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Online.ReLogin(),
                Handle = ProcessReLoginRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessReLoginRequest", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessReLoginRequest", Zeze.Transaction.DispatchMode.Normal),
            });
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
            service.Factorys.TryRemove(47676933001134, out var _);
            service.Factorys.TryRemove(47676519983553, out var _);
            service.Factorys.TryRemove(47678187220010, out var _);
            service.Factorys.TryRemove(47675064884515, out var _);
        }

        public void RegisterZezeTables(Zeze.Application zeze)
        {
            // register table
            zeze.AddTable(zeze.Config.GetTableConf(_tLocal.Name).DatabaseName, _tLocal);
            zeze.AddTable(zeze.Config.GetTableConf(_tOnline.Name).DatabaseName, _tOnline);
            zeze.AddTable(zeze.Config.GetTableConf(_tVersion.Name).DatabaseName, _tVersion);
        }

        public void UnRegisterZezeTables(Zeze.Application zeze)
        {
            zeze.RemoveTable(zeze.Config.GetTableConf(_tLocal.Name).DatabaseName, _tLocal);
            zeze.RemoveTable(zeze.Config.GetTableConf(_tOnline.Name).DatabaseName, _tOnline);
            zeze.RemoveTable(zeze.Config.GetTableConf(_tVersion.Name).DatabaseName, _tVersion);
        }

        public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks)
        {
        }


        protected abstract System.Threading.Tasks.Task<long>  ProcessLoginRequest(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessLogoutRequest(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessReliableNotifyConfirmRequest(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessReLoginRequest(Zeze.Net.Protocol p);
    }
}
