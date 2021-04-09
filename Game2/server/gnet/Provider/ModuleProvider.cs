
using System;
using System.Collections.Generic;
using static Zeze.Net.Service;

namespace gnet.Provider
{
    public sealed partial class ModuleProvider : AbstractModule
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public void Start(Game.App app)
        {
        }

        public void Stop(Game.App app)
        {
        }

        private void SendKick(Zeze.Net.AsyncSocket sender, long linkSid, int code, string desc)
        {
            var p = new Kick();
            p.Argument.Linksid = linkSid;
            p.Argument.Code = code;
            p.Argument.Desc = desc;
            p.Send(sender);
        }

        public override int ProcessDispatch(Dispatch p)
        {
            try
            {
                var factoryHandle = Game.App.Instance.Server.FindProtocolFactoryHandle(p.Argument.ProtocolType);
                if (null == factoryHandle)
                {
                    SendKick(p.Sender, p.Argument.LinkSid, BKick.ErrorProtocolUnkown, "unknown protocol");
                    return Zeze.Transaction.Procedure.LogicError;
                }
                var p2 = factoryHandle.Factory();
                p2.Decode(Zeze.Serialize.ByteBuffer.Wrap(p.Argument.ProtocolData));
                p2.Sender = p.Sender;
                
                p2.UserState = new Game.Login.Session(p.Argument.UserId, p.Argument.States, p.Sender, p.Argument.LinkSid);
                if (Zeze.Transaction.Transaction.Current != null)
                {
                    // 已经在事务中，嵌入执行。此时忽略p2的NoProcedure配置。
                    Zeze.Transaction.Transaction.Current.TopProcedure.ActionName = p2.GetType().FullName;
                    Zeze.Transaction.Transaction.Current.TopProcedure.UserState = p2.UserState;
                    return factoryHandle.Handle(p2);
                }

                if (p2.Sender.Service.Zeze == null || factoryHandle.NoProcedure)
                {
                    // 应用框架不支持事务或者协议配置了“不需要事务”
                    return factoryHandle.Handle(p2);
                }

                // 创建存储过程并且在当前线程中调用。
                return p2.Sender.Service.Zeze.NewProcedure(() => factoryHandle.Handle(p2), p2.GetType().FullName, p2.UserState).Call();
            }
            catch (Exception ex)
            {
                SendKick(p.Sender, p.Argument.LinkSid, BKick.ErrorProtocolException, ex.ToString());
                throw;
            }
        }

