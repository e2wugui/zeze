public class Redirect_Game_Rank : Game.Rank.ModuleRank
{
    public override async System.Threading.Tasks.Task<Zeze.Arch.RedirectAll> TestAllNoResult(int param)
    {
        // RedirectAll
        var reqall1 = new Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest();
        reqall1.Argument.ModuleId = 9;
        reqall1.Argument.HashCodeConcurrentLevel = 100;
        // reqall1.Argument.HashCodes = // setup in linkd;
        reqall1.Argument.MethodFullName = "Game.Rank:TestAllNoResult";
        reqall1.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteInt(param);
            reqall1.Argument.Params = new Zeze.Net.Binary(_bb_);
        }

        var decoder2 = Zeze.Arch.Gen.VoidResult.Decoder;
        var ctxall3 = new Zeze.Arch.RedirectAll(
            reqall1.Argument.HashCodeConcurrentLevel, reqall1.Argument.MethodFullName,
            decoder2);
        reqall1.Argument.SessionId = App.Zeze.Redirect.ProviderApp.ProviderDirectService.AddManualContextWithTimeout(ctxall3);

        App.Zeze.Redirect.RedirectAll(this, reqall1);

        await ctxall3.Future.Task;
        return ctxall3;
    }

    public override async System.Threading.Tasks.Task<Zeze.Arch.RedirectAll<long>> TestAllResult(int param)
    {
        // RedirectAll
        var reqall5 = new Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest();
        reqall5.Argument.ModuleId = 9;
        reqall5.Argument.HashCodeConcurrentLevel = 100;
        // reqall5.Argument.HashCodes = // setup in linkd;
        reqall5.Argument.MethodFullName = "Game.Rank:TestAllResult";
        reqall5.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteInt(param);
            reqall5.Argument.Params = new Zeze.Net.Binary(_bb_);
        }
        var decoder6 = (Zeze.Net.Binary _params_) => 
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            long result7;
            result7 = _bb_.ReadLong();
            return result7;
        };
        var ctxall8 = new Zeze.Arch.RedirectAll<long>(
            reqall5.Argument.HashCodeConcurrentLevel, reqall5.Argument.MethodFullName,
            decoder6);
        reqall5.Argument.SessionId = App.Zeze.Redirect.ProviderApp.ProviderDirectService.AddManualContextWithTimeout(ctxall8);

        App.Zeze.Redirect.RedirectAll(this, reqall5);

        await ctxall8.Future.Task;
        return ctxall8;
    }

    public override async System.Threading.Tasks.Task<Zeze.Arch.RedirectAll<Game.Rank.ModuleRank.MyResult>> TestAllResultProcessing(int param, System.Func<Zeze.Arch.RedirectAll<Game.Rank.ModuleRank.MyResult>, System.Threading.Tasks.Task> processing)
    {
        // RedirectAll
        var reqall13 = new Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest();
        reqall13.Argument.ModuleId = 9;
        reqall13.Argument.HashCodeConcurrentLevel = 100;
        // reqall13.Argument.HashCodes = // setup in linkd;
        reqall13.Argument.MethodFullName = "Game.Rank:TestAllResultProcessing";
        reqall13.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteInt(param);
            reqall13.Argument.Params = new Zeze.Net.Binary(_bb_);
        }
        var decoder14 = (Zeze.Net.Binary _params_) => 
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            Game.Rank.ModuleRank.MyResult result15 = default(Game.Rank.ModuleRank.MyResult);
            var tmp16 = _bb_.ReadByteBuffer();
            var tmp17 = new System.ReadOnlySpan<byte>(tmp16.Bytes, tmp16.ReadIndex, tmp16.Size);
            result15 = System.Text.Json.JsonSerializer.Deserialize<Game.Rank.ModuleRank.MyResult> (tmp17);
            return result15;
        };
        var ctxall18 = new Zeze.Arch.RedirectAll<Game.Rank.ModuleRank.MyResult>(
            reqall13.Argument.HashCodeConcurrentLevel, reqall13.Argument.MethodFullName,
            decoder14, processing);
        reqall13.Argument.SessionId = App.Zeze.Redirect.ProviderApp.ProviderDirectService.AddManualContextWithTimeout(ctxall18);

        App.Zeze.Redirect.RedirectAll(this, reqall13);

        await ctxall18.Future.Task;
        return ctxall18;
    }

    public override async System.Threading.Tasks.Task TestHash(int hash, int param, System.Action<int, int> result)
    {
        // RedirectHash
        var _target_ = App.Zeze.Redirect.ChoiceHash(this, hash);
        if (_target_ == null) {
            // local: loop-back
            await App.Zeze.NewProcedure(async () => { await base.TestHash(hash, param, result); return 0; }, "Game.Rank:TestHash").ExecuteAsync();
            return;
        }

        var rpc23 = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        rpc23.Argument.ModuleId = 9;
        rpc23.Argument.RedirectType = Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeWithHash;
        rpc23.Argument.HashCode = hash;
        rpc23.Argument.MethodFullName = "Game.Rank:TestHash";
        rpc23.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteInt(param);
            rpc23.Argument.Params = new Zeze.Net.Binary(_bb_);
        }

        var future24 = new System.Threading.Tasks.TaskCompletionSource();

        rpc23.Send(_target_, async (_) =>
        {
            if (rpc23.IsTimeout)
            {
                future24.TrySetException(new System.Exception("Game.Rank:TestHash Rpc Timeout."));
            }
            else if (0 != rpc23.ResultCode)
            {
                future24.TrySetException(new System.Exception($"Game.Rank:TestHash Rpc Error {rpc23.ResultCode}."));
            }
            else
            {
                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(rpc23.Result.Params);
                int tmp25;
                tmp25 = _bb_.ReadInt();
                int tmp26;
                tmp26 = _bb_.ReadInt();
                await App.Zeze.NewProcedure(async () => { result(tmp25, tmp26); return 0L; }, "ModuleRedirectResponse Procedure").CallAsync();
                future24.TrySetResult();
            }
            return Zeze.Transaction.Procedure.Success;
        });

        await future24.Task;
    }

    public override void TestHashNoWait(int hash, int param)
    {
        // RedirectHash
        var _target_ = App.Zeze.Redirect.ChoiceHash(this, hash);
        if (_target_ == null) {
            // local: loop-back
            App.Zeze.NewProcedure(async () => { base.TestHashNoWait(hash, param); return 0; }, "Game.Rank:TestHashNoWait").Execute();
            return;
        }

        var rpc30 = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        rpc30.Argument.ModuleId = 9;
        rpc30.Argument.RedirectType = Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeWithHash;
        rpc30.Argument.HashCode = hash;
        rpc30.Argument.MethodFullName = "Game.Rank:TestHashNoWait";
        rpc30.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteInt(param);
            rpc30.Argument.Params = new Zeze.Net.Binary(_bb_);
        }

        var future31 = new System.Threading.Tasks.TaskCompletionSource();

        rpc30.Send(_target_, async (_) =>
        {
            if (rpc30.IsTimeout)
            {
                future31.TrySetException(new System.Exception("Game.Rank:TestHashNoWait Rpc Timeout."));
            }
            else if (0 != rpc30.ResultCode)
            {
                future31.TrySetException(new System.Exception($"Game.Rank:TestHashNoWait Rpc Error {rpc30.ResultCode}."));
            }
            else
            {
                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(rpc30.Result.Params);
                future31.TrySetResult();
            }
            return Zeze.Transaction.Procedure.Success;
        });

    }

    public override async System.Threading.Tasks.Task<long> TestHashResult(int hash, int param, System.Action<int, int> result)
    {
        // RedirectHash
        var _target_ = App.Zeze.Redirect.ChoiceHash(this, hash);
        if (_target_ == null) {
            // local: loop-back
            var returnResult35 = default(long);
            await App.Zeze.NewProcedure(async () => { returnResult35 = await base.TestHashResult(hash, param, result); return 0; }, "Game.Rank:TestHashResult").ExecuteAsync();
            return returnResult35;
        }

        var rpc36 = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        rpc36.Argument.ModuleId = 9;
        rpc36.Argument.RedirectType = Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeWithHash;
        rpc36.Argument.HashCode = hash;
        rpc36.Argument.MethodFullName = "Game.Rank:TestHashResult";
        rpc36.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteInt(param);
            rpc36.Argument.Params = new Zeze.Net.Binary(_bb_);
        }

        var future37 = new System.Threading.Tasks.TaskCompletionSource<long>();

        rpc36.Send(_target_, async (_) =>
        {
            if (rpc36.IsTimeout)
            {
                future37.TrySetException(new System.Exception("Game.Rank:TestHashResult Rpc Timeout."));
            }
            else if (0 != rpc36.ResultCode)
            {
                future37.TrySetException(new System.Exception($"Game.Rank:TestHashResult Rpc Error {rpc36.ResultCode}."));
            }
            else
            {
                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(rpc36.Result.Params);
                int tmp38;
                tmp38 = _bb_.ReadInt();
                int tmp39;
                tmp39 = _bb_.ReadInt();
                await App.Zeze.NewProcedure(async () => { result(tmp38, tmp39); return 0L; }, "ModuleRedirectResponse Procedure").CallAsync();
                long theResult40;
                theResult40 = _bb_.ReadLong();
                future37.TrySetResult(theResult40);
            }
            return Zeze.Transaction.Procedure.Success;
        });

        return await future37.Task;
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

        var rpc47 = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        rpc47.Argument.ModuleId = 9;
        rpc47.Argument.RedirectType = Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeToServer;
        rpc47.Argument.HashCode = serverId;
        rpc47.Argument.MethodFullName = "Game.Rank:TestToServer";
        rpc47.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteInt(param);
            rpc47.Argument.Params = new Zeze.Net.Binary(_bb_);
        }

        var future48 = new System.Threading.Tasks.TaskCompletionSource();

        rpc47.Send(_target_, async (_) =>
        {
            if (rpc47.IsTimeout)
            {
                future48.TrySetException(new System.Exception("Game.Rank:TestToServer Rpc Timeout."));
            }
            else if (0 != rpc47.ResultCode)
            {
                future48.TrySetException(new System.Exception($"Game.Rank:TestToServer Rpc Error {rpc47.ResultCode}."));
            }
            else
            {
                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(rpc47.Result.Params);
                int tmp49;
                tmp49 = _bb_.ReadInt();
                int tmp50;
                tmp50 = _bb_.ReadInt();
                await App.Zeze.NewProcedure(async () => { result(tmp49, tmp50); return 0L; }, "ModuleRedirectResponse Procedure").CallAsync();
                future48.TrySetResult();
            }
            return Zeze.Transaction.Procedure.Success;
        });

        await future48.Task;
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

        var rpc56 = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        rpc56.Argument.ModuleId = 9;
        rpc56.Argument.RedirectType = Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeToServer;
        rpc56.Argument.HashCode = serverId;
        rpc56.Argument.MethodFullName = "Game.Rank:TestToServerNoWait";
        rpc56.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteInt(param);
            rpc56.Argument.Params = new Zeze.Net.Binary(_bb_);
        }

        var future57 = new System.Threading.Tasks.TaskCompletionSource();

        rpc56.Send(_target_, async (_) =>
        {
            if (rpc56.IsTimeout)
            {
                future57.TrySetException(new System.Exception("Game.Rank:TestToServerNoWait Rpc Timeout."));
            }
            else if (0 != rpc56.ResultCode)
            {
                future57.TrySetException(new System.Exception($"Game.Rank:TestToServerNoWait Rpc Error {rpc56.ResultCode}."));
            }
            else
            {
                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(rpc56.Result.Params);
                int tmp58;
                tmp58 = _bb_.ReadInt();
                int tmp59;
                tmp59 = _bb_.ReadInt();
                await App.Zeze.NewProcedure(async () => { result(tmp58, tmp59); return 0L; }, "ModuleRedirectResponse Procedure").CallAsync();
                future57.TrySetResult();
            }
            return Zeze.Transaction.Procedure.Success;
        });

    }

    public override async System.Threading.Tasks.Task<long> TestToServerResult(int serverId, int param, System.Action<int, int> result)
    {
        // RedirectToServer
        var _target_ = App.Zeze.Redirect.ChoiceServer(this, serverId);
        if (_target_ == null) {
            // local: loop-back
            var returnResult65 = default(long);
            await App.Zeze.NewProcedure(async () => { returnResult65 = await base.TestToServerResult(serverId, param, result); return 0; }, "Game.Rank:TestToServerResult").ExecuteAsync();
            return returnResult65;
        }

        var rpc66 = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        rpc66.Argument.ModuleId = 9;
        rpc66.Argument.RedirectType = Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeToServer;
        rpc66.Argument.HashCode = serverId;
        rpc66.Argument.MethodFullName = "Game.Rank:TestToServerResult";
        rpc66.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            _bb_.WriteInt(param);
            rpc66.Argument.Params = new Zeze.Net.Binary(_bb_);
        }

        var future67 = new System.Threading.Tasks.TaskCompletionSource<long>();

        rpc66.Send(_target_, async (_) =>
        {
            if (rpc66.IsTimeout)
            {
                future67.TrySetException(new System.Exception("Game.Rank:TestToServerResult Rpc Timeout."));
            }
            else if (0 != rpc66.ResultCode)
            {
                future67.TrySetException(new System.Exception($"Game.Rank:TestToServerResult Rpc Error {rpc66.ResultCode}."));
            }
            else
            {
                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(rpc66.Result.Params);
                int tmp68;
                tmp68 = _bb_.ReadInt();
                int tmp69;
                tmp69 = _bb_.ReadInt();
                await App.Zeze.NewProcedure(async () => { result(tmp68, tmp69); return 0L; }, "ModuleRedirectResponse Procedure").CallAsync();
                long theResult70;
                theResult70 = _bb_.ReadLong();
                future67.TrySetResult(theResult70);
            }
            return Zeze.Transaction.Procedure.Success;
        });

        return await future67.Task;
    }

    protected override async System.Threading.Tasks.Task<long> UpdateRank(int hash, Game.Rank.BConcurrentKey keyHint, long roleId, long value, Zeze.Net.Binary valueEx)
    {
        // RedirectHash
        var _target_ = App.Zeze.Redirect.ChoiceHash(this, hash);
        if (_target_ == null) {
            // local: loop-back
            var returnResult75 = default(long);
            await App.Zeze.NewProcedure(async () => { returnResult75 = await base.UpdateRank(hash, keyHint, roleId, value, valueEx); return 0; }, "Game.Rank:UpdateRank").ExecuteAsync();
            return returnResult75;
        }

        var rpc76 = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        rpc76.Argument.ModuleId = 9;
        rpc76.Argument.RedirectType = Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeWithHash;
        rpc76.Argument.HashCode = hash;
        rpc76.Argument.MethodFullName = "Game.Rank:UpdateRank";
        rpc76.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();
            keyHint.Encode(_bb_);
            _bb_.WriteLong(roleId);
            _bb_.WriteLong(value);
            _bb_.WriteBinary(valueEx);
            rpc76.Argument.Params = new Zeze.Net.Binary(_bb_);
        }

        var future77 = new System.Threading.Tasks.TaskCompletionSource<long>();

        rpc76.Send(_target_, async (_) =>
        {
            if (rpc76.IsTimeout)
            {
                future77.TrySetException(new System.Exception("Game.Rank:UpdateRank Rpc Timeout."));
            }
            else if (0 != rpc76.ResultCode)
            {
                future77.TrySetException(new System.Exception($"Game.Rank:UpdateRank Rpc Error {rpc76.ResultCode}."));
            }
            else
            {
                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(rpc76.Result.Params);
                long theResult78;
                theResult78 = _bb_.ReadLong();
                future77.TrySetResult(theResult78);
            }
            return Zeze.Transaction.Procedure.Success;
        });

        return await future77.Task;
    }

    public Redirect_Game_Rank(Game.App app) : base(app)
    {
        var hName4 = new Zeze.Arch.RedirectHandle();
        hName4.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName4.RequestHandle = async (_sessionId_, _hash_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            int param;
            param = _bb_.ReadInt();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            await base.TestAllNoResult(_hash_, param);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:TestAllNoResult", hName4);

        var hName9 = new Zeze.Arch.RedirectHandle();
        hName9.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName9.RequestHandle = async (_sessionId_, _hash_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            int param;
            param = _bb_.ReadInt();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            var asyncResult10 = await base.TestAllResult(_hash_, param);
            _bb_.WriteLong(asyncResult10);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:TestAllResult", hName9);

        var hName19 = new Zeze.Arch.RedirectHandle();
        hName19.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName19.RequestHandle = async (_sessionId_, _hash_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            int param;
            param = _bb_.ReadInt();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            var asyncResult20 = await base.TestAllResultProcessing(_hash_, param);
            _bb_.WriteBytes(System.Text.Json.JsonSerializer.SerializeToUtf8Bytes(asyncResult20, typeof(Game.Rank.ModuleRank.MyResult)));
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:TestAllResultProcessing", hName19);

        var hName27 = new Zeze.Arch.RedirectHandle();
        hName27.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName27.RequestHandle = async (_sessionId_, _HashOrServerId_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            int param;
            param = _bb_.ReadInt();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            System.Action<int, int> result = (tmp28, tmp29) =>
            {
                _bb_.WriteInt(tmp28);
                _bb_.WriteInt(tmp29);
            };
            await base.TestHash(_HashOrServerId_, param, result);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:TestHash", hName27);
        var hName32 = new Zeze.Arch.RedirectHandle();
        hName32.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName32.RequestHandle = async (_sessionId_, _HashOrServerId_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            int param;
            param = _bb_.ReadInt();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            base.TestHashNoWait(_HashOrServerId_, param);
            return Zeze.Net.Binary.Empty;
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:TestHashNoWait", hName32);
        var hName41 = new Zeze.Arch.RedirectHandle();
        hName41.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName41.RequestHandle = async (_sessionId_, _HashOrServerId_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            int param;
            param = _bb_.ReadInt();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            System.Action<int, int> result = (tmp42, tmp43) =>
            {
                _bb_.WriteInt(tmp42);
                _bb_.WriteInt(tmp43);
            };
            var asyncResult44 = await base.TestHashResult(_HashOrServerId_, param, result);
            _bb_.WriteLong(asyncResult44);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:TestHashResult", hName41);
        var hName51 = new Zeze.Arch.RedirectHandle();
        hName51.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName51.RequestHandle = async (_sessionId_, _HashOrServerId_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            int param;
            param = _bb_.ReadInt();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            System.Action<int, int> result = (tmp52, tmp53) =>
            {
                _bb_.WriteInt(tmp52);
                _bb_.WriteInt(tmp53);
            };
            await base.TestToServer(_HashOrServerId_, param, result);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:TestToServer", hName51);
        var hName60 = new Zeze.Arch.RedirectHandle();
        hName60.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName60.RequestHandle = async (_sessionId_, _HashOrServerId_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            int param;
            param = _bb_.ReadInt();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            System.Action<int, int> result = (tmp61, tmp62) =>
            {
                _bb_.WriteInt(tmp61);
                _bb_.WriteInt(tmp62);
            };
            base.TestToServerNoWait(_HashOrServerId_, result, param);
            return Zeze.Net.Binary.Empty;
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:TestToServerNoWait", hName60);
        var hName71 = new Zeze.Arch.RedirectHandle();
        hName71.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName71.RequestHandle = async (_sessionId_, _HashOrServerId_, _params_) =>
        {
            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);
            int param;
            param = _bb_.ReadInt();
            // WARNING reuse var _bb_ to encode result.
            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);
            System.Action<int, int> result = (tmp72, tmp73) =>
            {
                _bb_.WriteInt(tmp72);
                _bb_.WriteInt(tmp73);
            };
            var asyncResult74 = await base.TestToServerResult(_HashOrServerId_, param, result);
            _bb_.WriteLong(asyncResult74);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:TestToServerResult", hName71);
        var hName79 = new Zeze.Arch.RedirectHandle();
        hName79.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.Serializable;
        hName79.RequestHandle = async (_sessionId_, _HashOrServerId_, _params_) =>
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
            var asyncResult80 = await base.UpdateRank(_HashOrServerId_, keyHint, roleId, value, valueEx);
            _bb_.WriteLong(asyncResult80);
            return new Zeze.Net.Binary(_bb_);
        };
        App.Zeze.Redirect.Handles.TryAdd("Game.Rank:UpdateRank", hName79);
    }

}
