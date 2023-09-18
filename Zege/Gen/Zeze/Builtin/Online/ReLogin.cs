// auto-generated

// 网络断开重新登录（数据无法同步时会失败，此时客户端应该重新走完整的登录流程-装载数据。）
// ReSharper disable RedundantCast RedundantNameQualifier RedundantOverflowCheckingContext
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Online
{
    public sealed class ReLogin : Zeze.Net.Rpc<Zeze.Builtin.Online.BReLogin, Zeze.Util.ConfEmptyBean>
    {
        public const int ModuleId_ = 11100;
        public const int ProtocolId_ = 927898915;
        public const long TypeId_ = (long)ModuleId_ << 32 | unchecked((uint)ProtocolId_); // 47675064884515

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
