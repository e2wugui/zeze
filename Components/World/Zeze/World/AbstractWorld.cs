// auto generate

// ReSharper disable RedundantNameQualifier UnusedParameter.Global UnusedVariable
// ReSharper disable once CheckNamespace
namespace Zeze.World
{
    public abstract class AbstractWorld : Zeze.IModule 
    {
        public const int ModuleId = 11031;
        public override string FullName => "Zeze.World.World";
        public override string Name => "World";
        public override int Id => ModuleId;
        public override bool IsBuiltin => true;


        public const int eCommandHandlerMissing = 1;

        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(GetType());
            service.AddFactoryHandle(47378281792093, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.World.Command(),
                Handle = ProcessCommand,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessCommandp", Zeze.Transaction.TransactionLevel.None),
                Mode = _reflect.GetDispatchMode("ProcessCommand", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47381630294274, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.World.Query(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessQueryResponse", Zeze.Transaction.TransactionLevel.None),
                Mode = _reflect.GetDispatchMode("ProcessQueryResponse", Zeze.Transaction.DispatchMode.Normal),
            });
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
            service.Factorys.TryRemove(47378281792093, out var _);
            service.Factorys.TryRemove(47381630294274, out var _);
        }

        public void RegisterZezeTables(Zeze.Application zeze)
        {
            // register table
        }

        public void UnRegisterZezeTables(Zeze.Application zeze)
        {
        }


        protected abstract System.Threading.Tasks.Task<long> ProcessCommand(Zeze.Net.Protocol p);
    }
}
