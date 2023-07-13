// auto generate

// ReSharper disable RedundantNameQualifier UnusedParameter.Global UnusedVariable
// ReSharper disable once CheckNamespace
namespace Zeze.World
{
    public abstract class AbstractWorldStatic : Zeze.IModule 
    {
        public const int ModuleId = 11032;
        public override string FullName => "Zeze.World.WorldStatic";
        public override string Name => "WorldStatic";
        public override int Id => ModuleId;
        public override bool IsBuiltin => true;


        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(GetType());
            service.AddFactoryHandle(47385820582911, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.World.Static.SwitchWorld(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessSwitchWorldResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessSwitchWorldResponse", Zeze.Transaction.DispatchMode.Normal),
            });
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
            service.Factorys.TryRemove(47385820582911, out var _);
        }

        public void RegisterZezeTables(Zeze.Application zeze)
        {
            // register table
        }

        public void UnRegisterZezeTables(Zeze.Application zeze)
        {
        }

    }
}
