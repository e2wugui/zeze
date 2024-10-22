public class Redirect_Zeze_Game_Online : Zeze.Game.Online
{
    protected override async System.Threading.Tasks.Task RedirectNotify(int serverId, long roleId)
    {
        // RedirectToServer
        var _target_ = App.Zeze.Redirect.ChoiceServer(this, serverId);
        if (_target_ == null) {
            // local: loop-back
            await App.Zeze.NewProcedure(async () => { await base.RedirectNotify(serverId, roleId); return 0; }, "Zeze.Game.Online:RedirectNotify").ExecuteAsync();
            return;
        }

        var rpc13 = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        rpc13.Argument.ModuleId = 11013;
        rpc13.Argument.RedirectType = Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeToServer;
        rpc13.Argument.HashCode = serverId;
        rpc13.Argument.MethodFullName = "Zeze.Game.Online:RedirectNotify";
        rpc13.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteLong(roleId);
            rpc13.Argument.Params = new Zeze.Net.Binary(_bb_);
        }

        var future14 = new System.Threading.Tasks.TaskCompletionSource();

        rpc13.Send(_target_, (_) =>
        {
            if (rpc13.IsTimeout)
            {
                future14.TrySetException(new System.Exception("Zeze.Game.Online:RedirectNotify Rpc Timeout."));
            }
            else if (0 != rpc13.ResultCode)
            {
                future14.TrySetException(new System.Exception($"Zeze.Game.Online:RedirectNotify Rpc Error {rpc13.ResultCode}."));
            }
            else
            {
                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(rpc13.Result.Params);
                future14.TrySetResult();
            }
            return System.Threading.Tasks.Task.FromResult(Zeze.Util.ResultCode.Success);
        });

        await future14.Task;
    }

    public Redirect_Zeze_Game_Online(Zeze.AppBase app) : base(app)
    {
        var hName15 = new Zeze.Arch.RedirectHandle();
        hName15.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName15.RequestHandle = async (_sessionId_, _HashOrServerId_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            long roleId;
            roleId = _bb_.ReadLong();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            await base.RedirectNotify(_HashOrServerId_, roleId);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Zeze.Game.Online:RedirectNotify", hName15);
    }

}
