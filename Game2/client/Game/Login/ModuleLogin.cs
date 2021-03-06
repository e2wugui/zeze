﻿
using System.Collections.Generic;

namespace Game.Login
{
    public interface IReliableNotify
    {
        public void OnReliableNotify(Zeze.Net.Protocol p);
    }

    public sealed partial class ModuleLogin : AbstractModule
    {
        public void Start(Game.App app)
        {
        }

        public void Stop(Game.App app)
        {
        }

        // 没有考虑线程问题
        private long ReliableNotifyTotalCount;
        private Dictionary<int, IReliableNotify> ReliableNotifyMap { get; } = new Dictionary<int, IReliableNotify>();

        public void RegisterReliableNotify(int protocolTypeId, IReliableNotify handle)
        {
            ReliableNotifyMap.Add(protocolTypeId, handle);
        }

        public override int ProcessSReliableNotify(SReliableNotify protocol)
        {
            // TODO
            ReliableNotifyTotalCount += protocol.Argument.Notifies.Count;

            foreach (var notify in protocol.Argument.Notifies)
            {
                try
                {
                    var bb = Zeze.Serialize.ByteBuffer.Wrap(notify);
                    int typeId = bb.ReadInt4();
                    if (ReliableNotifyMap.TryGetValue(typeId, out var handle))
                    {
                        int size = bb.ReadInt4();
                        // 所有的notify必须定义了客户端处理。
                        var factoryHandle = Game.App.Instance.Client.FindProtocolFactoryHandle(typeId);
                        var pNotify = factoryHandle.Factory();
                        pNotify.Decode(bb);
                        handle.OnReliableNotify(pNotify);
                    }
                }
                catch (System.Exception ex)
                {
                    // TODO handle error here.
                }
            }

            // 马上确认。可以考虑达到一定数量或者定时。
            var confirm = new ReliableNotifyConfirm();
            confirm.Argument.ReliableNotifyConfirmCount = ReliableNotifyTotalCount;
            protocol.Sender.Send(confirm);
            // process rpc result
            // TODO 同步队列确认失败，应该重新开始一次完成的登录流程。

            return Zeze.Transaction.Procedure.Success;
        }
    }
}
