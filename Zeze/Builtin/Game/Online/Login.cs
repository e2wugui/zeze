// auto-generated

// 登录角色
// ReSharper disable RedundantCast RedundantNameQualifier RedundantOverflowCheckingContext
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Game.Online
{
    public sealed class Login : Zeze.Net.Rpc<Zeze.Builtin.Game.Online.BLogin, Zeze.Transaction.EmptyBean>
    {
        public const int ModuleId_ = 11013;
        public const int ProtocolId_ = -789575265; // 3505392031
        public const long TypeId_ = (long)ModuleId_ << 32 | unchecked((uint)ProtocolId_); // 47303980222879

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
