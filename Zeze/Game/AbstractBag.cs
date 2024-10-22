// auto generate

// ReSharper disable RedundantNameQualifier UnusedParameter.Global UnusedVariable
// ReSharper disable once CheckNamespace
namespace Zeze.Game
{
    public abstract class AbstractBag : Zeze.IModule 
    {
        public const int ModuleId = 11014;
        public override string FullName => "Zeze.Game.Bag";
        public override string Name => "Bag";
        public override int Id => ModuleId;
        public override bool IsBuiltin => true;


        public const int ResultCodeFromInvalid = 1;
        public const int ResultCodeToInvalid = 2;
        public const int ResultCodeFromNotExist = 3;
        public const int ResultCodeTrySplitButTargetExistDifferenceItem = 4;
        internal Zeze.Builtin.Game.Bag.tBag _tBag = new();
        internal Zeze.Builtin.Game.Bag.tItemClasses _tItemClasses = new();

        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(GetType());
            service.AddFactoryHandle(47307869964755, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Game.Bag.Destroy(),
                Handle = ProcessDestroyRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessDestroyRequest", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessDestroyRequest", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47308274693689, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Game.Bag.Move(),
                Handle = ProcessMoveRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessMoveRequest", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessMoveRequest", Zeze.Transaction.DispatchMode.Normal),
            });
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
            service.Factorys.TryRemove(47307869964755, out var _);
            service.Factorys.TryRemove(47308274693689, out var _);
        }

        public void RegisterZezeTables(Zeze.Application zeze)
        {
            // register table
            zeze.AddTable(zeze.Config.GetTableConf(_tBag.Name).DatabaseName, _tBag);
            zeze.AddTable(zeze.Config.GetTableConf(_tItemClasses.Name).DatabaseName, _tItemClasses);
        }

        public void UnRegisterZezeTables(Zeze.Application zeze)
        {
            zeze.RemoveTable(zeze.Config.GetTableConf(_tBag.Name).DatabaseName, _tBag);
            zeze.RemoveTable(zeze.Config.GetTableConf(_tItemClasses.Name).DatabaseName, _tItemClasses);
        }

        public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks)
        {
        }


        protected abstract System.Threading.Tasks.Task<long>  ProcessDestroyRequest(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessMoveRequest(Zeze.Net.Protocol p);
    }
}
