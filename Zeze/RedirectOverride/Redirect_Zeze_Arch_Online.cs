public class Redirect_Zeze_Arch_Online : Zeze.Arch.Online
{
    protected override async System.Threading.Tasks.Task RedirectNotify(int serverId, string account)
    {
        // RedirectToServer
        var _target_ = App.Zeze.Redirect.ChoiceServer(this, serverId);
        if (_target_ == null) {
            // local: loop-back
            await App.Zeze.NewProcedure(async () => { await base.RedirectNotify(serverId, account); return 0; }, "Zeze.Arch.Online:RedirectNotify").ExecuteAsync();
            return;
        }

        var rpc16 = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        rpc16.Argument.ModuleId = 11100;
        rpc16.Argument.RedirectType = Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeToServer;
        rpc16.Argument.HashCode = serverId;
        rpc16.Argument.MethodFullName = "Zeze.Arch.Online:RedirectNotify";
        rpc16.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteString(account);
            rpc16.Argument.Params = new Zeze.Net.Binary(_bb_);
        }

        var future17 = new System.Threading.Tasks.TaskCompletionSource();

        rpc16.Send(_target_, async (_) =>
        {
            if (rpc16.IsTimeout)
            {
                future17.TrySetException(new System.Exception("Zeze.Arch.Online:RedirectNotify Rpc Timeout."));
            }
            else if (0 != rpc16.ResultCode)
            {
                future17.TrySetException(new System.Exception($"Zeze.Arch.Online:RedirectNotify Rpc Error {rpc16.ResultCode}."));
            }
            else
            {
                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(rpc16.Result.Params);
                future17.TrySetResult();
            }
            return Zeze.Util.ResultCode.Success;
        });

        await future17.Task;
    }

    public Redirect_Zeze_Arch_Online(Zeze.AppBase app) : base(app)
    {
        var hName18 = new Zeze.Arch.RedirectHandle();
        hName18.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName18.RequestHandle = async (_sessionId_, _HashOrServerId_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            string account;
            account = _bb_.ReadString();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            await base.RedirectNotify(_HashOrServerId_, account);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Zeze.Arch.Online:RedirectNotify", hName18);
    }

}
