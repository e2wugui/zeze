// auto-generated

// 			客户端收到这条协议表示排队成功。此后可以连接link继续登录。
// ReSharper disable RedundantCast RedundantNameQualifier RedundantOverflowCheckingContext
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.LoginQueue
{
    public sealed class PutLoginToken : Zeze.Net.Protocol<Zeze.Builtin.LoginQueue.BLoginToken>
    {
        public const int ModuleId_ = 11043;
        public const int ProtocolId_ = 1893050735;
        public const long TypeId_ = (long)ModuleId_ << 32 | unchecked((uint)ProtocolId_); // 47431216900463

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
