using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
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
        private long sid;

        public TaskCompletionSource<TResult> Future { get; private set; }

        public Rpc()
        {
        }

        public override void Send(AsyncSocket so)
        {
            Send(so, 5000);
        }

        public void Send(AsyncSocket so, int millisecondsTimeout)
        {
            IsRequest = true;
            sid = so.Service.AddRpcContext(this);
            base.Send(so);

            global::Zeze.Util.Scheduler.Instance.Schedule(()=>
            {
                if (null == so.Service)
                    return; // Socket closed.

                Rpc<TArgument, TResult> context = so.Service.RemoveRpcContext<Rpc<TArgument, TResult>>(sid);
                if (null == context) // 一般来说，此时结果已经返回。
                    return;

                if (null != context.Future)
                {
                    context.Future.SetException(new RpcTimeoutException());
                    return;
                }
                so.Service.DispatchProtocol(context, so.Service.FindProtocolFactoryHandle(context.TypeId), Service.DispatchType.Timeout);
            }, millisecondsTimeout, -1);
        }

        public TaskCompletionSource<TResult> SendForWait(AsyncSocket so, int millisecondsTimeout = 5000)
        {
            Future = new TaskCompletionSource<TResult>();
            Send(so, millisecondsTimeout);
            return Future;
        }

        public void SendResult()
        {
            IsRequest = false;
            base.Send(Sender);
        }

        public void SendResultCode(int code)
        {
            ResultCode = code;
            SendResult();
        }

        internal override void Dispatch(Service service, Service.ProtocolFactoryHandle factoryHandle)
        {
            if (IsRequest)
            {
                service.DispatchProtocol(this, factoryHandle, Service.DispatchType.Request);
                return;
            }

            // response, 从上下文中查找原来发送的rpc对象，并派发该对象。
            Rpc<TArgument, TResult> context = service.RemoveRpcContext<Rpc<TArgument, TResult>>(sid);
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

            service.DispatchProtocol(context, factoryHandle, Service.DispatchType.Response);
        }

        public override void Decode(ByteBuffer bb)
        {
            sid = bb.ReadLong();
            IsRequest = ((ulong)sid & 0x8000000000000000) != 0;
            if (IsRequest)
            {
                sid &= 0x7fffffffffffffff;
                ResultCode = bb.ReadInt();
                Argument.Decode(bb);
            }
            else
            {
                ResultCode = bb.ReadInt();
                Result.Decode(bb);
            }
        }

        public override void Encode(ByteBuffer bb)
        {
            if (IsRequest)
            {
                bb.WriteLong((long)((ulong)sid | 0x8000000000000000));
                bb.WriteInt(ResultCode);
                Argument.Encode(bb);
            }
            else
            {
                bb.WriteLong(sid);
                bb.WriteInt(ResultCode);
                Result.Encode(bb);
            }
        }
    }
}
