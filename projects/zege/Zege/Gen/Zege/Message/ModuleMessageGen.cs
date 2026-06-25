// auto-generated

namespace Zege.Message
{
    public partial class ModuleMessage : AbstractModule
    {
        public const int ModuleId = 3;


        public global::Zege.App App { get; }

        public ModuleMessage(global::Zege.App app)
        {
            App = app;
        }

        public override void Register()
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(GetType());
            App.ClientService.AddFactoryHandle(13285773691, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Message.GetFriendMessage(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessGetFriendMessageResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessGetFriendMessageResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(14680978625, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Message.GetGroupMessage(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessGetGroupMessageResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessGetGroupMessageResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(14865288474, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Message.NotifyMessage(),
                Handle = ProcessNotifyMessageRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessNotifyMessageRequest", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessNotifyMessageRequest", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(12948077512, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Message.SendDepartmentMessage(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessSendDepartmentMessageResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessSendDepartmentMessageResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(14083283825, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Message.SendMessage(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessSendMessageResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessSendMessageResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(14659306406, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Message.SetFriendMessageHasRead(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessSetFriendMessageHasReadResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessSetFriendMessageHasReadResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(13866762277, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Message.SetGroupMessageHasRead(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessSetGroupMessageHasReadResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessSetGroupMessageHasReadResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            // register table
        }

        public override void UnRegister()
        {
            App.ClientService.Factorys.TryRemove(13285773691, out var _);
            App.ClientService.Factorys.TryRemove(14680978625, out var _);
            App.ClientService.Factorys.TryRemove(14865288474, out var _);
            App.ClientService.Factorys.TryRemove(12948077512, out var _);
            App.ClientService.Factorys.TryRemove(14083283825, out var _);
            App.ClientService.Factorys.TryRemove(14659306406, out var _);
            App.ClientService.Factorys.TryRemove(13866762277, out var _);
        }
    }
}
