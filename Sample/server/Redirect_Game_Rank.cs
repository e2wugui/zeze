using Zeze.Util;

public class Redirect_Game_Rank : Game.Rank.ModuleRank
{
    public override async System.Threading.Tasks.Task<Zeze.Arch.RedirectAll<Game.Rank.BRankList>> GetRankAll(Game.Rank.BConcurrentKey keyHint)
    {
        // RedirectAll
        var reqall1 = new Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest();
        reqall1.Argument.ModuleId = 9;
        reqall1.Argument.HashCodeConcurrentLevel = GetConcurrentLevel(keyHint.RankType);
        // reqall1.Argument.HashCodes = // setup in linkd;
        reqall1.Argument.MethodFullName = "Game.Rank:GetRankAll";
        reqall1.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            keyHint.Encode(_bb_);
            reqall1.Argument.Params = new Zeze.Net.Binary(_bb_);
        }
        var decoder2 = (Zeze.Net.Binary _params_) => 
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            Game.Rank.BRankList result3 = new Game.Rank.BRankList();
            result3.Decode(_bb_);
            return result3;
        };
        var ctxall4 = new Zeze.Arch.RedirectAll<Game.Rank.BRankList>(
            reqall1.Argument.HashCodeConcurrentLevel, reqall1.Argument.MethodFullName,
            decoder2);
        reqall1.Argument.SessionId = App.Zeze.Redirect.ProviderApp.ProviderDirectService.AddManualContextWithTimeout(ctxall4);

        App.Zeze.Redirect.RedirectAll(this, reqall1);

        await ctxall4.Future.Task;
        return ctxall4;
    }

    public override async System.Threading.Tasks.Task<Zeze.Arch.RedirectAll> TestAllNoResult(int param)
    {
        // RedirectAll
        var reqall7 = new Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest();
        reqall7.Argument.ModuleId = 9;
        reqall7.Argument.HashCodeConcurrentLevel = 100;
        // reqall7.Argument.HashCodes = // setup in linkd;
        reqall7.Argument.MethodFullName = "Game.Rank:TestAllNoResult";
        reqall7.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteInt(param);
            reqall7.Argument.Params = new Zeze.Net.Binary(_bb_);
        }
        var decoder8 = Zeze.Arch.Gen.VoidResult.Decoder;
        var ctxall9 = new Zeze.Arch.RedirectAll(
            reqall7.Argument.HashCodeConcurrentLevel, reqall7.Argument.MethodFullName,
            decoder8);
        reqall7.Argument.SessionId = App.Zeze.Redirect.ProviderApp.ProviderDirectService.AddManualContextWithTimeout(ctxall9);

        App.Zeze.Redirect.RedirectAll(this, reqall7);

        await ctxall9.Future.Task;
        return ctxall9;
    }

    public override async System.Threading.Tasks.Task<Zeze.Arch.RedirectAll<long>> TestAllResult(int param)
    {
        // RedirectAll
        var reqall11 = new Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest();
        reqall11.Argument.ModuleId = 9;
        reqall11.Argument.HashCodeConcurrentLevel = 100;
        // reqall11.Argument.HashCodes = // setup in linkd;
        reqall11.Argument.MethodFullName = "Game.Rank:TestAllResult";
        reqall11.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteInt(param);
            reqall11.Argument.Params = new Zeze.Net.Binary(_bb_);
        }
        var decoder12 = (Zeze.Net.Binary _params_) => 
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            long result13;
            result13 = _bb_.ReadLong();
            return result13;
        };
        var ctxall14 = new Zeze.Arch.RedirectAll<long>(
            reqall11.Argument.HashCodeConcurrentLevel, reqall11.Argument.MethodFullName,
            decoder12);
        reqall11.Argument.SessionId = App.Zeze.Redirect.ProviderApp.ProviderDirectService.AddManualContextWithTimeout(ctxall14);

        App.Zeze.Redirect.RedirectAll(this, reqall11);

        await ctxall14.Future.Task;
        return ctxall14;
    }

    public override async System.Threading.Tasks.Task<Zeze.Arch.RedirectAll<Game.Rank.ModuleRank.MyResult>> TestAllResultProcessing(int param, System.Func<Zeze.Arch.RedirectAll<Game.Rank.ModuleRank.MyResult>, System.Threading.Tasks.Task> processing)
    {
        // RedirectAll
        var reqall19 = new Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest();
        reqall19.Argument.ModuleId = 9;
        reqall19.Argument.HashCodeConcurrentLevel = 100;
        // reqall19.Argument.HashCodes = // setup in linkd;
        reqall19.Argument.MethodFullName = "Game.Rank:TestAllResultProcessing";
        reqall19.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteInt(param);
            reqall19.Argument.Params = new Zeze.Net.Binary(_bb_);
        }
        var decoder20 = (Zeze.Net.Binary _params_) => 
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            Game.Rank.ModuleRank.MyResult result21 = default(Game.Rank.ModuleRank.MyResult);
            var tmp22 = _bb_.ReadByteBuffer();
            var tmp23 = new System.ReadOnlySpan<byte>(tmp22.Bytes, tmp22.ReadIndex, tmp22.Size);
            result21 = System.Text.Json.JsonSerializer.Deserialize<Game.Rank.ModuleRank.MyResult> (tmp23);
            return result21;
        };
        var ctxall24 = new Zeze.Arch.RedirectAll<Game.Rank.ModuleRank.MyResult>(
            reqall19.Argument.HashCodeConcurrentLevel, reqall19.Argument.MethodFullName,
            decoder20, processing);
        reqall19.Argument.SessionId = App.Zeze.Redirect.ProviderApp.ProviderDirectService.AddManualContextWithTimeout(ctxall24);

        App.Zeze.Redirect.RedirectAll(this, reqall19);

        await ctxall24.Future.Task;
        return ctxall24;
    }

    public override async System.Threading.Tasks.Task TestHash(int hash, int param, System.Action<int, int> result)
    {
        // RedirectHash
        var _target_ = App.Zeze.Redirect.ChoiceHash(this, hash, 1);
        if (_target_ == null) {
            // local: loop-back
            await App.Zeze.NewProcedure(async () => { await base.TestHash(hash, param, result); return 0; }, "Game.Rank:TestHash").ExecuteAsync();
            return;
        }

        var rpc29 = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        rpc29.Argument.ModuleId = 9;
        rpc29.Argument.RedirectType = Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeWithHash;
        rpc29.Argument.HashCode = hash;
        rpc29.Argument.MethodFullName = "Game.Rank:TestHash";
        rpc29.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteInt(param);
            rpc29.Argument.Params = new Zeze.Net.Binary(_bb_);
        }

        var future30 = new System.Threading.Tasks.TaskCompletionSource();

        rpc29.Send(_target_, async (_) =>
        {
            if (rpc29.IsTimeout)
            {
                future30.TrySetException(new System.Exception("Game.Rank:TestHash Rpc Timeout."));
            }
            else if (0 != rpc29.ResultCode)
            {
                future30.TrySetException(new System.Exception($"Game.Rank:TestHash Rpc Error {rpc29.ResultCode}."));
            }
            else
            {
                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(rpc29.Result.Params);
                int tmp31;
                tmp31 = _bb_.ReadInt();
                int tmp32;
                tmp32 = _bb_.ReadInt();
                await App.Zeze.NewProcedure(async () => { result(tmp31, tmp32); return 0L; }, "ModuleRedirectResponse Procedure").CallAsync();
                future30.TrySetResult();
            }
            return ResultCode.Success;
        });

        await future30.Task;
    }

    public override void TestHashNoWait(int hash, int param)
    {
        // RedirectHash
        var _target_ = App.Zeze.Redirect.ChoiceHash(this, hash, 1);
        if (_target_ == null) {
            // local: loop-back
            App.Zeze.NewProcedure(async () => { base.TestHashNoWait(hash, param); return 0; }, "Game.Rank:TestHashNoWait").Execute();
            return;
        }

        var rpc36 = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        rpc36.Argument.ModuleId = 9;
        rpc36.Argument.RedirectType = Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeWithHash;
        rpc36.Argument.HashCode = hash;
        rpc36.Argument.MethodFullName = "Game.Rank:TestHashNoWait";
        rpc36.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteInt(param);
            rpc36.Argument.Params = new Zeze.Net.Binary(_bb_);
        }

        var future37 = new System.Threading.Tasks.TaskCompletionSource();

        rpc36.Send(_target_, async (_) =>
        {
            if (rpc36.IsTimeout)
            {
                future37.TrySetException(new System.Exception("Game.Rank:TestHashNoWait Rpc Timeout."));
            }
            else if (0 != rpc36.ResultCode)
            {
                future37.TrySetException(new System.Exception($"Game.Rank:TestHashNoWait Rpc Error {rpc36.ResultCode}."));
            }
            else
            {
                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(rpc36.Result.Params);
                future37.TrySetResult();
            }
            return ResultCode.Success;
        });

    }

    public override async System.Threading.Tasks.Task<long> TestHashResult(int hash, int param, System.Action<int, int> result)
    {
        // RedirectHash
        var _target_ = App.Zeze.Redirect.ChoiceHash(this, hash, 1);
        if (_target_ == null) {
            // local: loop-back
            var returnResult41 = default(long);
            await App.Zeze.NewProcedure(async () => { returnResult41 = await base.TestHashResult(hash, param, result); return 0; }, "Game.Rank:TestHashResult").ExecuteAsync();
            return returnResult41;
        }

        var rpc42 = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        rpc42.Argument.ModuleId = 9;
        rpc42.Argument.RedirectType = Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeWithHash;
        rpc42.Argument.HashCode = hash;
        rpc42.Argument.MethodFullName = "Game.Rank:TestHashResult";
        rpc42.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteInt(param);
            rpc42.Argument.Params = new Zeze.Net.Binary(_bb_);
        }

        var future43 = new System.Threading.Tasks.TaskCompletionSource<long>();

        rpc42.Send(_target_, async (_) =>
        {
            if (rpc42.IsTimeout)
            {
                future43.TrySetException(new System.Exception("Game.Rank:TestHashResult Rpc Timeout."));
            }
            else if (0 != rpc42.ResultCode)
            {
                future43.TrySetException(new System.Exception($"Game.Rank:TestHashResult Rpc Error {rpc42.ResultCode}."));
            }
            else
            {
                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(rpc42.Result.Params);
                int tmp44;
                tmp44 = _bb_.ReadInt();
                int tmp45;
                tmp45 = _bb_.ReadInt();
                await App.Zeze.NewProcedure(async () => { result(tmp44, tmp45); return 0L; }, "ModuleRedirectResponse Procedure").CallAsync();
                long theResult46;
                theResult46 = _bb_.ReadLong();
                future43.TrySetResult(theResult46);
            }
            return ResultCode.Success;
        });

        return await future43.Task;
    }

    public override async System.Threading.Tasks.Task TestToServer(int serverId, int param, System.Action<int, int> result)
    {
        // RedirectToServer
        var _target_ = App.Zeze.Redirect.ChoiceServer(this, serverId);
        if (_target_ == null) {
            // local: loop-back
            await App.Zeze.NewProcedure(async () => { await base.TestToServer(serverId, param, result); return 0; }, "Game.Rank:TestToServer").ExecuteAsync();
            return;
        }

        var rpc53 = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        rpc53.Argument.ModuleId = 9;
        rpc53.Argument.RedirectType = Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeToServer;
        rpc53.Argument.HashCode = serverId;
        rpc53.Argument.MethodFullName = "Game.Rank:TestToServer";
        rpc53.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteInt(param);
            rpc53.Argument.Params = new Zeze.Net.Binary(_bb_);
        }

        var future54 = new System.Threading.Tasks.TaskCompletionSource();

        rpc53.Send(_target_, async (_) =>
        {
            if (rpc53.IsTimeout)
            {
                future54.TrySetException(new System.Exception("Game.Rank:TestToServer Rpc Timeout."));
            }
            else if (0 != rpc53.ResultCode)
            {
                future54.TrySetException(new System.Exception($"Game.Rank:TestToServer Rpc Error {rpc53.ResultCode}."));
            }
            else
            {
                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(rpc53.Result.Params);
                int tmp55;
                tmp55 = _bb_.ReadInt();
                int tmp56;
                tmp56 = _bb_.ReadInt();
                await App.Zeze.NewProcedure(async () => { result(tmp55, tmp56); return 0L; }, "ModuleRedirectResponse Procedure").CallAsync();
                future54.TrySetResult();
            }
            return ResultCode.Success;
        });

        await future54.Task;
    }

    public override void TestToServerNoWait(int serverId, System.Action<int, int> result, int param)
    {
        // RedirectToServer
        var _target_ = App.Zeze.Redirect.ChoiceServer(this, serverId);
        if (_target_ == null) {
            // local: loop-back
            App.Zeze.NewProcedure(async () => { base.TestToServerNoWait(serverId, result, param); return 0; }, "Game.Rank:TestToServerNoWait").Execute();
            return;
        }

        var rpc62 = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        rpc62.Argument.ModuleId = 9;
        rpc62.Argument.RedirectType = Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeToServer;
        rpc62.Argument.HashCode = serverId;
        rpc62.Argument.MethodFullName = "Game.Rank:TestToServerNoWait";
        rpc62.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteInt(param);
            rpc62.Argument.Params = new Zeze.Net.Binary(_bb_);
        }

        var future63 = new System.Threading.Tasks.TaskCompletionSource();

        rpc62.Send(_target_, async (_) =>
        {
            if (rpc62.IsTimeout)
            {
                future63.TrySetException(new System.Exception("Game.Rank:TestToServerNoWait Rpc Timeout."));
            }
            else if (0 != rpc62.ResultCode)
            {
                future63.TrySetException(new System.Exception($"Game.Rank:TestToServerNoWait Rpc Error {rpc62.ResultCode}."));
            }
            else
            {
                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(rpc62.Result.Params);
                int tmp64;
                tmp64 = _bb_.ReadInt();
                int tmp65;
                tmp65 = _bb_.ReadInt();
                await App.Zeze.NewProcedure(async () => { result(tmp64, tmp65); return 0L; }, "ModuleRedirectResponse Procedure").CallAsync();
                future63.TrySetResult();
            }
            return ResultCode.Success;
        });

    }

    public override async System.Threading.Tasks.Task<long> TestToServerResult(int serverId, int param, System.Action<int, int> result)
    {
        // RedirectToServer
        var _target_ = App.Zeze.Redirect.ChoiceServer(this, serverId);
        if (_target_ == null) {
            // local: loop-back
            var returnResult71 = default(long);
            await App.Zeze.NewProcedure(async () => { returnResult71 = await base.TestToServerResult(serverId, param, result); return 0; }, "Game.Rank:TestToServerResult").ExecuteAsync();
            return returnResult71;
        }

        var rpc72 = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        rpc72.Argument.ModuleId = 9;
        rpc72.Argument.RedirectType = Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeToServer;
        rpc72.Argument.HashCode = serverId;
        rpc72.Argument.MethodFullName = "Game.Rank:TestToServerResult";
        rpc72.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteInt(param);
            rpc72.Argument.Params = new Zeze.Net.Binary(_bb_);
        }

        var future73 = new System.Threading.Tasks.TaskCompletionSource<long>();

        rpc72.Send(_target_, async (_) =>
        {
            if (rpc72.IsTimeout)
            {
                future73.TrySetException(new System.Exception("Game.Rank:TestToServerResult Rpc Timeout."));
            }
            else if (0 != rpc72.ResultCode)
            {
                future73.TrySetException(new System.Exception($"Game.Rank:TestToServerResult Rpc Error {rpc72.ResultCode}."));
            }
            else
            {
                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(rpc72.Result.Params);
                int tmp74;
                tmp74 = _bb_.ReadInt();
                int tmp75;
                tmp75 = _bb_.ReadInt();
                await App.Zeze.NewProcedure(async () => { result(tmp74, tmp75); return 0L; }, "ModuleRedirectResponse Procedure").CallAsync();
                long theResult76;
                theResult76 = _bb_.ReadLong();
                future73.TrySetResult(theResult76);
            }
            return ResultCode.Success;
        });

        return await future73.Task;
    }

    protected override async System.Threading.Tasks.Task<long> UpdateRank(int hash, Game.Rank.BConcurrentKey keyHint, long roleId, long value, Zeze.Net.Binary valueEx)
    {
        // RedirectHash
        var _target_ = App.Zeze.Redirect.ChoiceHash(this, hash, GetConcurrentLevel(keyHint.RankType));
        if (_target_ == null) {
            // local: loop-back
            var returnResult81 = default(long);
            await App.Zeze.NewProcedure(async () => { returnResult81 = await base.UpdateRank(hash, keyHint, roleId, value, valueEx); return 0; }, "Game.Rank:UpdateRank").ExecuteAsync();
            return returnResult81;
        }

        var rpc82 = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        rpc82.Argument.ModuleId = 9;
        rpc82.Argument.RedirectType = Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeWithHash;
        rpc82.Argument.HashCode = hash;
        rpc82.Argument.MethodFullName = "Game.Rank:UpdateRank";
        rpc82.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            keyHint.Encode(_bb_);
            _bb_.WriteLong(roleId);
            _bb_.WriteLong(value);
            _bb_.WriteBinary(valueEx);
            rpc82.Argument.Params = new Zeze.Net.Binary(_bb_);
        }

        var future83 = new System.Threading.Tasks.TaskCompletionSource<long>();

        rpc82.Send(_target_, async (_) =>
        {
            if (rpc82.IsTimeout)
            {
                future83.TrySetException(new System.Exception("Game.Rank:UpdateRank Rpc Timeout."));
            }
            else if (0 != rpc82.ResultCode)
            {
                future83.TrySetException(new System.Exception($"Game.Rank:UpdateRank Rpc Error {rpc82.ResultCode}."));
            }
            else
            {
                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(rpc82.Result.Params);
                long theResult84;
                theResult84 = _bb_.ReadLong();
                future83.TrySetResult(theResult84);
            }
            return ResultCode.Success;
        });

        return await future83.Task;
    }

    public Redirect_Game_Rank(Game.App app) : base(app)
    {
        var hName5 = new Zeze.Arch.RedirectHandle();
        hName5.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName5.RequestHandle = async (_sessionId_, _hash_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            Game.Rank.BConcurrentKey keyHint = new Game.Rank.BConcurrentKey();
            keyHint.Decode(_bb_);
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            var asyncResult6 = await base.GetRankAll(_hash_, keyHint);
            asyncResult6.Encode(_bb_);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:GetRankAll", hName5);

        var hName10 = new Zeze.Arch.RedirectHandle();
        hName10.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName10.RequestHandle = async (_sessionId_, _hash_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            int param;
            param = _bb_.ReadInt();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            await base.TestAllNoResult(_hash_, param);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:TestAllNoResult", hName10);

        var hName15 = new Zeze.Arch.RedirectHandle();
        hName15.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName15.RequestHandle = async (_sessionId_, _hash_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            int param;
            param = _bb_.ReadInt();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            var asyncResult16 = await base.TestAllResult(_hash_, param);
            _bb_.WriteLong(asyncResult16);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:TestAllResult", hName15);

        var hName25 = new Zeze.Arch.RedirectHandle();
        hName25.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName25.RequestHandle = async (_sessionId_, _hash_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            int param;
            param = _bb_.ReadInt();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            var asyncResult26 = await base.TestAllResultProcessing(_hash_, param);
            _bb_.WriteBytes(System.Text.Json.JsonSerializer.SerializeToUtf8Bytes(asyncResult26, typeof(Game.Rank.ModuleRank.MyResult)));
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:TestAllResultProcessing", hName25);

        var hName33 = new Zeze.Arch.RedirectHandle();
        hName33.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName33.RequestHandle = async (_sessionId_, _HashOrServerId_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            int param;
            param = _bb_.ReadInt();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            System.Action<int, int> result = (tmp34, tmp35) =>
            {
                _bb_.WriteInt(tmp34);
                _bb_.WriteInt(tmp35);
            };
            await base.TestHash(_HashOrServerId_, param, result);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:TestHash", hName33);
        var hName38 = new Zeze.Arch.RedirectHandle();
        hName38.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName38.RequestHandle = async (_sessionId_, _HashOrServerId_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            int param;
            param = _bb_.ReadInt();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            base.TestHashNoWait(_HashOrServerId_, param);
            return Zeze.Net.Binary.Empty;
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:TestHashNoWait", hName38);
        var hName47 = new Zeze.Arch.RedirectHandle();
        hName47.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName47.RequestHandle = async (_sessionId_, _HashOrServerId_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            int param;
            param = _bb_.ReadInt();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            System.Action<int, int> result = (tmp48, tmp49) =>
            {
                _bb_.WriteInt(tmp48);
                _bb_.WriteInt(tmp49);
            };
            var asyncResult50 = await base.TestHashResult(_HashOrServerId_, param, result);
            _bb_.WriteLong(asyncResult50);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:TestHashResult", hName47);
        var hName57 = new Zeze.Arch.RedirectHandle();
        hName57.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName57.RequestHandle = async (_sessionId_, _HashOrServerId_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            int param;
            param = _bb_.ReadInt();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            System.Action<int, int> result = (tmp58, tmp59) =>
            {
                _bb_.WriteInt(tmp58);
                _bb_.WriteInt(tmp59);
            };
            await base.TestToServer(_HashOrServerId_, param, result);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:TestToServer", hName57);
        var hName66 = new Zeze.Arch.RedirectHandle();
        hName66.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName66.RequestHandle = async (_sessionId_, _HashOrServerId_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            int param;
            param = _bb_.ReadInt();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            System.Action<int, int> result = (tmp67, tmp68) =>
            {
                _bb_.WriteInt(tmp67);
                _bb_.WriteInt(tmp68);
            };
            base.TestToServerNoWait(_HashOrServerId_, result, param);
            return Zeze.Net.Binary.Empty;
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:TestToServerNoWait", hName66);
        var hName77 = new Zeze.Arch.RedirectHandle();
        hName77.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName77.RequestHandle = async (_sessionId_, _HashOrServerId_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            int param;
            param = _bb_.ReadInt();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            System.Action<int, int> result = (tmp78, tmp79) =>
            {
                _bb_.WriteInt(tmp78);
                _bb_.WriteInt(tmp79);
            };
            var asyncResult80 = await base.TestToServerResult(_HashOrServerId_, param, result);
            _bb_.WriteLong(asyncResult80);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:TestToServerResult", hName77);
        var hName85 = new Zeze.Arch.RedirectHandle();
        hName85.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName85.RequestHandle = async (_sessionId_, _HashOrServerId_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            Game.Rank.BConcurrentKey keyHint = new Game.Rank.BConcurrentKey();
            long roleId;
            long value;
            Zeze.Net.Binary valueEx;
            keyHint.Decode(_bb_);
            roleId = _bb_.ReadLong();
            value = _bb_.ReadLong();
            valueEx = _bb_.ReadBinary();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            var asyncResult86 = await base.UpdateRank(_HashOrServerId_, keyHint, roleId, value, valueEx);
            _bb_.WriteLong(asyncResult86);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:UpdateRank", hName85);
    }

}