        public override int ProcessLinkBroken(LinkBroken protocol)
        {
            // 目前仅需设置online状态。
            if (protocol.Argument.States.Count > 0)
            {
                var roleId = protocol.Argument.States[0];
                Game.App.Instance.Game_Login.Onlines.OnLinkBroken(roleId);
            }
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessModuleRedirectRequest(ModuleRedirect rpc)
        {
            try
            {
                // replace RootProcedure.ActionName. 为了统计和日志输出。
                Zeze.Transaction.Transaction.Current.TopProcedure.ActionName = rpc.Argument.MethodFullName;

                rpc.Result.ModuleId = rpc.Argument.ModuleId;
                rpc.Result.AutoKeyLocalId = App.Zeze.Config.AutoKeyLocalId;
                if (false == Game.ModuleRedirect.Instance.Handles.TryGetValue(rpc.Argument.MethodFullName, out var handle))
                {
                    rpc.SendResultCode(ModuleRedirect.ResultCodeMethodFullNameNotFound);
                    return Zeze.Transaction.Procedure.LogicError;
                }
                var (ReturnCode, Params) = handle(rpc.SessionId, rpc.Argument.HashCode, rpc.Argument.Params, rpc.Result.Actions);
                rpc.Result.ReturnCode = ReturnCode;
                if (ReturnCode == Zeze.Transaction.Procedure.Success)
                {
                    rpc.Result.Params = Params;
                }
                // rpc 成功了，具体handle结果还需要看ReturnCode。
                rpc.SendResultCode(ModuleRedirect.ResultCodeSuccess);
                return rpc.Result.ReturnCode;
            }
            catch (Exception)
            {
                rpc.SendResultCode(ModuleRedirect.ResultCodeHandleException);
                throw;
            }
        }

        private void SendResultIfSizeExceed(Zeze.Net.AsyncSocket sender, ModuleRedirectAllResult result)
        {
            int size = 0;
            foreach (var hashResult in result.Argument.Hashs.Values2)
            {
                size += hashResult.Params.Count;
                foreach (var hashActions in hashResult.Actions)
                {
                    size += hashActions.Params.Count;
                }
            }
            if (size > 2 * 1024 * 1024) // 2M
            {
                result.Send(sender);
                result.Argument.Hashs.Clear();
            }
        }

        public override int ProcessModuleRedirectAllRequest(ModuleRedirectAllRequest protocol)
        {
            var result = new ModuleRedirectAllResult();
            try
            {
                // replace RootProcedure.ActionName. 为了统计和日志输出。
                Zeze.Transaction.Transaction.Current.TopProcedure.ActionName = protocol.Argument.MethodFullName;

                // common parameters for result
                result.Argument.ModuleId = protocol.Argument.ModuleId;
                result.Argument.AutoKeyLocalId = App.Zeze.Config.AutoKeyLocalId;
                result.Argument.SourceProvider = protocol.Argument.SourceProvider;
                result.Argument.SessionId = protocol.Argument.SessionId;
                result.Argument.MethodFullName = protocol.Argument.MethodFullName;

                if (false == Game.ModuleRedirect.Instance.Handles.TryGetValue(
                    protocol.Argument.MethodFullName, out var handle))
                {
                    result.ResultCode = ModuleRedirect.ResultCodeMethodFullNameNotFound;
                    // 失败了，需要把hash返回。此时是没有处理结果的。
                    foreach (var hash in protocol.Argument.HashCodes)
                    {
                        result.Argument.Hashs.Add(hash, new BModuleRedirectAllHash()
                        {
                            ReturnCode = Zeze.Transaction.Procedure.NotImplement
                        });
                    }
                    result.Send(protocol.Sender);
                    return Zeze.Transaction.Procedure.LogicError;
                }
                result.ResultCode = ModuleRedirect.ResultCodeSuccess;

                foreach (var hash in protocol.Argument.HashCodes)
                {
                    // 嵌套存储过程，某个分组处理失败不影响其他分组。
                    var hashResult = new BModuleRedirectAllHash();
                    Zeze.Net.Binary Params = null;
                    hashResult.ReturnCode = App.Zeze.NewProcedure(() =>
                    {
                        var (_ReturnCode, _Params) = handle(protocol.Argument.SessionId, hash, protocol.Argument.Params, hashResult.Actions);
                        Params = _Params;
                        return _ReturnCode;
                    }, Zeze.Transaction.Transaction.Current.TopProcedure.ActionName).Call();

                    // 单个分组处理失败继续执行。XXX
                    if (hashResult.ReturnCode == Zeze.Transaction.Procedure.Success)
                    {
                        hashResult.Params = Params;
                    }
                    result.Argument.Hashs.Add(hash, hashResult);
                    SendResultIfSizeExceed(protocol.Sender, result);
                }

                // send remain
                if (result.Argument.Hashs.Count > 0)
                {
                    result.Send(protocol.Sender);
                }
                return Zeze.Transaction.Procedure.Success;
            }
            catch (Exception)
            {
                result.ResultCode = ModuleRedirect.ResultCodeHandleException;
                result.Send(protocol.Sender);
                throw;
            }
        }

        public class ModuleRedirectAllContext : Zeze.Net.Service.ManualContext
        {
            public string MethodFullName { get; }
            public HashSet<int> HashCodes { get; } = new HashSet<int>();
            public Action<ModuleRedirectAllContext> OnHashEnd { get; set; }

            public ModuleRedirectAllContext(int concurrentLevel, string methodFullName)
            {
                for (int hash = 0; hash < concurrentLevel; ++hash)
                    HashCodes.Add(hash);
                MethodFullName = methodFullName;
            }

            public override void OnRemoved()
            {
                lock (this)
                {
                    OnHashEnd?.Invoke(this);
                }
            }

            /// <summary>
            /// 调用这个方法处理hash分组结果，真正的处理代码在action中实现。
            /// 1) 在锁内执行；
            /// 2) 需要时初始化UserState并传给action；
            /// 3) 处理完成时删除Context
            /// </summary>
            public int ProcessHash<T>(int hash, Func<T> factory, Func<T, int> action)
            {
                lock (this)
                {
                    try
                    {
                        if (null == UserState)
                            UserState = factory();
                        return action((T)UserState);
                    }
                    finally
                    {
                        HashCodes.Remove(hash); // 如果不允许一个hash分组处理措辞，把这个移到开头并判断结果。
                        if (HashCodes.Count == 0)
                        {
                            Game.App.Instance.Server.TryRemoveManualContext<ManualContext>(SessionId);
                        }
                    }
                }
            }

            // 这里处理真正redirect发生时，从远程返回的结果。
            public void ProcessResult(ModuleRedirectAllResult result)
            {
                foreach (var h in result.Argument.Hashs)
                {
                    // 嵌套存储过程，单个分组的结果处理不影响其他分组。
                    // 不判断单个分组的处理结果，错误也继续执行其他分组。XXX
                    Game.App.Instance.Zeze.NewProcedure(() =>
                        ProcessHashResult(h.Key, h.Value.ReturnCode, h.Value.Params, h.Value.Actions),
                        MethodFullName).Call();
                }
            }

            // 生成代码实现。see Game.ModuleRedirect.cs
            public virtual int ProcessHashResult(int _hash_, int _returnCode_, Zeze.Net.Binary _params, IList<gnet.Provider.BActionParam> _actions_)
            {
                return Zeze.Transaction.Procedure.NotImplement;
            }
        }

        public override int ProcessModuleRedirectAllResult(ModuleRedirectAllResult protocol)
        {
            // replace RootProcedure.ActionName. 为了统计和日志输出。
            Zeze.Transaction.Transaction.Current.TopProcedure.ActionName = protocol.Argument.MethodFullName;
            App.Server.TryGetManualContext<ModuleRedirectAllContext>(protocol.Argument.SessionId)?.ProcessResult(protocol);
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessTransmit(Transmit protocol)
        {
            App.Game_Login.Onlines.ProcessTransmit(protocol.Argument.Sender,
                protocol.Argument.ActionName, protocol.Argument.Roles.Keys2);
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessAnnounceLinkInfo(AnnounceLinkInfo protocol)
        {
            var linkSession = protocol.Sender.UserState as Game.Server.LinkSession;
            linkSession.Setup(protocol.Argument.LinkId, protocol.Argument.ProviderSessionId);
            return Zeze.Transaction.Procedure.Success;
        }
    }
}
