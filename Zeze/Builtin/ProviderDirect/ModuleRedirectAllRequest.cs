// auto-generated

// 使用protocol而不是rpc，是为了可以按分组返回结果，当然现在定义支持一个结果里面包含多个分组结果
// ReSharper disable RedundantCast RedundantNameQualifier RedundantOverflowCheckingContext
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.ProviderDirect
{
    public sealed class ModuleRedirectAllRequest : Zeze.Net.Protocol<Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest>
    {
        public const int ModuleId_ = 11009;
        public const int ProtocolId_ = -773666772; // 3521300524
        public const long TypeId_ = (long)ModuleId_ << 32 | unchecked((uint)ProtocolId_); // 47286816262188

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
