// auto-generated

namespace Zeze.Builtin.Online
{
    public partial class ModuleOnline : AbstractModule
    {
        public const int ModuleId = 11100;


        public global::Zege.App App { get; }

        public ModuleOnline(global::Zege.App app)
        {
            App = app;
        }

        public override void Register()
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(GetType());
            App.ClientService.AddFactoryHandle(47676933001134, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Online.Login(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessLoginResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessLoginResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(47676519983553, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Online.Logout(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessLogoutResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessLogoutResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(47678187220010, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Online.ReliableNotifyConfirm(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessReliableNotifyConfirmResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessReliableNotifyConfirmResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(47675064884515, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Online.ReLogin(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessReLoginResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessReLoginResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(47674377593304, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Online.SReliableNotify(),
                Handle = ProcessSReliableNotify,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessSReliableNotifyp", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessSReliableNotify", Zeze.Transaction.DispatchMode.Normal),
            });
            // register table
        }

        public override void UnRegister()
        {
            App.ClientService.Factorys.TryRemove(47676933001134, out var _);
            App.ClientService.Factorys.TryRemove(47676519983553, out var _);
            App.ClientService.Factorys.TryRemove(47678187220010, out var _);
            App.ClientService.Factorys.TryRemove(47675064884515, out var _);
            App.ClientService.Factorys.TryRemove(47674377593304, out var _);
        }
    }
}
