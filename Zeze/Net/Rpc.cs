using System;
using System.Threading.Tasks;
using Zeze.Serialize;
using Zeze.Util;

namespace Zeze.Net
{
    public interface Rpc : Serializable
    {
#if USE_CONFCS
        ConfBean ResultBean { get; }
#else
        Transaction.Bean ResultBean { get; }
#endif

        Binary ResultEncoded { get; set; } // 如果设置了这个，发送结果的时候，优先使用这个编码过的。
        int FamilyClass { get; }
        bool IsTimeout { get; }
        long SessionId { get; set; }
        Func<Protocol, Task<long>> ResponseHandle { get; set; }
        int Timeout { get; set; }

        bool Send(AsyncSocket so);
        bool Send(AsyncSocket so, Func<Protocol, Task<long>> responseHandle, int millisecondsTimeout = 5000);

        void SendReturnVoid(Service service, AsyncSocket so, Func<Protocol, Task<long>> responseHandle,
            int millisecondsTimeout = 5000);

        Task SendAsync(AsyncSocket so, int millisecondsTimeout = 5000);
        Task SendAndCheckResultCodeAsync(AsyncSocket so, int millisecondsTimeout = 5000);

        void SendResult(Binary result = null);
        bool TrySendResultCode(long code);
        void ClearParameters(ProtocolPool.ReuseLevel level);
    }

