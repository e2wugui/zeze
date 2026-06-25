// auto-generated

namespace Zege.Linkd
{
    public partial class ModuleLinkd : AbstractModule
    {
        public const int ModuleId = 10000;


        public global::Zege.App App { get; }

        public ModuleLinkd(global::Zege.App app)
        {
            App = app;
        }

        public override void Register()
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(GetType());
            App.ClientService.AddFactoryHandle(42949786375344, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Linkd.Challenge(),
                Handle = ProcessChallengeRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessChallengeRequest", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessChallengeRequest", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(42949746867130, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Linkd.ChallengeMe(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessChallengeMeResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessChallengeMeResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(42952977189268, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Linkd.ChallengeResult(),
                Handle = ProcessChallengeResultRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessChallengeResultRequest", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessChallengeResultRequest", Zeze.Transaction.DispatchMode.Normal),
            });
            // register table
        }

        public override void UnRegister()
        {
            App.ClientService.Factorys.TryRemove(42949786375344, out var _);
            App.ClientService.Factorys.TryRemove(42949746867130, out var _);
            App.ClientService.Factorys.TryRemove(42952977189268, out var _);
        }
    }
}
