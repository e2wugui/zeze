public class Redirect_Zeze_Game_Rank : Zeze.Game.Rank
{
    protected override async System.Threading.Tasks.Task<long> UpdateRank(int hash, Zeze.Builtin.Game.Rank.BConcurrentKey keyHint, long roleId, long value, Zeze.Net.Binary valueEx)
    {
        // RedirectHash
        var _target_ = App.Zeze.Redirect.ChoiceHash(this, hash);
        if (_target_ == null) {
            // local: loop-back
            var returnResult1 = default(long);
            await App.Zeze.NewProcedure(async () => { returnResult1 = await base.UpdateRank(hash, keyHint, roleId, value, valueEx); return 0; }, "Zeze.Game.Rank:UpdateRank").ExecuteAsync();
            return returnResult1;
        }

        var rpc2 = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        rpc2.Argument.ModuleId = 11015;
        rpc2.Argument.RedirectType = Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeWithHash;
        rpc2.Argument.HashCode = hash;
        rpc2.Argument.MethodFullName = "Zeze.Game.Rank:UpdateRank";
        rpc2.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            keyHint.Encode(_bb_);
            _bb_.WriteLong(roleId);
            _bb_.WriteLong(value);
            _bb_.WriteBinary(valueEx);
            rpc2.Argument.Params = new Zeze.Net.Binary(_bb_);
        }

        var future3 = new System.Threading.Tasks.TaskCompletionSource<long>();

        rpc2.Send(_target_, async (_) =>
        {
            if (rpc2.IsTimeout)
            {
                future3.TrySetException(new System.Exception("Zeze.Game.Rank:UpdateRank Rpc Timeout."));
            }
            else if (0 != rpc2.ResultCode)
            {
                future3.TrySetException(new System.Exception($"Zeze.Game.Rank:UpdateRank Rpc Error {rpc2.ResultCode}."));
            }
            else
            {
                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(rpc2.Result.Params);
                long theResult4;
                theResult4 = _bb_.ReadLong();
                future3.TrySetResult(theResult4);
            }
            return Zeze.Transaction.Procedure.Success;
        });

        return await future3.Task;
    }

    public Redirect_Zeze_Game_Rank(Zeze.AppBase app) : base(app)
    {
        var hName5 = new Zeze.Arch.RedirectHandle();
        hName5.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName5.RequestHandle = async (_sessionId_, _HashOrServerId_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            Zeze.Builtin.Game.Rank.BConcurrentKey keyHint = new Zeze.Builtin.Game.Rank.BConcurrentKey();
            long roleId;
            long value;
            Zeze.Net.Binary valueEx;
            keyHint.Decode(_bb_);
            roleId = _bb_.ReadLong();
            value = _bb_.ReadLong();
            valueEx = _bb_.ReadBinary();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            var asyncResult6 = await base.UpdateRank(_HashOrServerId_, keyHint, roleId, value, valueEx);
            _bb_.WriteLong(asyncResult6);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Zeze.Game.Rank:UpdateRank", hName5);
    }

}
