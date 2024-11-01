public class Redirect_Zeze_Game_Rank : Zeze.Game.Rank
{
    public override async System.Threading.Tasks.Task<Zeze.Arch.RedirectAll<Zeze.Builtin.Game.Rank.BRankList>> GetRankAll(Zeze.Builtin.Game.Rank.BConcurrentKey key)
    {
        // RedirectAll
        var reqall1 = new Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest();
        reqall1.Argument.ModuleId = 11015;
        reqall1.Argument.HashCodeConcurrentLevel = GetConcurrentLevel(key.RankType);
        // reqall1.Argument.HashCodes = // setup in linkd;
        reqall1.Argument.MethodFullName = "Zeze.Game.Rank:GetRankAll";
        reqall1.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            key.Encode(_bb_);
            reqall1.Argument.Params = new Zeze.Net.Binary(_bb_);
        }
        var decoder2 = (Zeze.Net.Binary _params_) => 
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            Zeze.Builtin.Game.Rank.BRankList result3 = new Zeze.Builtin.Game.Rank.BRankList();
            result3.Decode(_bb_);
            return result3;
        };
        var ctxall4 = new Zeze.Arch.RedirectAll<Zeze.Builtin.Game.Rank.BRankList>(
            reqall1.Argument.HashCodeConcurrentLevel, reqall1.Argument.MethodFullName,
            decoder2);
        reqall1.Argument.SessionId = App.Zeze.Redirect.ProviderApp.ProviderDirectService.AddManualContextWithTimeout(ctxall4);

        App.Zeze.Redirect.RedirectAll(this, reqall1);

        await ctxall4.Future.Task;
        return ctxall4;
    }

    protected override async System.Threading.Tasks.Task<long> UpdateRank(int hash, Zeze.Builtin.Game.Rank.BConcurrentKey keyHint, long roleId, long value, Zeze.Net.Binary valueEx)
    {
        // RedirectHash
        var _target_ = App.Zeze.Redirect.ChoiceHash(this, hash, GetConcurrentLevel(keyHint.RankType));
        if (_target_ == null) {
            // local: loop-back
            var returnResult7 = default(long);
            await App.Zeze.NewProcedure(async () => { returnResult7 = await base.UpdateRank(hash, keyHint, roleId, value, valueEx); return 0; }, "Zeze.Game.Rank:UpdateRank").ExecuteAsync();
            return returnResult7;
        }

        var rpc8 = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        rpc8.Argument.ModuleId = 11015;
        rpc8.Argument.RedirectType = Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeWithHash;
        rpc8.Argument.HashCode = hash;
        rpc8.Argument.MethodFullName = "Zeze.Game.Rank:UpdateRank";
        rpc8.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            keyHint.Encode(_bb_);
            _bb_.WriteLong(roleId);
            _bb_.WriteLong(value);
            _bb_.WriteBinary(valueEx);
            rpc8.Argument.Params = new Zeze.Net.Binary(_bb_);
        }

        var future9 = new System.Threading.Tasks.TaskCompletionSource<long>();

        rpc8.Send(_target_, (_) =>
        {
            if (rpc8.IsTimeout)
            {
                future9.TrySetException(new System.Exception("Zeze.Game.Rank:UpdateRank Rpc Timeout."));
            }
            else if (0 != rpc8.ResultCode)
            {
                future9.TrySetException(new System.Exception($"Zeze.Game.Rank:UpdateRank Rpc Error {rpc8.ResultCode}."));
            }
            else
            {
                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(rpc8.Result.Params);
                long theResult10;
                theResult10 = _bb_.ReadLong();
                future9.TrySetResult(theResult10);
            }
            return System.Threading.Tasks.Task.FromResult(Zeze.Util.ResultCode.Success);
        });

        return await future9.Task;
    }

    public Redirect_Zeze_Game_Rank(Zeze.AppBase app) : base(app)
    {
        var hName5 = new Zeze.Arch.RedirectHandle();
        hName5.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName5.RequestHandle = async (_sessionId_, _hash_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            Zeze.Builtin.Game.Rank.BConcurrentKey key = new Zeze.Builtin.Game.Rank.BConcurrentKey();
            key.Decode(_bb_);
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            var asyncResult6 = await base.GetRankAll(_hash_, key);
            asyncResult6.Encode(_bb_);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Zeze.Game.Rank:GetRankAll", hName5);

        var hName11 = new Zeze.Arch.RedirectHandle();
        hName11.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName11.RequestHandle = async (_sessionId_, _HashOrServerId_, _params_) =>
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
            var asyncResult12 = await base.UpdateRank(_HashOrServerId_, keyHint, roleId, value, valueEx);
            _bb_.WriteLong(asyncResult12);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Zeze.Game.Rank:UpdateRank", hName11);
    }

}
