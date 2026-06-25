// auto-generated

/*
			客户端收到这条协议之前，显示排队中，数量显示大于10000
			LoginQueue服务器最多只会广播通知前10000个客户端。
			客户端收到这条协议，就更新显示的排队数量。排队时，每N秒更新一次。
*/
// ReSharper disable RedundantCast RedundantNameQualifier RedundantOverflowCheckingContext
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.LoginQueue
{
    public sealed class PutQueuePosition : Zeze.Net.Protocol<Zeze.Builtin.LoginQueue.BQueuePosition>
    {
        public const int ModuleId_ = 11043;
        public const int ProtocolId_ = -1003438419; // 3291528877
        public const long TypeId_ = (long)ModuleId_ << 32 | unchecked((uint)ProtocolId_); // 47432615378605

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
