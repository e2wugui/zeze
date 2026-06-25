// auto-generated

// ReSharper disable RedundantCast RedundantNameQualifier RedundantOverflowCheckingContext
// ReSharper disable once CheckNamespace
namespace Zege.Linkd
{
    public sealed class Challenge : Zeze.Net.Rpc<Zege.Linkd.BChallengeArgument, Zege.Linkd.BChallengeResult>
    {
        public const int ModuleId_ = 10000;
        public const int ProtocolId_ = 113415344;
        public const long TypeId_ = (long)ModuleId_ << 32 | unchecked((uint)ProtocolId_); // 42949786375344

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
