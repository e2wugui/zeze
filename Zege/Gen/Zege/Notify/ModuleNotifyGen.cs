// auto-generated

namespace Zege.Notify
{
    public partial class ModuleNotify : AbstractModule
    {
        public const int ModuleId = 4;


        public global::Zege.App App { get; }

        public ModuleNotify(global::Zege.App app)
        {
            App = app;
        }

        public override void Register()
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(GetType());
            App.ClientService.AddFactoryHandle(17408948748, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Notify.GetNotifyNode(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessGetNotifyNodeResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessGetNotifyNodeResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(20922496474, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Notify.NotifyNodeLogBeanNotify(),
                Handle = ProcessNotifyNodeLogBeanNotify,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessNotifyNodeLogBeanNotifyp", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessNotifyNodeLogBeanNotify", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(20814075074, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Notify.RemoveNotify(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessRemoveNotifyResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessRemoveNotifyResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(18457530286, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Notify.SendNotify(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessSendNotifyResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessSendNotifyResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            // register table
        }

        public override void UnRegister()
        {
            App.ClientService.Factorys.TryRemove(17408948748, out var _);
            App.ClientService.Factorys.TryRemove(20922496474, out var _);
            App.ClientService.Factorys.TryRemove(20814075074, out var _);
            App.ClientService.Factorys.TryRemove(18457530286, out var _);
        }
    }
}
