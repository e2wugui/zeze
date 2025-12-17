// auto generate

// ReSharper disable RedundantNameQualifier UnusedParameter.Global UnusedVariable
// ReSharper disable once CheckNamespace
namespace Zeze.Gen.java
{
    public abstract class AbstractHotDistribute : Zeze.IModule 
    {
        public const int ModuleId = 11033;
        public override string FullName => "Zeze.Gen.java.HotDistribute";
        public override string Name => "HotDistribute";
        public override int Id => ModuleId;
        public override bool IsBuiltin => true;


        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(GetType());
            service.AddFactoryHandle(47389512970537, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.HotDistribute.GetLastVersionBeanInfo(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessGetLastVersionBeanInfoResponse", Zeze.Transaction.TransactionLevel.None),
                Mode = _reflect.GetDispatchMode("ProcessGetLastVersionBeanInfoResponse", Zeze.Transaction.DispatchMode.Normal),
            });
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
            service.Factorys.TryRemove(47389512970537, out var _);
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
