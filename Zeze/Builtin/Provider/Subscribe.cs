// auto-generated

// 通知linkd订阅模块的服务列表。
// ReSharper disable RedundantCast RedundantNameQualifier RedundantOverflowCheckingContext
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Provider
{
    public sealed class Subscribe : Zeze.Net.Rpc<Zeze.Builtin.Provider.BSubscribe, Zeze.Transaction.EmptyBean>
    {
        public const int ModuleId_ = 11008;
        public const int ProtocolId_ = 1110460218;
        public const long TypeId_ = (long)ModuleId_ << 32 | unchecked((uint)ProtocolId_); // 47280110454586

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
