// auto-generated

// ReSharper disable RedundantCast RedundantNameQualifier RedundantOverflowCheckingContext
// ReSharper disable once CheckNamespace
namespace Zege.Message
{
    public sealed class GetFriendMessage : Zeze.Net.Rpc<Zege.Message.BGetFriendMessage, Zege.Message.BGetMessageResult>
    {
        public const int ModuleId_ = 3;
        public const int ProtocolId_ = 400871803;
        public const long TypeId_ = (long)ModuleId_ << 32 | unchecked((uint)ProtocolId_); // 13285773691

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
