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

        public override void Send(AsyncSocket so)
        {
            Send(so, null);
        }

        public void Send(AsyncSocket so, Func<Protocol, int> responseHandle, int millisecondsTimeout = 5000)
        {
            this.IsRequest = true;
            this.ResponseHandle = responseHandle;
            this.SessionId = so.Service.AddRpcContext(this);

            global::Zeze.Util.Scheduler.Instance.Schedule((ThisTask)=>
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

            base.Send(so);
        }

        public TaskCompletionSource<TResult> SendForWait(AsyncSocket so, int millisecondsTimeout = 5000)
        {
            Future = new TaskCompletionSource<TResult>();
            Send(so, null, millisecondsTimeout);
            return Future;
        }

        public void SendAndWaitCheckResultCode(AsyncSocket so, int millisecondsTimeout = 5000)
        {
            // 使用异步方式实现的同步等待版本
            var tmpFuture = new TaskCompletionSource<int>();
            Send(so, (_) =>
            {
                if (IsTimeout)
                {
                    tmpFuture.TrySetException(new RpcTimeoutException($"RpcTimeout {this}"));
                }
                else if (ResultCode != 0)
                {
                    tmpFuture.TrySetException(new Exception($"Rpc Invalid ResultCode={ResultCode} {this}"));
                }
                tmpFuture.SetResult(0);
                return Zeze.Transaction.Procedure.Success;
            }, millisecondsTimeout);
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
