using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Text;
using Zeze.Serialize;

namespace Zeze.Net
{
    public abstract class Rpc<TArgument, TResult> : Protocol<TArgument>
        where TArgument: Zeze.Transaction.Bean, new()
        where TResult: Zeze.Transaction.Bean, new()
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public TResult Result { get; set; } = new TResult();

        public bool IsRequest { get; private set; }

        private long sid;

        public override void Send(AsyncSocket so)
        {
            IsRequest = true;
            sid = so.Manager.AddRpcContext(this);
            base.Send(so);
            Zeze.Util.Scheduler.Instance.Schedule(_OnTimeout, 2000);
        }

        public virtual int ProcessServer()
        {
            throw new NotImplementedException();
        }

        public virtual int ProcessClient()
        {
            throw new NotImplementedException();
        }

        public virtual int ProcessTimeout()
        {
            logger.Info("Rpc.OnTimeout not implement. {0}", this.ToString());
            return 0;
        }

        private void _OnTimeout()
        {
            Rpc<TArgument, TResult> context = Sender.Manager.RemoveRpcContext<Rpc<TArgument, TResult>>(sid);
            if (null != context)
                context.ProcessTimeout();
        }

        public override int Process()
        {
            try
            {
                if (IsRequest)
                {
                    int r = ProcessServer();
                    if (0 == r)
                    {
                        IsRequest = false;
                        base.Send(Sender); // TODO 如果客户端不是直接连接，而是通过代理包装转发，不能这样直接发送结果。
                    }
                    return r;
                }
                else
                {
                    return ProcessClient();
                }
            }
            catch (Exception e)
            {
                logger.Error(e, "Rpc.Run {0}", this.ToString());
                return Transaction.Procedure.Excption;
            }
        }

        internal override void Dispatch(Manager manager)
        {
            if (IsRequest)
            {
                manager.DispatchProtocol(this);
                return;
            }

            // response, 从上下文中查找原来发送的rpc对象，并派发该对象。
            Rpc<TArgument, TResult> context = manager.RemoveRpcContext<Rpc<TArgument, TResult>>(sid);
            if (null == context)
            {
                logger.Info("rpc response: lost context, maybe timeout. {0}", this.ToString());
            }
            else
            {
                context.IsRequest = false;
                manager.DispatchProtocol(context);
            }
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
