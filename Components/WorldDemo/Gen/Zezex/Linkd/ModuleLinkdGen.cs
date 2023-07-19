// auto-generated

namespace Zezex.Linkd
{
    public partial class ModuleLinkd : AbstractModule
    {
        public const int ModuleId = 10000;


        public global::Zeze.App App { get; }

        public ModuleLinkd(global::Zeze.App app)
        {
            App = app;
        }

        public override void Register()
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(GetType());
            App.ClientService.AddFactoryHandle(42952970027574, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zezex.Linkd.Auth(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessAuthResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessAuthResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(42951249029979, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zezex.Linkd.KeepAlive(),
                Handle = ProcessKeepAlive,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessKeepAlivep", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessKeepAlive", Zeze.Transaction.DispatchMode.Normal),
            });
            // register table
        }

        public override void UnRegister()
        {
            App.ClientService.Factorys.TryRemove(42952970027574, out var _);
            App.ClientService.Factorys.TryRemove(42951249029979, out var _);
        }
    }
}
