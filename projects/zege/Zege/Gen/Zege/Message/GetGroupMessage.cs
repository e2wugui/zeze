// auto-generated

// ReSharper disable RedundantCast RedundantNameQualifier RedundantOverflowCheckingContext
// ReSharper disable once CheckNamespace
namespace Zege.Message
{
    public sealed class GetGroupMessage : Zeze.Net.Rpc<Zege.Message.BGetGroupMessage, Zege.Message.BGetMessageResult>
    {
        public const int ModuleId_ = 3;
        public const int ProtocolId_ = 1796076737;
        public const long TypeId_ = (long)ModuleId_ << 32 | unchecked((uint)ProtocolId_); // 14680978625

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
