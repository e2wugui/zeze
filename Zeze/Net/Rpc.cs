using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Net
{
    public abstract class Rpc<TArgument, TResult> : Protocol<TArgument>
        where TArgument: global::Zeze.Transaction.Bean, new()
        where TResult: global::Zeze.Transaction.Bean, new()
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public TResult Result { get; set; } = new TResult();

        public bool IsRequest { get; private set; }
        public Func<Protocol, int> ResponseHandle;
        public bool IsTimeout { get; private set; }
        public long SessionId { get; private set; }

        public TaskCompletionSource<TResult> Future { get; private set; }

        public Rpc()
        {
            this.IsTimeout = false;
        }

        public override bool Send(AsyncSocket so)
        {
            return Send(so, null);
        }

        public bool Send(AsyncSocket so, Func<Protocol, int> responseHandle, int millisecondsTimeout = 5000)
        {
            this.IsRequest = true;
            this.ResponseHandle = responseHandle;
            this.SessionId = so.Service.AddRpcContext(this);

            var timeoutTask = global::Zeze.Util.Scheduler.Instance.Schedule((ThisTask)=>
            {
                if (null == so.Service)
                    return; // Socket closed.

                Rpc<TArgument, TResult> context = so.Service.RemoveRpcContext<Rpc<TArgument, TResult>>(SessionId);
                if (null == context) // 一般来说，此时结果已经返回。
                    return;

                if (null != context.Future)
                {
                    context.Future.TrySetException(new RpcTimeoutException());
                    return;
                }
                context.IsTimeout = true;
                this.ResponseHandle?.Invoke(context);
            }, millisecondsTimeout, -1);

            if (base.Send(so))
                return true;

            // 发送失败，一般是连接失效，此时删除上下文。
            // 其中rpc-trigger-result的原子性由RemoveRpcContext保证。
            // Cancel不是必要的。
            timeoutTask.Cancel(); 
            // 【注意】当上下文已经其他并发过程删除（得到了处理），那么这里就返回成功。
            // see OnSocketDisposed
            // 这里返回 false 表示真的没有发送成功，外面根据自己需要决定是否重连并再次发送。
            Rpc<TArgument, TResult> context = so.Service.RemoveRpcContext<Rpc<TArgument, TResult>>(this.SessionId);
            return context == null;
        }

        public TaskCompletionSource<TResult> SendForWait(AsyncSocket so, int millisecondsTimeout = 5000)
        {
            Future = new TaskCompletionSource<TResult>();
            if (false == Send(so, null, millisecondsTimeout))
            {
                Future.SetException(new Exception("Send Failed."));
            }
            return Future;
        }

        // 使用异步方式实现的同步等待版本
        public void SendAndWaitCheckResultCode(AsyncSocket so, int millisecondsTimeout = 5000)
        {
            var tmpFuture = new TaskCompletionSource<int>();
            if (false == Send(so, 
                (_) =>
                {
                    if (IsTimeout)
                    {
                        tmpFuture.TrySetException(new RpcTimeoutException($"RpcTimeout {this}"));
                    }
                    else if (ResultCode != 0)
                    {
                        tmpFuture.TrySetException(new Exception($"Rpc Invalid ResultCode={ResultCode} {this}"));
                    }
                    else
                    {
                        tmpFuture.SetResult(0);
                    }
                    return Zeze.Transaction.Procedure.Success;
                }, millisecondsTimeout))
            {
                throw new Exception("Send Failed.");
            }
            tmpFuture.Task.Wait();
        }

        public void SendResult()
        {
            IsRequest = false;
            base.Send(Sender);
        }

        public void SendResultCode(int code)
        {
            ResultCode = code;
            IsRequest = false;
            base.Send(Sender);
        }

        internal override void Dispatch(Service service, Service.ProtocolFactoryHandle factoryHandle)
        {
            if (IsRequest)
            {
                service.DispatchProtocol(this, factoryHandle);
                return;
            }

            // response, 从上下文中查找原来发送的rpc对象，并派发该对象。
            Rpc<TArgument, TResult> context = service.RemoveRpcContext<Rpc<TArgument, TResult>>(SessionId);
            if (null == context)
            {
                logger.Info("rpc response: lost context, maybe timeout. {0}", this);
                return;
            }

            context.IsRequest = false;
            context.Result = Result;
            context.Sender = Sender;
            context.ResultCode = ResultCode;
            context.UserState = UserState;

            if (context.Future != null)
            {
                context.Future.SetResult(context.Result);
                return; // SendForWait，设置结果唤醒等待者。
            }
            context.IsTimeout = false; // not need
            if (null != context.ResponseHandle)
            {
                service.DispatchRpcResponse(this, context.ResponseHandle, factoryHandle);
            }
        }

        public override void Decode(ByteBuffer bb)
        {
            IsRequest = bb.ReadBool();
            SessionId = bb.ReadLong();
            ResultCode = bb.ReadInt();

            if (IsRequest)
            {
                Argument.Decode(bb);
            }
            else
            {
                Result.Decode(bb);
            }
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteBool(IsRequest);
            bb.WriteLong(SessionId);
            bb.WriteInt(ResultCode);

            if (IsRequest)
            {
                Argument.Encode(bb);
            }
            else
            {
                Result.Encode(bb);
            }
        }

        public override string ToString()
        {
            return $"{this.GetType().FullName} Argument={Argument} Result={Result}";
        }
    }
}