    public abstract class Rpc<TArgument, TResult> : Protocol<TArgument>, Rpc
        where TArgument : Serializable, new()
        where TResult : Serializable, new()
    {
        private static readonly ILogger logger = LogManager.GetLogger(typeof(Rpc));

        public TResult Result { get; set; } = new TResult();

#if USE_CONFCS
        public override ConfBean ResultBean => Result as ConfBean;
#else
        public override Transaction.Bean ResultBean => Result as Transaction.Bean;
#endif
        public Binary ResultEncoded { get; set; } // 如果设置了这个，发送结果的时候，优先使用这个编码过的。
        public override int FamilyClass => IsRequest ? Zeze.Net.FamilyClass.Request : Zeze.Net.FamilyClass.Response;
        public bool IsTimeout { get; internal set; }
        public long SessionId { get; set; }

        public Func<Protocol, Task<long>> ResponseHandle { get; set; }
        public int Timeout { get; set; } = 5000;

        public TaskCompletionSource<TResult> Future { get; internal set; }

        /// <summary>
        /// 使用当前 rpc 中设置的参数发送。
        /// 总是建立上下文，总是返回true。
        /// 这个方法是 Protocol 的重载。
        /// 用于不需要处理结果的请求
        /// 或者重新发送已经设置过 ResponseHandle 等的请求。
        /// </summary>
        /// <param name="so"></param>
        /// <returns></returns>
        public override bool Send(AsyncSocket so)
        {
            return Send(so, ResponseHandle, Timeout);
        }

        private static SchedulerTask Schedule(Service service, long sessionId, int millisecondsTimeout)
        {
            return Scheduler.Schedule(_ =>
                {
                    var context = service.RemoveRpcContext<Rpc<TArgument, TResult>>(sessionId);
                    if (context == null) // 一般来说，此时结果已经返回。
                        return;

                    context.IsTimeout = true;
                    context.ResultCode = Zeze.Util.ResultCode.Timeout;

                    if (context.Future != null)
                        context.Future.TrySetException(new RpcTimeoutException());
                    else if (context.ResponseHandle != null)
                    {
                        // 本来Schedule已经在Task中执行了，这里又派发一次。
                        // 主要是为了让应用能拦截修改Response的处理方式。
                        // Timeout 应该是少的，先这样了。
                        var factoryHandle = service.FindProtocolFactoryHandle(context.TypeId);
                        if (factoryHandle != null)
                            service.DispatchRpcResponse(context, context.ResponseHandle, factoryHandle);
                    }
                },
                millisecondsTimeout);
        }

        /// <summary>
        /// 异步发送rpc请求。
        /// 1. 如果返回true，表示请求已经发送，并且建立好了上下文。
        /// 2. 如果返回false，请求没有发送成功，上下文也没有保留。
        /// </summary>
        /// <param name="so"></param>
        /// <param name="responseHandle"></param>
        /// <param name="millisecondsTimeout"></param>
        /// <returns></returns>
        public bool Send(AsyncSocket so, Func<Protocol, Task<long>> responseHandle, int millisecondsTimeout = 5000)
        {
            if (so?.Service == null)
                return false;

            IsRequest = true;
            ResponseHandle = responseHandle;
            Timeout = millisecondsTimeout;

            // try remove . 只维护一个上下文。
            //so.Service.TryRemoveRpcContext(SessionId, this);
            SessionId = so.Service.AddRpcContext(this);

            var timeoutTask = Schedule(so.Service, SessionId, millisecondsTimeout);

            if (base.Send(so))
                return true;

            // 发送失败，一般是连接失效，此时删除上下文。
            // 其中rpc-trigger-result的原子性由RemoveRpcContext保证。
            // Cancel不是必要的。
            timeoutTask?.Cancel();
            var ctx = so.Service.RemoveRpcContext<Rpc<TArgument, TResult>>(SessionId);
            // 恢复最初的语义吧：如果ctx已经被并发的Remove，也就是被处理了，这里返回true。
            return ctx == null;
        }

        /// <summary>
        /// 不管发送是否成功，总是建立RpcContext。
        /// 连接(so)可以为null，此时Rpc请求将在Timeout后回调。
        /// </summary>
        /// <param name="service"></param>
        /// <param name="so"></param>
        /// <param name="responseHandle"></param>
        /// <param name="millisecondsTimeout"></param>
        public void SendReturnVoid(Service service, AsyncSocket so, Func<Protocol, Task<long>> responseHandle,
            int millisecondsTimeout = 5000)
        {
            if (so != null && so.Service != service)
                throw new Exception("so.Service != service");

            IsRequest = true;
            ResponseHandle = responseHandle;
            Timeout = millisecondsTimeout;
            Service = service;

            // try remove . 只维护一个上下文。
            service.TryRemoveRpcContext(SessionId, this);
            SessionId = service.AddRpcContext(this);
            Schedule(service, SessionId, millisecondsTimeout);
            base.Send(so);
        }

        public async Task SendAsync(AsyncSocket so, int millisecondsTimeout = 5000)
        {
            Future = new TaskCompletionSource<TResult>(TaskCreationOptions.RunContinuationsAsynchronously);
            if (!Send(so, null, millisecondsTimeout))
                Future.SetException(new Exception("Send Failed."));
            await Future.Task;
        }

        public async Task SendAndCheckResultCodeAsync(AsyncSocket so, int millisecondsTimeout = 5000)
        {
            await SendAsync(so, millisecondsTimeout);
            if (ResultCode != 0)
                throw new Exception($"Rpc Invalid ResultCode={ResultCode} {this}");
        }

        private bool SendResultDone; // XXX ugly

        public override void SendResult(Binary result = null)
        {
            if (SendResultDone)
            {
                logger.Error($"Rpc.SendResult Already Done {Sender.LocalAddress}-{Sender.RemoteAddress} {this}");
                return;
            }
            SendResultDone = true;

            ResultEncoded = result;
            IsRequest = false;
            if (!base.Send(Sender))
            {
                logger.Log(Service.SocketOptions.SocketLogLevel, $"Rpc.SendResult Failed {Sender.LocalAddress}-{Sender.RemoteAddress} {this}");
            }
        }

        public override bool TrySendResultCode(long code)
        {
            if (SendResultDone)
                return false;
            ResultCode = code;
            SendResult();
            return true;
        }

        internal override void Dispatch(Service service, Service.ProtocolFactoryHandle factoryHandle)
        {
            if (IsRequest)
            {
                service.DispatchProtocol(this, factoryHandle);
                return;
            }

            // response, 从上下文中查找原来发送的rpc对象，并派发该对象。
            var context = service.RemoveRpcContext<Rpc<TArgument, TResult>>(SessionId);
            if (context == null)
            {
                logger.Log(Service.SocketOptions.SocketLogLevel, "rpc response: lost context, maybe timeout. {0}", this);
                return;
            }

            context.IsRequest = false;
            context.Result = Result;
            context.Sender = Sender;
            context.ResultCode = ResultCode;

            if (context.Future != null)
            {
                context.Future.SetResult(context.Result);
                return; // SendForWait，设置结果唤醒等待者。
            }
            context.IsTimeout = false; // not need
            if (context.ResponseHandle != null)
                service.DispatchRpcResponse(context, context.ResponseHandle, factoryHandle);
        }

        public override void Decode(ByteBuffer bb)
        {
            var compress = bb.ReadUInt();
            var familyClass = compress & Zeze.Net.FamilyClass.FamilyClassMask;
            IsRequest = familyClass == Zeze.Net.FamilyClass.Request;
            ResultCode = (compress & Zeze.Net.FamilyClass.BitResultCode) != 0 ? bb.ReadLong() : 0;
            SessionId = bb.ReadLong();
            if (IsRequest)
                Argument.Decode(bb);
            else
                Result.Decode(bb);
        }

        public override void Encode(ByteBuffer bb)
        {
            var compress = FamilyClass;
            if (ResultCode != 0)
                compress |= Zeze.Net.FamilyClass.BitResultCode;
            bb.WriteUInt(compress);
            if (ResultCode != 0)
                bb.WriteLong(ResultCode);
            bb.WriteLong(SessionId);

            if (IsRequest)
                Argument.Encode(bb);
            else if (ResultEncoded != null)
                bb.Append(ResultEncoded.Bytes, ResultEncoded.Offset, ResultEncoded.Count);
            else
                Result.Encode(bb);
        }

        public override string ToString()
        {
            return $"{GetType().FullName} SessionId={SessionId} ResultCode={ResultCode}{Environment.NewLine}" +
                   $"  Argument={Argument}{Environment.NewLine}" +
                   $"  Result={Result}";
        }

        public override void ClearParameters(ProtocolPool.ReuseLevel level)
        {
            switch (level)
            {
                case ProtocolPool.ReuseLevel.Protocol:
                    Argument = new TArgument();
                    Result = new TResult();
                    break;

                case ProtocolPool.ReuseLevel.Bean:
                    ArgumentBean?.ClearParameters();
                    ResultBean?.ClearParameters();
                    break;
            }
        }
    }
}
