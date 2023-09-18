// auto-generated

namespace Zeze.Builtin.LinkdBase
{
    public partial class ModuleLinkdBase : AbstractModule
    {
        public const int ModuleId = 11011;


        public global::Zege.App App { get; }

        public ModuleLinkdBase(global::Zege.App app)
        {
            App = app;
        }

        public override void Register()
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(GetType());
            App.ClientService.AddFactoryHandle(47292803771063, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.LinkdBase.ReportError(),
                Handle = ProcessReportError,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessReportErrorp", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessReportError", Zeze.Transaction.DispatchMode.Normal),
            });
            // register table
        }

        public override void UnRegister()
        {
            App.ClientService.Factorys.TryRemove(47292803771063, out var _);
        }
    }
}
