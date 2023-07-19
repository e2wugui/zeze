// auto-generated

// ReSharper disable RedundantCast RedundantNameQualifier RedundantOverflowCheckingContext
// ReSharper disable once CheckNamespace
namespace Zezex.Linkd
{
    public sealed class Auth : Zeze.Net.Rpc<Zezex.Linkd.BAuth, Zeze.Util.ConfEmptyBean>
    {
        public const int ModuleId_ = 10000;
        public const int ProtocolId_ = -997899722; // 3297067574
        public const long TypeId_ = (long)ModuleId_ << 32 | unchecked((uint)ProtocolId_); // 42952970027574

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
        public const int Success = 0;
        public const int Error = 1;

    }
}
