// auto-generated

namespace Zege.User
{
    public partial class ModuleUser : AbstractModule
    {
        public const int ModuleId = 1;


        public global::Zege.App App { get; }

        public ModuleUser(global::Zege.App app)
        {
            App = app;
        }

        public override void Register()
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(GetType());
            App.ClientService.AddFactoryHandle(6049344077, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.User.Create(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessCreateResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessCreateResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(7699023506, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.User.CreateWithCert(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessCreateWithCertResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessCreateWithCertResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(7678745845, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.User.Prepare(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessPrepareResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessPrepareResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(5616455661, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.User.VerifyChallengeResult(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessVerifyChallengeResultResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessVerifyChallengeResultResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            // register table
        }

        public override void UnRegister()
        {
            App.ClientService.Factorys.TryRemove(6049344077, out var _);
            App.ClientService.Factorys.TryRemove(7699023506, out var _);
            App.ClientService.Factorys.TryRemove(7678745845, out var _);
            App.ClientService.Factorys.TryRemove(5616455661, out var _);
        }
    }
}
