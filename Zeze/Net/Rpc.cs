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
        public override int TypeId => typeId;

        private long sid;
        private int typeId;
        private TaskCompletionSource<TResult> future;

        public Rpc()
        {
            typeId = base.TypeId;
        }

        public override void Send(AsyncSocket so)
        {
            IsRequest = true;
            sid = so.Service.AddRpcContext(this);
            base.Send(so);

            global::Zeze.Util.Scheduler.Instance.Schedule(()=>
            {
                Rpc<TArgument, TResult> context = so.Service.RemoveRpcContext<Rpc<TArgument, TResult>>(sid);
                if (null == context)
                {
                    // 一般来说，此时结果已经返回。
                    return;
                }

                if (null != context.future)
                {
                    context.future.SetException(new Exception("Rpc.Timeout " + context.ToString()));
                    return;
                }

                context.Sender = null; // timeout 没有网络。
                context.typeId = TypeRpcTimeoutId;
                so.Service.DispatchProtocol(context);
            }, 5000);
        }

        public TaskCompletionSource<TResult> SendForWait(AsyncSocket so)
        {
            future = new TaskCompletionSource<TResult>();
            Send(so);
            return future;
        }

        public void SendResult()
        {
            IsRequest = false;
            base.Send(Sender);
        }

        internal override void Dispatch(Service service)
        {
            if (IsRequest)
            {
                service.DispatchProtocol(this);
                return;
            }

            // response, 从上下文中查找原来发送的rpc对象，并派发该对象。
            Rpc<TArgument, TResult> context = service.RemoveRpcContext<Rpc<TArgument, TResult>>(sid);
            if (null == context)
            {
                logger.Info("rpc response: lost context, maybe timeout. {0}", this);
                return;
            }

            if (context.future != null)
            {
                future.SetResult(Result);
                return; // SendForWait，设置结果唤醒等待者。
            }

            context.typeId = TypeRpcResponseId;
            context.IsRequest = false;
            context.Sender = Sender;
            service.DispatchProtocol(context);
        }

        public override void Decode(ByteBuffer bb)
        {
            sid = bb.ReadLong();
            IsRequest = ((ulong)sid & 0x8000000000000000) != 0;
            if (IsRequest)
            {
                sid &= 0x7fffffffffffffff;
                Argument.Decode(bb);
            }
            else
            {
                Result.Decode(bb);
            }
        }

        public override void Encode(ByteBuffer bb)
        {
            if (IsRequest)
            {
                bb.WriteLong((long)((ulong)sid | 0x8000000000000000));
                Argument.Encode(bb);
            }
            else
            {
                bb.WriteLong(sid);
                Result.Encode(bb);
            }
        }
    }
}
