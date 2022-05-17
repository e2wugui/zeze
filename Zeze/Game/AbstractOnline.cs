// auto generate
namespace Zeze.Game
{
    public abstract class AbstractOnline : Zeze.IModule 
    {
        public const int ModuleId = 11013;
        public override string FullName => "Zeze.Game.Online";
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
        internal Zeze.Builtin.Game.Online.taccount _taccount = new Zeze.Builtin.Game.Online.taccount();
        internal Zeze.Builtin.Game.Online.tlocal _tlocal = new Zeze.Builtin.Game.Online.tlocal();
        internal Zeze.Builtin.Game.Online.tonline _tonline = new Zeze.Builtin.Game.Online.tonline();
        internal Zeze.Builtin.Game.Online.tversion _tversion = new Zeze.Builtin.Game.Online.tversion();

        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(this.GetType());
            service.AddFactoryHandle(47303980222879, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Game.Online.Login(),
                Handle = ProcessLoginRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessLoginRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47304205955457, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Game.Online.Logout(),
                Handle = ProcessLogoutRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessLogoutRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47304349755660, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Game.Online.ReliableNotifyConfirm(),
                Handle = ProcessReliableNotifyConfirmRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessReliableNotifyConfirmRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47304551116333, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Game.Online.ReLogin(),
                Handle = ProcessReLoginRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessReLoginRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
            service.Factorys.TryRemove(47303980222879, out var _);
            service.Factorys.TryRemove(47304205955457, out var _);
            service.Factorys.TryRemove(47304349755660, out var _);
            service.Factorys.TryRemove(47304551116333, out var _);
        }

        public void RegisterZezeTables(Zeze.Application zeze)
        {
            // register table
            zeze.AddTable(zeze.Config.GetTableConf(_taccount.Name).DatabaseName, _taccount);
            zeze.AddTable(zeze.Config.GetTableConf(_tlocal.Name).DatabaseName, _tlocal);
            zeze.AddTable(zeze.Config.GetTableConf(_tonline.Name).DatabaseName, _tonline);
            zeze.AddTable(zeze.Config.GetTableConf(_tversion.Name).DatabaseName, _tversion);
        }

        public void UnRegisterZezeTables(Zeze.Application zeze)
        {
            zeze.RemoveTable(zeze.Config.GetTableConf(_taccount.Name).DatabaseName, _taccount);
            zeze.RemoveTable(zeze.Config.GetTableConf(_tlocal.Name).DatabaseName, _tlocal);
            zeze.RemoveTable(zeze.Config.GetTableConf(_tonline.Name).DatabaseName, _tonline);
            zeze.RemoveTable(zeze.Config.GetTableConf(_tversion.Name).DatabaseName, _tversion);
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
